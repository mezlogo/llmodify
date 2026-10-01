package mezlogo.llmodify.modify.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("write")
data class WriteTO(
    @XmlValue @XmlCData val content: String,
    @XmlElement(false) val path: String,
    @XmlElement(false) val description: String? = null,
)
