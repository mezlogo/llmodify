package mezlogo.llmodify.prompt.adapter.xmlmodel

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("file")
data class FileTO(
    @XmlCData val content: String,
    @XmlElement(false) val path: String,
    @XmlElement(false) val language: String,
    @XmlElement(false) val module: String,
    @XmlElement(false) val scope: String,
)
