package mezlogo.llmodify.core.repo.impl

import mezlogo.llmodify.core.configtree.ConfigTreeUseCase
import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.models.HostDeclaration
import mezlogo.llmodify.core.models.Link
import mezlogo.llmodify.core.models.ModuleDeclaration
import mezlogo.llmodify.core.models.RelativeConfigDirectory
import mezlogo.llmodify.core.models.RelativeConfigFile
import mezlogo.llmodify.core.models.RelativeConfigLink
import mezlogo.llmodify.core.repo.HostDefinition
import mezlogo.llmodify.core.repo.MergedByHostRepresentation
import mezlogo.llmodify.core.repo.ModuleDefinition
import mezlogo.llmodify.core.repo.RepoConfig
import mezlogo.llmodify.core.repo.RepoRepresentation
import mezlogo.llmodify.core.repo.RepoUseCase
import mezlogo.llmodify.port.platform.PlatformPort
import net.mamoe.yamlkt.Yaml

class RepoService(
    val platformPort: PlatformPort,
    val yaml: Yaml,
    val configTreeUseCase: ConfigTreeUseCase,
) : RepoUseCase {

  override fun readRepository(config: RepoConfig): RepoRepresentation {
    val repo = config.repo
    return RepoRepresentation(
        path = repo,
        hosts = readHosts(repo),
        modules = readModules(repo),
    )
  }

  override fun getMergedByHostRepresentation(
      config: RepoConfig,
      throwOnConflict: Boolean,
      featureFlags: FeatureFlags,
  ): MergedByHostRepresentation {
    val repo = readRepository(config)

    val hostDefinition =
        repo.hosts.find { it.name == config.host }
            ?: error("Host '${config.host}' is not declared in the repository")

    // Resolve modules referenced by the host, skipping ones whose directories do not exist.
    val modulesByName = repo.modules.associateBy { it.module }
    val modules = hostDefinition.modules.mapNotNull { modulesByName[it] }

    // Pacman packages: deduplicated and stable-sorted so callers get deterministic output.
    val pacman =
        if (featureFlags.pacmanEnabled) modules.flatMap { it.pacman }.distinct().sorted()
        else emptyList()

    // AUR packages: deduplicated and stable-sorted so callers get deterministic output.
    val aur =
        if (featureFlags.aurEnabled) modules.flatMap { it.aur }.distinct().sorted() else emptyList()

    // Systemd services: deduplicated and stable-sorted.
    val systemdServices =
        if (featureFlags.systemdEnabled) modules.flatMap { it.systemdServices }.distinct().sorted()
        else emptyList()
    val userSystemdServices =
        if (featureFlags.userSystemdEnabled)
            modules.flatMap { it.userSystemdServices }.distinct().sorted()
        else emptyList()

    // Config trees: merge all module configs (later modules override earlier ones on conflict).
    val configDirs = modules.mapNotNull { it.config }
    val mergedConfig =
        if (featureFlags.configEnabled && configDirs.isNotEmpty())
            configTreeUseCase.merge(configDirs, throwOnConflict)
        else null

    // Home trees: same treatment.
    val homeDirs = modules.mapNotNull { it.home }
    val mergedHome =
        if (featureFlags.homeEnabled && homeDirs.isNotEmpty())
            configTreeUseCase.merge(homeDirs, throwOnConflict)
        else null

    // Etc trees: same treatment.
    val etcDirs = modules.mapNotNull { it.etc }
    val mergedEtc =
        if (featureFlags.etcEnabled && etcDirs.isNotEmpty())
            configTreeUseCase.merge(etcDirs, throwOnConflict)
        else null

    return MergedByHostRepresentation(
        pacman = pacman,
        aur = aur,
        systemdServices = systemdServices,
        userSystemdServices = userSystemdServices,
        config = mergedConfig,
        home = mergedHome,
        etc = mergedEtc,
    )
  }

  /**
   * List all files in $CONFIG/hosts dir. Each file is a yaml representation HostDeclaration Parse
   * it using yaml. Map it to HostDefinition with name as file name without extension.
   */
  private fun readHosts(repo: Directory): List<HostDefinition> {
    val hostsPath = repo.resolveChild("hosts")
    val hostsDir = platformPort.getFile(hostsPath) as? Directory ?: return emptyList()

    return platformPort
        .listFiles(hostsDir)
        .filterIsInstance<File>()
        .filter { it.path.endsWith(".yaml") || it.path.endsWith(".yml") }
        .map { file ->
          val declaration =
              yaml.decodeFromString(
                  HostDeclaration.serializer(),
                  platformPort.readFileUtf8(file),
              )

          val fileName = file.path.substringAfterLast('/')
          val hostName = fileName.substringBeforeLast('.')

          HostDefinition(
              name = hostName,
              modules = declaration.modules,
          )
        }
        .sortedBy { it.name }
  }

  /**
   * List all files in $CONFIG/modules dir. Each file is a dir with
   * $CONFIG/modules/$MODULE_NAME/module.yaml representation ModuleDeclaration Parse it using yaml.
   * Map it to ModuleDefinition with name as module directory name.
   */
  private fun readModules(repo: Directory): List<ModuleDefinition> {
    val modulesPath = repo.resolveChild("modules")
    val modulesDir: Directory =
        platformPort.getFile(modulesPath) as? Directory ?: return emptyList()

    return platformPort
        .listFiles(modulesDir)
        .filterIsInstance<Directory>()
        .mapNotNull { moduleDir ->
          val moduleName = moduleDir.filename()

          val moduleFilePath = moduleDir.resolveChild("module.yaml")
          val moduleFile = platformPort.getFile(moduleFilePath)
          if (moduleFile !is File) return@mapNotNull null

          val declaration =
              yaml.decodeFromString(
                  ModuleDeclaration.serializer(),
                  platformPort.readFileUtf8(moduleFile),
              )

          val configs =
              when (val root = platformPort.getFile(moduleDir.resolveChild("config"))) {
                is Directory -> readRelativeFiles(root)
                else -> null
              }

          val home =
              when (val root = platformPort.getFile(moduleDir.resolveChild("home"))) {
                is Directory -> readRelativeFiles(root)
                else -> null
              }

          val etc =
              when (val root = platformPort.getFile(moduleDir.resolveChild("etc"))) {
                is Directory -> readRelativeFiles(root)
                else -> null
              }

          ModuleDefinition(
              module = moduleName,
              pacman = declaration.pacman ?: emptyList(),
              aur = declaration.aur ?: emptyList(),
              systemdServices = declaration.systemd ?: emptyList(),
              userSystemdServices = declaration.usersystemd ?: emptyList(),
              config = configs,
              home = home,
              etc = etc,
          )
        }
        .sortedBy { it.module }
  }

  /** null if dir does not exist. build a tree files from rootDir. */
  private fun readRelativeFiles(rootDir: Directory): RelativeConfigDirectory? {
    val root = platformPort.getFile(rootDir.path) as? Directory ?: return null

    fun buildDirectory(
        absolute: Directory,
        relative: Directory,
        parent: Directory?,
    ): RelativeConfigDirectory {
      val children =
          platformPort
              .listFiles(absolute)
              .sortedBy { it.filename() }
              .map { child ->
                val name = child.filename()
                when (child) {
                  is Directory ->
                      buildDirectory(
                          absolute = child,
                          relative = Directory(name),
                          parent = relative,
                      )

                  is File ->
                      RelativeConfigFile(
                          absoluteFile = child,
                          relativeFile = File(name),
                          parent = relative,
                      )

                  is Link ->
                      RelativeConfigLink(
                          absoluteLink = child,
                          relativeLink = File(name),
                          parent = relative,
                      )
                }
              }

      return RelativeConfigDirectory(
          absoluteDirectory = absolute,
          relativeDirectory = relative,
          children = children,
          parent = parent,
      )
    }

    return buildDirectory(
        absolute = root,
        relative = Directory(""),
        parent = null,
    )
  }
}
