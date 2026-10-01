package mezlogo.llmodify.prompt.adapter.xmlmodel

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("system")
data class SystemPromptTO(
    @XmlCData
    val content: String,
)