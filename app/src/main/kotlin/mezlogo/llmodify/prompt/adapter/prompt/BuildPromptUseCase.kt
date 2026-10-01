package mezlogo.llmodify.prompt.adapter.prompt

import mezlogo.llmodify.prompt.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.prompt.adapter.xmlmodel.PromptTO
import mezlogo.llmodify.prompt.model.PromptOverrideParameters

interface BuildPromptUseCase {
    /**
     * Build whole prompt.
     */
    fun buildPrompt(promptOverrideParameters: PromptOverrideParameters, context: ContextTO): PromptTO
}