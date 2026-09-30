package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.port.platform.PlatformPort

abstract class AbstractArchSyncCommand(name: String, val platformPort: PlatformPort) :
    CliktCommand(name) {

  val host: String? by
      option(
          help = "Hostname for automatic host declaration detection. Env: ARCHSYNC_HOST",
          envvar = "ARCHSYNC_HOST",
      )

  val repo: String? by
      option(
          help = "Repository with modules. Env: ARCHSYNC_REPO",
          envvar = "ARCHSYNC_REPO",
      )

  val home: String? by
      option(
          help =
              "All files under \$module/home will be compared with this folder. Env: ARCHSYNC_USER_HOME",
          envvar = "ARCHSYNC_USER_HOME",
      )

  val config: String? by
      option(
          help =
              "All files under \$module/config will be compared with this folder. Env: ARCHSYNC_USER_CONFIG",
          envvar = "ARCHSYNC_USER_CONFIG",
      )

  val etc: String? by
      option(
          help =
              "All files under \$module/etc will be compared with this folder. Env: ARCHSYNC_USER_ETC",
          envvar = "ARCHSYNC_USER_ETC",
      )

  val onlyPacman: Boolean? by
      option(
              help = "Include Pacman. Env: ARCHSYNC_USER_ONLY_PACMAN",
              envvar = "ARCHSYNC_USER_ONLY_PACMAN",
          )
          .flag()
  val onlyAur: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()
  val onlySystemd: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()
  val onlyUserSystemd: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()
  val onlyConfig: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()
  val onlyHome: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()
  val onlyEtc: Boolean? by
      option(help = "Include AUR. Env: ARCHSYNC_USER_ONLY_AUR", envvar = "ARCHSYNC_USER_ONLY_AUR")
          .flag()

  override fun run() {
    val config = buildConfig()
    val featureFlags = buildFeatureFlags()
    runRepoCallback(config, featureFlags)
  }

  abstract fun runRepoCallback(config: RepoConfig, featureFlags: FeatureFlags)

  /** If ANY only flag has true -> include ONLY this flags. Else include all true. */
  fun buildFeatureFlags(): FeatureFlags {
    // `null` means "not passed"; `false` means "explicitly excluded".
    val onlyFlags =
        listOf(
            onlyPacman,
            onlyAur,
            onlySystemd,
            onlyUserSystemd,
            onlyConfig,
            onlyHome,
            onlyEtc,
        )

    // If at least one `--only-*` flag is set to true, we are in "only mode" and every feature
    // that is not explicitly requested is disabled. Otherwise, everything is enabled.
    val onlyMode = onlyFlags.any { it == true }

    fun enabled(flag: Boolean?): Boolean = if (onlyMode) flag == true else true

    return FeatureFlags(
        pacmanEnabled = enabled(onlyPacman),
        aurEnabled = enabled(onlyAur),
        systemdEnabled = enabled(onlySystemd),
        userSystemdEnabled = enabled(onlyUserSystemd),
        homeEnabled = enabled(onlyHome),
        configEnabled = enabled(onlyConfig),
        etcEnabled = enabled(onlyEtc),
    )
  }

  fun buildConfig(): RepoConfig {
    val homePath =
        home ?: platformPort.getEnv("HOME") ?: error("HOME is not set and --home was not provided")

    val repoPath = repo ?: "$homePath/.config/archsync"
    val configPath = config ?: "$homePath/.config"
    val etcPath = etc ?: "/etc"

    val hostValue =
        host
            ?: runCatching { platformPort.readFileUtf8(File("/etc/hostname")).trim() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            ?: "localhost"

    return RepoConfig(
        host = hostValue,
        home = Directory(platformPort.getAbsolutePath(homePath)),
        etc = Directory(platformPort.getAbsolutePath(etcPath)),
        repo = Directory(platformPort.getAbsolutePath(repoPath)),
        config = Directory(platformPort.getAbsolutePath(configPath)),
    )
  }
}
