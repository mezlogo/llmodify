package mezlogo.llmodify.core.diffcalculator.impl

import mezlogo.llmodify.core.diffcalculator.ConfigurationDiffResult
import mezlogo.llmodify.core.diffcalculator.ConfigurationFileTreeDiff
import mezlogo.llmodify.core.diffcalculator.ConfigurationPacmanDiff
import mezlogo.llmodify.core.diffcalculator.DiffCalculatorUseCase
import mezlogo.llmodify.core.diffcalculator.SystemdServiceDiff
import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.models.Link
import mezlogo.llmodify.core.models.Path
import mezlogo.llmodify.core.models.RelativeConfigDirectory
import mezlogo.llmodify.core.models.RelativeConfigFile
import mezlogo.llmodify.core.models.RelativeConfigLink
import mezlogo.llmodify.core.pacman.PacmanUseCase
import mezlogo.llmodify.core.repo.MergedByHostRepresentation
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.systemd.SystemdUseCase
import mezlogo.llmodify.port.platform.PlatformPort

/**
 * Computes the diff between the declared (merged) state and the real state of a machine.
 *
 * The diff is computed by walking the *declared* tree and probing only the real paths that the
 * declared tree actually mentions. We never enumerate the real tree recursively: this avoids
 * reading directories we have no business touching (for example
 * `/etc/NetworkManager/system-connections`, which is often mode 0700 and owned by root), and it
 * keeps the diff cheap even when the real root is huge.
 */
class DiffCalculatorService(
    private val platformPort: PlatformPort,
    private val pacmanUseCase: PacmanUseCase,
    private val systemdUseCase: SystemdUseCase,
) : DiffCalculatorUseCase {

  override fun calculateDiff(
      config: RepoConfig,
      mergedByHostRepresentation: MergedByHostRepresentation,
      featureFlags: FeatureFlags,
  ): ConfigurationDiffResult {
    return ConfigurationDiffResult(
        pacmanDiff =
            if (featureFlags.pacmanEnabled)
                calculatePacmanDiff(
                    mergedByHostRepresentation.pacman,
                    pacmanUseCase.listExplicitlyInstalledPackages(),
                )
            else emptyPacmanDiff(),
        aurDiff =
            if (featureFlags.aurEnabled)
                calculatePacmanDiff(
                    mergedByHostRepresentation.aur,
                    pacmanUseCase.listExplicitlyForeignInstalledPackages(),
                )
            else emptyPacmanDiff(),
        configRelatedFileTreeDiff =
            if (featureFlags.configEnabled)
                calculateFileTreeDiff(mergedByHostRepresentation.config, config.config)
            else emptyFileTreeDiff(),
        homeRelatedFileTreeDiff =
            if (featureFlags.homeEnabled)
                calculateFileTreeDiff(mergedByHostRepresentation.home, config.home)
            else emptyFileTreeDiff(),
        // /etc is populated with `sudo cp`, not with symlinks.
        etcRelatedFileTreeDiff =
            if (featureFlags.etcEnabled)
                calculateFileTreeDiff(
                    mergedByHostRepresentation.etc,
                    config.etc,
                    symlinkMode = false,
                )
            else emptyFileTreeDiff(),
        systemdServiceDiff =
            if (featureFlags.systemdEnabled)
                calculateSystemdDiff(
                    declared = mergedByHostRepresentation.systemdServices,
                    enabled = systemdUseCase.getEnabledSystemServices(),
                )
            else emptySystemdDiff(),
        userSystemdServiceDiff =
            if (featureFlags.userSystemdEnabled)
                calculateSystemdDiff(
                    declared = mergedByHostRepresentation.userSystemdServices,
                    enabled = systemdUseCase.getEnabledUserServices(),
                )
            else emptySystemdDiff(),
    )
  }

  private fun emptyFileTreeDiff() = ConfigurationFileTreeDiff(emptyList(), emptyList(), emptyList())

  private fun emptyPacmanDiff() =
      ConfigurationPacmanDiff(emptyList(), emptyList(), emptyList(), emptyList())

  private fun emptySystemdDiff() = SystemdServiceDiff(emptyList(), emptyList(), emptyList())

  // ----- pacman -------------------------------------------------------------

  private fun calculatePacmanDiff(
      declared: List<String>,
      installed: Set<String>,
  ): ConfigurationPacmanDiff {
    val declaredSet = declared.toSet()

    return ConfigurationPacmanDiff(
        installedExplicitPackages = installed.sorted(),
        declaredPackages = declaredSet.sorted(),
        notDeclaredInstalledPackages = installed.subtract(declaredSet).sorted(),
        declaredNotInstalledPackages = declaredSet.subtract(installed).sorted(),
    )
  }

  // ----- systemd ------------------------------------------------------------

  private fun calculateSystemdDiff(
      declared: List<String>,
      enabled: Set<String>,
  ): SystemdServiceDiff {
    val declaredSet = declared.toSet()

    return SystemdServiceDiff(
        declaredServices = declaredSet.sorted(),
        declaredNotEnabledServices = declaredSet.subtract(enabled).sorted(),
        notDeclaredEnabledServices = enabled.subtract(declaredSet).sorted(),
    )
  }

  // ----- file trees ---------------------------------------------------------

  /**
   * Compare the declared tree (relative) with the real tree on disk.
   *
   * Only paths that appear in [declared] are probed on the real filesystem. For every declared file
   * we `lstat` the corresponding real path and produce the appropriate action:
   *
   * - missing / directory in the way / regular file (in symlink mode) / wrong link → remove if
   *   needed, then link;
   * - correct symlink (in symlink mode) → already synced, nothing to do.
   *
   * @param symlinkMode when `true`, the desired end-state for a declared file is a symlink to the
   *   declared file. When `false` (used for `/etc`), the desired end-state is a regular copy of the
   *   declared file: an existing regular file is simply overwritten, while any other kind of node
   *   (dir, symlink, link-to-elsewhere) has to be removed first.
   */
  private fun calculateFileTreeDiff(
      declared: RelativeConfigDirectory?,
      realRoot: Directory,
      symlinkMode: Boolean = true,
  ): ConfigurationFileTreeDiff {
    if (declared == null || declared.children.isEmpty()) {
      return ConfigurationFileTreeDiff(
          filesToRemoveBefore = emptyList(),
          directoriesToCreate = emptyList(),
          linksToCreate = emptyList(),
      )
    }

    val filesToRemoveBefore = mutableListOf<Path>()
    val linksToCreate = mutableListOf<Link>()
    val directoriesToCreate = linkedSetOf<Directory>()

    // The root itself is usually assumed to exist. If it does not, create it; if it is occupied
    // by a file or a link, that occupant must go before we can create the directory.
    when (val rootNode = platformPort.getFile(realRoot.path)) {
      is Directory -> Unit
      null -> directoriesToCreate.add(realRoot)
      else -> {
        filesToRemoveBefore.add(rootNode)
        directoriesToCreate.add(realRoot)
      }
    }

    fun enqueue(realFile: Path, declaredFile: Path) {
      linksToCreate.add(Link(realFile.path, target = declaredFile))
      directoriesToCreate.addAll(missingParentsOf(realFile, realRoot))
    }

    fun walk(declaredDir: RelativeConfigDirectory, realDir: Directory) {
      for (child in declaredDir.children) {
        when (child) {
          is RelativeConfigFile -> {
            val realPath = realDir.resolveChild(child.relativeFile.path)
            when (val real = platformPort.getFile(realPath)) {
              null -> enqueue(File(realPath), child.absoluteFile)

              is Directory -> {
                // A directory sits where a file should be: remove it, then link.
                filesToRemoveBefore.add(real)
                enqueue(real, child.absoluteFile)
              }

              is File -> {
                if (symlinkMode) {
                  // Symlink mode: a regular file is in the way, remove it then link.
                  filesToRemoveBefore.add(real)
                  enqueue(real, child.absoluteFile)
                } else {
                  // Copy mode (/etc): file exists with identical content → already synced.
                  // Otherwise schedule a copy (an existing regular file is overwritten in
                  // place by `sudo cp`, no removal needed).
                  if (!platformPort.filesAreIdentical(real, child.absoluteFile)) {
                    enqueue(real, child.absoluteFile)
                  }
                }
              }

              is Link -> {
                val alreadySynced = symlinkMode && real.target.path == child.absoluteFile.path
                if (!alreadySynced) {
                  filesToRemoveBefore.add(real)
                  enqueue(real, child.absoluteFile)
                }
              }
            }
          }

          is RelativeConfigDirectory -> {
            val realSub = Directory(realDir.resolveChild(child.relativeDirectory.path))
            when (val real = platformPort.getFile(realSub.path)) {
              is Directory -> Unit
              null -> Unit // will be created by missingParentsOf
              else -> {
                // A file or link occupies the slot where we need a directory.
                // Remove the occupant and recreate the slot as a directory.
                filesToRemoveBefore.add(real)
                directoriesToCreate.add(realSub)
              }
            }
            walk(child, realSub)
          }

          is RelativeConfigLink -> {
            // Declared module trees are expected to contain only regular files.
            // A link on the declared side is ignored, exactly like before.
          }
        }
      }
    }

    walk(declared, realRoot)

    return ConfigurationFileTreeDiff(
        filesToRemoveBefore = filesToRemoveBefore,
        directoriesToCreate = directoriesToCreate.toList(),
        linksToCreate = linksToCreate,
    )
  }

  /**
   * Walk up from [realFile]'s parent, collecting directories that do not exist yet, stopping at
   * [root] (exclusive). The returned list is ordered outermost → innermost.
   *
   * If an existing file or link sits where a parent directory is required, we stop: that node has
   * already been registered as a conflict (and as a directory to recreate) by
   * [calculateFileTreeDiff]'s directory walk, so we must not emit a bogus `Directory` for it.
   */
  private fun missingParentsOf(realFile: Path, root: Directory): List<Directory> {
    val rootPath = root.path.trimEnd('/')
    val missing = mutableListOf<Directory>()

    var current: Path = realFile.parent()
    while (current is Directory) {
      val path = current.path.trimEnd('/')
      if (path.isEmpty() || path == rootPath) break
      if (!path.startsWith("$rootPath/")) break

      when (platformPort.getFile(current.path)) {
        is Directory -> break
        null -> {
          missing.add(current)
          current = current.parent()
        }
        else -> break // occupied by a file/link; conflict already registered
      }
    }

    missing.reverse()
    return missing
  }
}
