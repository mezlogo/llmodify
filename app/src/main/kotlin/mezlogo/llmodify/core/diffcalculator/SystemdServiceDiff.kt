package mezlogo.llmodify.core.diffcalculator

data class SystemdServiceDiff(
    val declaredServices: List<String>,
    val declaredNotEnabledServices: List<String>,
    val notDeclaredEnabledServices: List<String>,
)
