package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import mezlogo.llmodify.modify.adapter.patch.PatchUseCase
import java.nio.file.Path

class PatchCommand(
    val patchUseCase: PatchUseCase,
) : CliktCommand(name = "patch") {

  private val repo: Path by
      option(
              "-r",
              "--repo",
              help = "Repository root to traverse",
          )
          .path()
          .default(Path.of("."))

  private val patch: Path? by
      option(
              "-p",
              "--patch",
              help = "Path to xml patch file",
          )
          .path()

  private val stdin: Boolean by
      option(
              "--stdin",
              help = "When true accept xml patch content from stdin",
          )
          .flag()

  override fun run() {}
}
