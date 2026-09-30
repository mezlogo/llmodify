package mezlogo.llmodify.core.models

import kotlinx.serialization.Serializable

@Serializable
data class ModuleDeclaration(
    val name: String? = null,
    val description: String? = null,
    val pacman: List<String>? = null,
    val aur: List<String>? = null,
    val systemd: List<String>? = null,
    val usersystemd: List<String>? = null,
)
