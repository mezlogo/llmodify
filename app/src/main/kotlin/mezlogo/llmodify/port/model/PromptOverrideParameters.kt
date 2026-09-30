package mezlogo.llmodify.port.model

data class PromptOverrideParameters(
    val instructions: String,
    val systemPrompt: String? = null,
)
