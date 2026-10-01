package mezlogo.llmodify.prompt.adapter.xmlmodel

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("prompt")
data class PromptTO(
    val system: SystemPromptTO,
    val user: UserPromptTO,
)
