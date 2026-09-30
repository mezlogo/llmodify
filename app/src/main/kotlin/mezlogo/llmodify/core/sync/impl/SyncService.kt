package mezlogo.llmodify.core.sync.impl

import mezlogo.llmodify.core.diffcalculator.ConfigurationDiffResult
import mezlogo.llmodify.core.diffcalculator.ConfigurationFileTreeDiff
import mezlogo.llmodify.core.diffcalculator.SystemdServiceDiff
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.pacman.PacmanUseCase
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.sync.SyncUseCase
import mezlogo.llmodify.core.systemd.SystemdUseCase
import mezlogo.llmodify.port.platform.PlatformPort

class SyncService(
    val platformPort: PlatformPort,
    val systemdUseCase: SystemdUseCase,
    val pacmanUseCase: PacmanUseCase,
) : SyncUseCase {

  override fun sync(
      config: RepoConfig,
      diffResult: ConfigurationDiffResult,
      featureFlags: FeatureFlags,
      cleanup: Boolean,
      overwrite: Boolean,
      interactive: Boolean,
  ) {
    println(
        "[sync] start host=${config.host} repo=${config.repo.path} home=${config.home.path} config=${config.config.path}"
    )
    println("[sync] options cleanup=$cleanup overwrite=$overwrite interactive=$interactive")

    // `cleanup` (removing packages that are installed but not declared) is not implemented yet.
    // Do the safety check first so we fail before touching the system.
    if (cleanup) {
      TODO("Cleanup (removing undeclared packages) is not yet implemented")
    }

    // Pre-flight: do not make a single change if any conflicting real path exists unless the
    // caller explicitly allowed overwriting. This makes `sync` atomic from the user's point of
    // view: either it does the safe work, or it fails before doing anything.
    val conflicts =
        diffResult.configRelatedFileTreeDiff.filesToRemoveBefore +
            diffResult.homeRelatedFileTreeDiff.filesToRemoveBefore +
            diffResult.etcRelatedFileTreeDiff.filesToRemoveBefore
    check(overwrite || conflicts.isEmpty()) {
      buildString {
        appendLine("Refusing to sync: the following paths must be removed first:")
        conflicts.forEach { appendLine("  ${it.path}") }
        append("Re-run with --overwrite to allow removal (dangerous).")
      }
    }
    if (conflicts.isNotEmpty()) {
      println("[sync] overwrite enabled, ${conflicts.size} conflicting path(s) will be removed")
    }

    if (featureFlags.pacmanEnabled) syncPacman(diffResult, interactive)
    if (featureFlags.aurEnabled) syncAur(diffResult, interactive)

    if (featureFlags.systemdEnabled) syncSystemdServices(diffResult.systemdServiceDiff)
    if (featureFlags.userSystemdEnabled) syncUserSystemdServices(diffResult.userSystemdServiceDiff)

    if (featureFlags.configEnabled) syncFileTree("config", diffResult.configRelatedFileTreeDiff)
    if (featureFlags.homeEnabled) syncFileTree("home", diffResult.homeRelatedFileTreeDiff)

    if (featureFlags.etcEnabled) syncEtcFileTree(diffResult.etcRelatedFileTreeDiff)

    println("[sync] done host=${config.host}")
  }

  private fun syncPacman(diffResult: ConfigurationDiffResult, interactive: Boolean) {
    val toInstall = diffResult.pacmanDiff.declaredNotInstalledPackages.toSet()
    if (toInstall.isEmpty()) {
      println("[sync] pacman: nothing to install")
      return
    }

    println(
        "[sync] pacman: installing ${toInstall.size} package(s): ${toInstall.sorted().joinToString(", ")}"
    )
    pacmanUseCase.installPackages(toInstall, interactive)
    println("[sync] pacman: install finished")
  }

  private fun syncAur(diffResult: ConfigurationDiffResult, interactive: Boolean) {
    val toInstall = diffResult.aurDiff.declaredNotInstalledPackages.toSet()
    if (toInstall.isEmpty()) {
      println("[sync] aur: nothing to install")
      return
    }

    println(
        "[sync] aur: installing ${toInstall.size} package(s): ${toInstall.sorted().joinToString(", ")}"
    )
    pacmanUseCase.installAurPackages(toInstall, interactive)
    println("[sync] aur: install finished")
  }

  private fun syncSystemdServices(diff: SystemdServiceDiff) {
    if (diff.declaredNotEnabledServices.isEmpty()) {
      println("[sync] systemd: nothing to enable")
      return
    }
    for (service in diff.declaredNotEnabledServices) {
      println("[sync] systemd: enabling $service")
      val enabled = systemdUseCase.enableServices(service)
      check(enabled) { "Failed to enable systemd service: $service" }
      println("[sync] systemd: enabled $service")
    }
  }

  private fun syncUserSystemdServices(diff: SystemdServiceDiff) {
    if (diff.declaredNotEnabledServices.isEmpty()) {
      println("[sync] user systemd: nothing to enable")
      return
    }
    for (service in diff.declaredNotEnabledServices) {
      println("[sync] user systemd: enabling $service")
      val enabled = systemdUseCase.enableUserServices(service)
      check(enabled) { "Failed to enable user systemd service: $service" }
      println("[sync] user systemd: enabled $service")
    }
  }

  private fun syncFileTree(rootName: String, diff: ConfigurationFileTreeDiff) {
    println(
        "[sync] $rootName: ${diff.filesToRemoveBefore.size} file(s) to remove, " +
            "${diff.directoriesToCreate.size} dir(s) to create, " +
            "${diff.linksToCreate.size} link(s) to create"
    )

    for (path in diff.filesToRemoveBefore) {
      println("[sync] $rootName: removing ${path.path}")
      val deleted = platformPort.deleteFile(path)
      check(deleted) { "Failed to delete conflicting $rootName path: ${path.path}" }
      println("[sync] $rootName: removed ${path.path}")
    }

    for (dir in diff.directoriesToCreate) {
      println("[sync] $rootName: mkdir ${dir.path}")
      platformPort.mkdirRecursive(dir)
      println("[sync] $rootName: mkdir ok ${dir.path}")
    }

    for (link in diff.linksToCreate) {
      println("[sync] $rootName: link ${link.path} -> ${link.target.path}")
      platformPort.createLink(link)
      println("[sync] $rootName: link ok ${link.path}")
    }
  }

  private fun syncEtcFileTree(diff: ConfigurationFileTreeDiff) {
    println(
        "[sync] etc: ${diff.filesToRemoveBefore.size} file(s) to remove, " +
            "${diff.directoriesToCreate.size} dir(s) to create, " +
            "${diff.linksToCreate.size} file(s) to copy"
    )

    for (path in diff.filesToRemoveBefore) {
      println(
          "[sync] etc: NOW sudo rm -rf ${path.path} is not supported. However sudo cp will overwrite file anyway"
      )
      // e.g. platformPort.sudoDeleteRecursive(path)
    }

    for (dir in diff.directoriesToCreate) {
      println("[sync] etc: sudo mkdir -p ${dir.path}")
      platformPort.sudoMkdirRecursive(dir)
      println("[sync] etc: mkdir ok ${dir.path}")
    }

    for (link in diff.linksToCreate) {
      val from = link.target as File
      val to = File(link.path)
      println("[sync] etc: sudo cp ${from.path} ${to.path}")
      platformPort.sudoCopyFile(from = from, to = to)
      println("[sync] etc: copied ${to.path}")
    }
  }
}
