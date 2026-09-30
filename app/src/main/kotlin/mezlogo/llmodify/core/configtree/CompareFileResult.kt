package mezlogo.llmodify.core.configtree

import mezlogo.llmodify.core.models.Path

/** This file contains only absolute path in file system, not relative to any roots. */
data class CompareFileResult(
    val declaredFile: Path,
    val realFile: Path,
    val status: CompareFileStatus,
)
