package mezlogo.llmodify.adapter.xml.model

import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("prompt")
data class PromptTO(
    @XmlCData
    val content: String,
    @XmlElement(false)
    val path: String,
    @XmlElement(false)
    val language: String,
    @XmlElement(false)
    val module: String,
    @XmlElement(false)
    val scope: String,
)
