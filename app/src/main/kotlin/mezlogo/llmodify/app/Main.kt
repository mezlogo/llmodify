package mezlogo.llmodify.app

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import mezlogo.llmodify.app.command.ContextCommand
import mezlogo.llmodify.app.command.LlmModifyRootCommand
import mezlogo.llmodify.app.command.PatchCommand
import mezlogo.llmodify.app.command.PromptCommand
import mezlogo.llmodify.modify.adapter.patch.PatchUseCase
import mezlogo.llmodify.modify.adapter.patch.impl.PatchService
import mezlogo.llmodify.prompt.adapter.context.BuildContextUseCase
import mezlogo.llmodify.prompt.adapter.context.impl.BuildContextService
import mezlogo.llmodify.prompt.adapter.prompt.BuildPromptUseCase
import mezlogo.llmodify.prompt.adapter.prompt.impl.BuildPromptService

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val buildContextUseCase: BuildContextUseCase = BuildContextService()
    val buildPromptUseCase: BuildPromptUseCase = BuildPromptService()
      val patchUseCase: PatchUseCase = PatchService()

      LlmModifyRootCommand()
          .subcommands(
              ContextCommand(buildContextUseCase),
              PromptCommand(buildContextUseCase, buildPromptUseCase),
              PatchCommand(patchUseCase),
          )
          .completionOption()
          .main(args)
  }
}
