package mezlogo.llmodify.core.pacman

interface PacmanUseCase {

  /** Returns sorted set of explicit installed packages in pacman without aur. Use `pacman -Qenq` */
  fun listExplicitlyInstalledPackages(): Set<String>

  /** Returns set of explicit installed packages in pacman from aur ONLY. Use `pacman -Qemq` */
  fun listExplicitlyForeignInstalledPackages(): Set<String>

  /** Install packages. When [interactive] is true, omit --noconfirm and inherit tty. */
  fun installPackages(packages: Set<String>, interactive: Boolean = false)

  /** Install AUR packages. When [interactive] is true, omit --noconfirm and inherit tty. */
  fun installAurPackages(packages: Set<String>, interactive: Boolean = false)
}
