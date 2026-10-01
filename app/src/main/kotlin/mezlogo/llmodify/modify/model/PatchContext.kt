package mezlogo.llmodify.modify.model

import java.nio.file.Path

data class PatchContext(
    /** Root for all relative path in modifications. */
    val contextRoot: Path,
)
