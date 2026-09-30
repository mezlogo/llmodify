package mezlogo.llmodify.adapter.prompt.impl

import mezlogo.llmodify.adapter.prompt.BuildPromptUseCase
import mezlogo.llmodify.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.adapter.xmlmodel.PromptTO
import mezlogo.llmodify.port.model.PromptOverrideParameters

/**
 * Take defaults from resource classpath OR from overrides.
 */
class BuildPromptService: BuildPromptUseCase {
    override fun buildPrompt(
        promptOverrideParameters: PromptOverrideParameters,
        context: ContextTO
    ): PromptTO {
        TODO("Not yet implemented")
    }
}