package mezlogo.llmodify.adapter.context

import mezlogo.llmodify.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.port.model.GivenFilesParameters
import mezlogo.llmodify.port.model.TraverseParameters

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