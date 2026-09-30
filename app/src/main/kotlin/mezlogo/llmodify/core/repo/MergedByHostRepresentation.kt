package mezlogo.llmodify.core.repo

import mezlogo.llmodify.core.models.RelativeConfigDirectory

data class MergedByHostRepresentation(
    val pacman: List<String>,
    val aur: List<String>,
    val systemdServices: List<String>,
    val userSystemdServices: List<String>,
    val config: RelativeConfigDirectory? = null,
    val etc: RelativeConfigDirectory? = null,
    val home: RelativeConfigDirectory? = null,
)
