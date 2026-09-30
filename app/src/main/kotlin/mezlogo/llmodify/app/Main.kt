package mezlogo.llmodify.app

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import mezlogo.llmodify.adapter.context.BuildContextUseCase
import mezlogo.llmodify.adapter.context.impl.BuildContextService
import mezlogo.llmodify.adapter.prompt.BuildPromptUseCase
import mezlogo.llmodify.adapter.prompt.impl.BuildPromptService
import mezlogo.llmodify.app.command.ContextCommand
import mezlogo.llmodify.app.command.LlmModifyRootCommand
import mezlogo.llmodify.app.command.PromptCommand

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val buildContextUseCase: BuildContextUseCase = BuildContextService()
        val buildPromptUseCase: BuildPromptUseCase = BuildPromptService()

        LlmModifyRootCommand()
            .subcommands(
                ContextCommand(buildContextUseCase),
                PromptCommand(buildContextUseCase, buildPromptUseCase),
                )
            .completionOption()
            .main(args)
    }
}