package mezlogo.llmodify.core.pacman.impl

import mezlogo.llmodify.core.pacman.PacmanUseCase
import mezlogo.llmodify.port.platform.PlatformPort

class PacmanService(val platformPort: PlatformPort) : PacmanUseCase {
  override fun listExplicitlyInstalledPackages(): Set<String> {
    val result = platformPort.executeProcess("pacman", listOf("-Qenq"))

    check(result.code == 0) {
      "pacman -Qenq failed with exit code ${result.code}: ${result.output.trim()}"
    }

    return result.output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
  }

  override fun listExplicitlyForeignInstalledPackages(): Set<String> {
    val result = platformPort.executeProcess("pacman", listOf("-Qemq"))

    check(result.code == 0) {
      "pacman -Qemq failed with exit code ${result.code}: ${result.output.trim()}"
    }

    return result.output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
  }

  override fun installPackages(packages: Set<String>, interactive: Boolean) {
    if (packages.isEmpty()) return

    val args = buildList {
      add("-S")
      add("--needed")
      if (!interactive) add("--noconfirm")
      addAll(packages.sorted())
    }

    if (interactive) {
      val code = platformPort.executeProcessInteractive("sudo", listOf("pacman") + args)
      check(code == 0) { "sudo pacman -S failed with exit code $code" }
    } else {
      val result = platformPort.executeProcess("sudo", listOf("pacman") + args)
      check(result.code == 0) {
        "sudo pacman -S --needed --noconfirm failed with exit code ${result.code}: ${result.output.trim()}"
      }
    }
  }

  override fun installAurPackages(packages: Set<String>, interactive: Boolean) {
    if (packages.isEmpty()) return

    // `yay` must NOT be invoked through `sudo`: AUR helpers refuse to run as root and will call
    // `sudo` themselves for the underlying pacman operations.
    val args = buildList {
      add("-S")
      add("--needed")
      if (!interactive) add("--noconfirm")
      addAll(packages.sorted())
    }

    if (interactive) {
      val code = platformPort.executeProcessInteractive("yay", args)
      check(code == 0) { "yay -S failed with exit code $code" }
    } else {
      val result = platformPort.executeProcess("yay", args)
      check(result.code == 0) {
        "yay -S --needed --noconfirm failed with exit code ${result.code}: ${result.output.trim()}"
      }
    }
  }
}
