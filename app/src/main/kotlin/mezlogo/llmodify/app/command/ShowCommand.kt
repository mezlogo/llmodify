package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import mezlogo.llmodify.core.diffcalculator.ConfigurationFileTreeDiff
import mezlogo.llmodify.core.diffcalculator.ConfigurationPacmanDiff
import mezlogo.llmodify.core.diffcalculator.DiffCalculatorUseCase
import mezlogo.llmodify.core.diffcalculator.SystemdServiceDiff
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.RelativeConfigDirectory
import mezlogo.llmodify.core.models.RelativeConfigFile
import mezlogo.llmodify.core.models.RelativeConfigLink
import mezlogo.llmodify.core.repo.MergedByHostRepresentation
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.repo.RepoUseCase
import mezlogo.llmodify.port.platform.PlatformPort

/**
 * command: show
 *
 * Show declared host with config:
 * - pacman: list declared packages set, when diff is enabled show installed declared, not installed
 *   declared and not declared installed
 * - systemd/user systemd: list declared services, when diff is enabled show enabled declared,
 *   declared not enabled and not declared enabled
 * - configs: merge all declared configs and print it, when merge option is true pass it to ignore
 *   raising an error.
 */
class ShowCommand(
    platformPort: PlatformPort,
    val repoUseCase: RepoUseCase,
    val diffCalculatorUseCase: DiffCalculatorUseCase,
) : AbstractArchSyncCommand("show", platformPort) {

  val merge: Boolean by
      option(help = "Merge conflicts in configs or report about conflict").flag(default = false)

  val diff: Boolean by option(help = "Compare with current state").flag(default = false)

  override fun runRepoCallback(config: RepoConfig, featureFlags: FeatureFlags) {
    // `--merge` means "do not fail on conflicts, just override earlier modules with later ones".
    val throwOnConflict = !merge
    val merged = repoUseCase.getMergedByHostRepresentation(config, throwOnConflict, featureFlags)

    echo("Host: ${config.host}")
    echo("Repo: ${config.repo.path}")
    echo("")

    if (diff) {
      printDiff(config, merged, featureFlags)
    } else {
      printDeclared(config, merged, featureFlags)
    }
  }

  // ----- declared (no --diff) ----------------------------------------------

  private fun printDeclared(
      config: RepoConfig,
      merged: MergedByHostRepresentation,
      featureFlags: FeatureFlags,
  ) {
    if (featureFlags.pacmanEnabled) {
      echo("Pacman (${merged.pacman.size}):")
      if (merged.pacman.isEmpty()) {
        echo("  (none)")
      } else {
        for (pkg in merged.pacman) echo("  $pkg")
      }
      echo("")
    }

    if (featureFlags.systemdEnabled) {
      echo("Systemd (${merged.systemdServices.size}):")
      if (merged.systemdServices.isEmpty()) {
        echo("  (none)")
      } else {
        for (service in merged.systemdServices) echo("  $service")
      }
      echo("")
    }

    if (featureFlags.userSystemdEnabled) {
      echo("User systemd (${merged.userSystemdServices.size}):")
      if (merged.userSystemdServices.isEmpty()) {
        echo("  (none)")
      } else {
        for (service in merged.userSystemdServices) echo("  $service")
      }
      echo("")
    }

    if (featureFlags.configEnabled) {
      echo("Config (${config.config.path}):")
      printDeclaredTree(merged.config)
      echo("")
    }

    if (featureFlags.homeEnabled) {
      echo("Home (${config.home.path}):")
      printDeclaredTree(merged.home)
    }

    if (featureFlags.etcEnabled) {
      echo("Etc (${config.etc.path}):")
      printDeclaredTree(merged.etc)
    }
  }

  private fun printDeclaredTree(tree: RelativeConfigDirectory?) {
    if (tree == null) {
      echo("  (none)")
      return
    }

    val files = collectRelativeFiles(tree)
    if (files.isEmpty()) {
      echo("  (empty)")
    } else {
      for (file in files) echo("  $file")
    }
  }

  // ----- diff (--diff) ------------------------------------------------------

  private fun printDiff(
      config: RepoConfig,
      merged: MergedByHostRepresentation,
      featureFlags: FeatureFlags,
  ) {
    val result = diffCalculatorUseCase.calculateDiff(config, merged, featureFlags)

    if (featureFlags.pacmanEnabled) {
      printPacmanDiff(result.pacmanDiff)
    }

    if (featureFlags.aurEnabled) {
      printAurDiff(result.aurDiff)
    }

    if (featureFlags.systemdEnabled) {
      printSystemdDiff("Systemd", result.systemdServiceDiff)
    }

    if (featureFlags.userSystemdEnabled) {
      printSystemdDiff("User systemd", result.userSystemdServiceDiff)
    }

    if (featureFlags.configEnabled) {
      echo("Config diff (${config.config.path}):")
      printFileTreeDiff(result.configRelatedFileTreeDiff)
      echo("")
    }

    if (featureFlags.homeEnabled) {
      echo("Home diff (${config.home.path}):")
      printFileTreeDiff(result.homeRelatedFileTreeDiff)
    }

    if (featureFlags.etcEnabled) {
      echo("Etc diff (${config.etc.path}):")
      printFileTreeDiff(result.etcRelatedFileTreeDiff)
    }
  }

  private fun printPacmanDiff(
      pacman: ConfigurationPacmanDiff,
  ) {
    val installedSet = pacman.installedExplicitPackages.toSet()
    val declaredSet = pacman.declaredPackages.toSet()
    val installedDeclared = declaredSet.filter { it in installedSet }

    echo("Pacman:")
    echo("  Installed & declared (${installedDeclared.size}):")
    if (installedDeclared.isEmpty()) echo("    (none)")
    else for (pkg in installedDeclared) echo("    $pkg")

    echo("  Declared but not installed (${pacman.declaredNotInstalledPackages.size}):")
    if (pacman.declaredNotInstalledPackages.isEmpty()) echo("    (none)")
    else for (pkg in pacman.declaredNotInstalledPackages) echo("    $pkg")

    echo("  Installed but not declared (${pacman.notDeclaredInstalledPackages.size}):")
    if (pacman.notDeclaredInstalledPackages.isEmpty()) echo("    (none)")
    else for (pkg in pacman.notDeclaredInstalledPackages) echo("    $pkg")
    echo("")
  }

  private fun printAurDiff(aur: ConfigurationPacmanDiff) {
    val installedSet = aur.installedExplicitPackages.toSet()
    val declaredSet = aur.declaredPackages.toSet()
    val installedDeclared = declaredSet.filter { it in installedSet }

    echo("AUR:")
    echo("  Installed & declared (${installedDeclared.size}):")
    if (installedDeclared.isEmpty()) echo("    (none)")
    else for (pkg in installedDeclared) echo("    $pkg")

    echo("  Declared but not installed (${aur.declaredNotInstalledPackages.size}):")
    if (aur.declaredNotInstalledPackages.isEmpty()) echo("    (none)")
    else for (pkg in aur.declaredNotInstalledPackages) echo("    $pkg")

    echo("  Installed but not declared (${aur.notDeclaredInstalledPackages.size}):")
    if (aur.notDeclaredInstalledPackages.isEmpty()) echo("    (none)")
    else for (pkg in aur.notDeclaredInstalledPackages) echo("    $pkg")
    echo("")
  }

  private fun printSystemdDiff(title: String, diff: SystemdServiceDiff) {
    val enabledSet = diff.declaredServices.toSet() - diff.declaredNotEnabledServices.toSet()
    val enabledDeclared = diff.declaredServices.filter { it in enabledSet }

    echo("$title:")
    echo("  Enabled & declared (${enabledDeclared.size}):")
    if (enabledDeclared.isEmpty()) echo("    (none)")
    else for (service in enabledDeclared) echo("    $service")

    echo("  Declared but not enabled (${diff.declaredNotEnabledServices.size}):")
    if (diff.declaredNotEnabledServices.isEmpty()) echo("    (none)")
    else for (service in diff.declaredNotEnabledServices) echo("    $service")

    echo("  Enabled but not declared (${diff.notDeclaredEnabledServices.size}):")
    if (diff.notDeclaredEnabledServices.isEmpty()) echo("    (none)")
    else for (service in diff.notDeclaredEnabledServices) echo("    $service")
    echo("")
  }

  private fun printFileTreeDiff(diff: ConfigurationFileTreeDiff) {
    echo("  Files to remove before (${diff.filesToRemoveBefore.size}):")
    if (diff.filesToRemoveBefore.isEmpty()) {
      echo("    (none)")
    } else {
      for (path in diff.filesToRemoveBefore) echo("    ${path.path}")
    }

    echo("  Directories to create (${diff.directoriesToCreate.size}):")
    if (diff.directoriesToCreate.isEmpty()) {
      echo("    (none)")
    } else {
      for (dir in diff.directoriesToCreate) echo("    ${dir.path}")
    }

    echo("  Links to create (${diff.linksToCreate.size}):")
    if (diff.linksToCreate.isEmpty()) {
      echo("    (none)")
    } else {
      for (link in diff.linksToCreate) {
        echo("    ${link.path} -> ${link.target.path}")
      }
    }
  }

  // ----- helpers ------------------------------------------------------------

  /**
   * Walk a [RelativeConfigDirectory] tree and return every child path relative to the tree root
   * (e.g. "myapp/myapp.cfg"). Directories themselves are not emitted, only files and links.
   */
  private fun collectRelativeFiles(
      dir: RelativeConfigDirectory,
      prefix: String = "",
  ): List<String> {
    val result = mutableListOf<String>()

    for (child in dir.children) {
      when (child) {
        is RelativeConfigFile -> {
          val name = child.relativeFile.path
          result.add(if (prefix.isEmpty()) name else "$prefix/$name")
        }

        is RelativeConfigDirectory -> {
          val name = child.relativeDirectory.path
          val newPrefix = if (prefix.isEmpty()) name else "$prefix/$name"
          result.addAll(collectRelativeFiles(child, newPrefix))
        }

        is RelativeConfigLink -> {
          val name = child.relativeLink.path
          result.add(if (prefix.isEmpty()) name else "$prefix/$name")
        }
      }
    }

    return result
  }
}
