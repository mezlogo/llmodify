package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import mezlogo.llmodify.prompt.adapter.context.BuildContextUseCase
import mezlogo.llmodify.prompt.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.prompt.model.GivenFilesParameters
import mezlogo.llmodify.prompt.model.TraverseParameters
import nl.adaptivity.xmlutil.serialization.XML

class ContextCommand(
    val buildContextUseCase: BuildContextUseCase,
) : CliktCommand(name = "context") {

  private val output: Path? by
      option(
              "-o",
              "--output",
              help = "Write XML context to file instead of stdout",
          )
          .path()

  private val repo: Path by
      option(
              "-r",
              "--repo",
              help = "Repository root to traverse",
          )
          .path()
          .default(Path.of("."))

  private val includeGlobs: List<String> by
      option(
              "-g",
              "--glob",
              help = "Include filename glob; repeatable",
          )
          .multiple()

  private val excludeGlobs: List<String> by
      option(
              "-e",
              "--exclude",
              help = "Exclude filename glob; repeatable",
          )
          .multiple()

  private val stdin: Boolean by
      option(
              "--stdin",
              help =
                  "When true accept file pathes from stdin. In this mode all other filters are turn off.",
          )
          .flag()

  override fun run() {
    val context: ContextTO =
        if (stdin) {
          val givenFiles =
              generateSequence {
                    readlnOrNull()
                  }
                  .mapNotNull { it.trim() }
                  .filter { it.isNotEmpty() }
                  .map { Path.of(it) }
                  .toList()

          val parameters =
              GivenFilesParameters(
                  contextRoot = repo,
                  givenFiles = givenFiles,
              )
          buildContextUseCase.buildContext(parameters)
        } else {
          val parameters = buildTraverseParameters()
          buildContextUseCase.buildContext(parameters)
        }

    val xml = XML.v1 { setIndent(2) }.encodeToString(ContextTO.serializer(), context)

    val out = output
    if (out == null) {
      echo(xml)
    } else {
      out.parent?.createDirectories()
      out.writeText(xml)
    }
  }

  private fun buildTraverseParameters(): TraverseParameters {
    return TraverseParameters(
        contextRoot = repo,
        includeGlobs = includeGlobs,
        excludeGlobs = excludeGlobs,
    )
  }
}
