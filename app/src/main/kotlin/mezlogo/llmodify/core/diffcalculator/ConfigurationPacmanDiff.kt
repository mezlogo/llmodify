package mezlogo.llmodify.core.diffcalculator

/** Pacman related fields. */
data class ConfigurationPacmanDiff(
    val installedExplicitPackages: List<String>,
    val declaredPackages: List<String>,
    val notDeclaredInstalledPackages: List<String>,
    val declaredNotInstalledPackages: List<String>,
)
