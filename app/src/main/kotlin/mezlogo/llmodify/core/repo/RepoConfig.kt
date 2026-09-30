package mezlogo.llmodify.core.repo

import mezlogo.llmodify.core.models.Directory

data class RepoConfig(
    val host: String,
    val home: Directory,
    val repo: Directory,
    val config: Directory,
    val etc: Directory,
)
