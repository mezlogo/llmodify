package mezlogo.llmodify.modify.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("patch")
data class PatchTO(
    @XmlElement(true) val modify: List<ModifyTO> = emptyList(),
    @XmlElement(true) val write: List<WriteTO> = emptyList(),
    @XmlElement(true) val move: List<MoveTO> = emptyList(),
    @XmlElement(true) val delete: List<DeleteTO> = emptyList(),
    @XmlElement(false) val description: String? = null,
)
