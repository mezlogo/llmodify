package mezlogo.llmodify.adapter.xml.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("Context")
data class ContextTO(
    @XmlElement(false)
    val repo: String,
    val files: List<FileTO>,
)