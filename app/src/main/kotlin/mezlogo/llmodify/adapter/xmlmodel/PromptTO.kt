package mezlogo.llmodify.adapter.xmlmodel

import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("prompt")
data class PromptTO(
    val system: SystemPromptTO,
    val user: UserPromptTO,
)
