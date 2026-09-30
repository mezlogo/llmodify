package mezlogo.llmodify.core.repo

import mezlogo.llmodify.core.models.RelativeConfigDirectory

data class ModuleDefinition(
    val module: String,
    val pacman: List<String>,
    val aur: List<String>,
    val systemdServices: List<String>,
    val userSystemdServices: List<String>,
    val etc: RelativeConfigDirectory? = null,
    val config: RelativeConfigDirectory? = null,
    val home: RelativeConfigDirectory? = null,
)
