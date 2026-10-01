package mezlogo.llmodify.modify.model

import nl.adaptivity.xmlutil.serialization.XmlElement
import java.nio.file.Path

data class PatchContext(
    /** Root for all relative path in modifications. */
    val contextRoot: Path,
    @XmlElement(false) val description: String? = null,
)
