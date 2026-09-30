package mezlogo.llmodify.adapter.xml.impl

import mezlogo.llmodify.adapter.xml.BuildContextUseCase
import mezlogo.llmodify.adapter.xml.model.ContextTO
import mezlogo.llmodify.port.model.ContextConfig

/**
 * Use kotlin-jvm file io for traverse files, filter by globs, exclude by globs,
 */
class BuildContextService: BuildContextUseCase {
    override fun buildContext(contextConfig: ContextConfig): ContextTO {
        TODO("Not yet implemented")
    }

    /**
     * This function implements all glob related things.
     */
    fun testGlob(fileName: String, glob: String): Boolean {
        TODO("IMPLEMENT")
    }
}