package mezlogo.llmodify.app

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import mezlogo.llmodify.app.command.ContextCommand
import mezlogo.llmodify.app.command.LlmModifyRootCommand

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        LlmModifyRootCommand()
            .subcommands(ContextCommand())
            .completionOption()
            .main(args)
    }
}