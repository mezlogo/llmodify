package mezlogo.llmodify.app.command

import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.RelativeConfigDirectory
import mezlogo.llmodify.core.models.RelativeConfigFile
import mezlogo.llmodify.core.models.RelativeConfigLink
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.repo.RepoUseCase
import mezlogo.llmodify.port.platform.PlatformPort

/**
 * command: dump
 *
 * Dump all hosts and modules in repository
 *
 * dump ignores FeatureFlags by design.
 */
class DumpCommand(
    platformPort: PlatformPort,
    val repoUseCase: RepoUseCase,
) : AbstractArchSyncCommand("dump", platformPort) {

  override fun runRepoCallback(config: RepoConfig, featureFlags: FeatureFlags) {
    val repo = repoUseCase.readRepository(config)

    echo("Repository: ${repo.path.path}")
    echo("Host: ${config.host}")
    echo("")

    echo("Hosts (${repo.hosts.size}):")
    if (repo.hosts.isEmpty()) {
      echo("  (none)")
    } else {
      for (host in repo.hosts) {
        val modules = host.modules.joinToString(", ")
        echo("  ${host.name}: [$modules]")
      }
    }
    echo("")

    echo("Modules (${repo.modules.size}):")
    if (repo.modules.isEmpty()) {
      echo("  (none)")
    } else {
      for (module in repo.modules) {
        val pacman = module.pacman.joinToString(", ")
        echo("pacman:  ${module.module}: pacman=[$pacman]")

        val aur = module.pacman.joinToString(", ")
        echo("aur:  ${module.module}: pacman=[$aur]")

        val configFiles = module.config?.let(::collectRelativeFiles) ?: emptyList()
        echo("    config (${configFiles.size}):")
        if (configFiles.isEmpty()) {
          echo("      (none)")
        } else {
          for (file in configFiles) {
            echo("      $file")
          }
        }

        val homeFiles = module.home?.let(::collectRelativeFiles) ?: emptyList()
        echo("    home (${homeFiles.size}):")
        if (homeFiles.isEmpty()) {
          echo("      (none)")
        } else {
          for (file in homeFiles) {
            echo("      $file")
          }
        }
      }
    }
  }

  /**
   * Walk a [RelativeConfigDirectory] tree and return the paths of every file, relative to the
   * directory root (e.g. "myapp/myapp.cfg").
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

        is RelativeConfigLink -> TODO("IMPLEMENT")
      }
    }

    return result
  }
}
