package mezlogo.llmodify.prompt.adapter.context

import mezlogo.llmodify.prompt.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.prompt.model.GivenFilesParameters
import mezlogo.llmodify.prompt.model.TraverseParameters

interface BuildContextUseCase {
    /**
     * Build context based on traverse parameters.
     */
    fun buildContext(traverseParameters: TraverseParameters): ContextTO

    /**
     * Build context based on concrete file pathes.
     */
    fun buildContext(givenFilesParameters: GivenFilesParameters): ContextTO
}