package mezlogo.llmodify.modify.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("modify")
data class ModifyTO(
    @XmlValue
    @XmlCData val content: String,
    @XmlElement(false) val path: String,
    @XmlElement(false) @XmlSerialName("line_start") val lineStart: String,
    @XmlElement(false) @XmlSerialName("replace_lines") val replaceLines: String,
    @XmlElement(false) val description: String? = null,
)
