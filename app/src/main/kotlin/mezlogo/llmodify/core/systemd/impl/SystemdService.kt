package mezlogo.llmodify.core.systemd.impl

import mezlogo.llmodify.core.systemd.SystemdUseCase
import mezlogo.llmodify.port.platform.PlatformPort

class SystemdService(val platformPort: PlatformPort) : SystemdUseCase {

  override fun getEnabledSystemServices(): Set<String> = listEnabledServices(user = false)

  override fun getEnabledUserServices(): Set<String> = listEnabledServices(user = true)

  override fun enableServices(service: String): Boolean {
    val result = platformPort.executeProcess("sudo", listOf("systemctl", "enable", service))
    return result.code == 0
  }

  override fun enableUserServices(service: String): Boolean {
    val result = platformPort.executeProcess("systemctl", listOf("--user", "enable", service))
    return result.code == 0
  }

  private fun listEnabledServices(user: Boolean): Set<String> {
    val args = buildList {
      add("list-unit-files")
      if (user) add("--user")
      add("--type=service")
      add("--state=enabled")
      add("--no-legend")
    }

    val result = platformPort.executeProcess("systemctl", args)

    //    check(result.code == 0) {
    //      "systemctl ${args.joinToString(" ")} failed with exit code ${result.code}:
    // ${result.output.trim()}"
    //    }

    return if (result.output.isNullOrEmpty())
        result.output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.substringBefore(' ') }
            .filter { it.isNotEmpty() }
            .toSet()
    else emptySet()
  }
}
