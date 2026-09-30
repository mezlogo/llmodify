package mezlogo.llmodify.core.diffcalculator

data class ConfigurationDiffResult(
    val pacmanDiff: ConfigurationPacmanDiff,
    val aurDiff: ConfigurationPacmanDiff,

    /** $HOME/.config related */
    val configRelatedFileTreeDiff: ConfigurationFileTreeDiff,

    /** $HOME related fields */
    val homeRelatedFileTreeDiff: ConfigurationFileTreeDiff,

    /** /etc related */
    val etcRelatedFileTreeDiff: ConfigurationFileTreeDiff,

    /** system-wide systemd service diff */
    val systemdServiceDiff: SystemdServiceDiff,

    /** user-wide systemd service diff */
    val userSystemdServiceDiff: SystemdServiceDiff,
)
