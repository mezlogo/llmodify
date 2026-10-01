package mezlogo.llmodify.modify.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("delete")
data class DeleteTO(
    @XmlElement(false)
    val path: String,
)