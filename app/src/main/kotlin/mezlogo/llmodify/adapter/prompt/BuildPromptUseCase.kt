package mezlogo.llmodify.adapter.prompt

import mezlogo.llmodify.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.adapter.xmlmodel.PromptTO
import mezlogo.llmodify.port.model.GivenFilesParameters
import mezlogo.llmodify.port.model.PromptOverrideParameters
import mezlogo.llmodify.port.model.TraverseParameters

interface BuildPromptUseCase {
    /**
     * Build whole prompt.
     */
    fun buildPrompt(promptOverrideParameters: PromptOverrideParameters, context: ContextTO): PromptTO
}