package mezlogo.llmodify.core.repo

import mezlogo.llmodify.core.models.Directory

data class RepoRepresentation(
    val path: Directory,
    val hosts: List<HostDefinition>,
    val modules: List<ModuleDefinition>,
)
