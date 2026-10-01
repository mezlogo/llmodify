package mezlogo.llmodify.prompt.adapter.xmlmodel

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("instructions")
data class InstructionsTO(
    @XmlCData
    val content: String,
)