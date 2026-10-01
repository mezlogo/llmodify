package mezlogo.llmodify.modify.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("move")
data class MoveTO(
    @XmlElement(false)
    val from: String,

    @XmlElement(false)
    val to: String,
)