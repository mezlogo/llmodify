package mezlogo.llmodify.app

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import mezlogo.llmodify.adapter.xml.BuildContextUseCase
import mezlogo.llmodify.adapter.xml.impl.BuildContextService
import mezlogo.llmodify.app.command.ContextCommand
import mezlogo.llmodify.app.command.LlmModifyRootCommand

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val buildContextUseCase: BuildContextUseCase = BuildContextService()

        LlmModifyRootCommand()
            .subcommands(ContextCommand(buildContextUseCase))
            .completionOption()
            .main(args)
    }
}