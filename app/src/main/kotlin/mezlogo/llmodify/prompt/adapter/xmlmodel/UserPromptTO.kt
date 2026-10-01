package mezlogo.llmodify.prompt.adapter.xmlmodel

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("user")
data class UserPromptTO(
    val context: ContextTO,
    val instructions: InstructionsTO,
)
