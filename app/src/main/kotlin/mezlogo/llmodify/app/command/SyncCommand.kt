package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import mezlogo.llmodify.core.diffcalculator.DiffCalculatorUseCase
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.repo.RepoUseCase
import mezlogo.llmodify.core.sync.SyncUseCase
import mezlogo.llmodify.port.platform.PlatformPort

/**
 * command: sync
 *
 * Sync declared data:
 * - pacman: install packages
 * - configs: create dirs when they are not created and link config files
 */
class SyncCommand(
    platformPort: PlatformPort,
    val repoUseCase: RepoUseCase,
    val diffCalculatorUseCase: DiffCalculatorUseCase,
    val syncUseCase: SyncUseCase,
) : AbstractArchSyncCommand("sync", platformPort) {

  val merge: Boolean by
      option(help = "Merge conflicts in configs or report about conflict").flag(default = false)

  val overwrite: Boolean by
      option(help = "Overwrite real files. IT IS DANGEROUS!").flag(default = false)

  val cleanup: Boolean by
      option(help = "Remove installed packages that are not declared. IT IS DANGEROUS!")
          .flag(default = false)

  /** When set, pacman/yay run on the real tty: no --noconfirm, prompts and output pass through. */
  val interactive: Boolean by
      option(help = "Run pacman/yay interactively (no --noconfirm, inherit tty).")
          .flag(default = false)

  override fun runRepoCallback(config: RepoConfig, featureFlags: FeatureFlags) {
    val throwOnConflict = !merge
    val merged = repoUseCase.getMergedByHostRepresentation(config, throwOnConflict, featureFlags)
    val diffResult = diffCalculatorUseCase.calculateDiff(config, merged, featureFlags)

    syncUseCase.sync(
        config = config,
        diffResult = diffResult,
        featureFlags = featureFlags,
        cleanup = cleanup,
        overwrite = overwrite,
        interactive = interactive,
    )

    echo("Sync completed for host ${config.host}.")
  }
}
