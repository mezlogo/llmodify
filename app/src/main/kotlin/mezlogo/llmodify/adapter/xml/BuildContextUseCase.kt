package mezlogo.llmodify.adapter.xml

import mezlogo.llmodify.adapter.xml.model.ContextTO

interface BuildContextUseCase {
    fun buildContext(): ContextTO
}