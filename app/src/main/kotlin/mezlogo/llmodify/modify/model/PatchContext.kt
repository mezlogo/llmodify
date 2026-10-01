package mezlogo.llmodify.modify.model

import java.nio.file.Path
import nl.adaptivity.xmlutil.serialization.XmlElement

data class PatchContext(
    /** Root for all relative path in modifications. */
    val contextRoot: Path,
    @XmlElement(false) val description: String? = null,
)
