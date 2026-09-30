package mezlogo.llmodify.core.models

import kotlinx.serialization.Serializable

@Serializable
data class HostDeclaration(
    val name: String? = null,
    val description: String? = null,
    val modules: List<String>,
)
