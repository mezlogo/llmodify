package mezlogo.llmodify.core.models

data class FeatureFlags(
    val pacmanEnabled: Boolean,
    val aurEnabled: Boolean,
    val systemdEnabled: Boolean,
    val userSystemdEnabled: Boolean,
    val homeEnabled: Boolean,
    val configEnabled: Boolean,
    val etcEnabled: Boolean,
)
