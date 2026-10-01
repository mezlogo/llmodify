package mezlogo.llmodify.modify.model

import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("modify")
data class ModifyTO(
    @XmlCData
    val content: String,

    @XmlElement(false)
    val path: String,

    @XmlElement(false)
    @XmlSerialName("line_start")
    val lineStart: String,

    @XmlElement(false)
    @XmlSerialName("replace_lines")
    val replaceLines: String,
)
