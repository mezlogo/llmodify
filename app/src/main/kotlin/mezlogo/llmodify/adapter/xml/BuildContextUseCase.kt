package mezlogo.llmodify.adapter.xml

import mezlogo.llmodify.adapter.xml.model.ContextTO
import mezlogo.llmodify.port.model.ContextConfig

interface BuildContextUseCase {
    fun buildContext(contextConfig: ContextConfig): ContextTO
}