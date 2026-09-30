package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import mezlogo.llmodify.adapter.context.BuildContextUseCase
import mezlogo.llmodify.adapter.prompt.BuildPromptUseCase
import mezlogo.llmodify.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.adapter.xmlmodel.PromptTO
import mezlogo.llmodify.port.model.GivenFilesParameters
import mezlogo.llmodify.port.model.PromptOverrideParameters
import mezlogo.llmodify.port.model.TraverseParameters
import nl.adaptivity.xmlutil.serialization.XML
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class PromptCommand(
    val buildContextUseCase: BuildContextUseCase,
    val buildPromptUseCase: BuildPromptUseCase,
) : CliktCommand(name = "prompt") {

    private val output: Path? by option(
        "-o", "--output",
        help = "Write XML prompt to file instead of stdout"
    ).path()

    private val repo: Path by option(
        "-r", "--repo",
        help = "Repository root to traverse"
    ).path().default(Path.of("."))

    private val includeGlobs: List<String> by option(
        "-g", "--glob",
        help = "Include filename glob; repeatable"
    ).multiple()

    private val excludeGlobs: List<String> by option(
        "-e", "--exclude",
        help = "Exclude filename glob; repeatable"
    ).multiple()

    private val stdin: Boolean by option(
        "--stdin",
        help = "When true accept file pathes from stdin. In this mode all other filters are turn off."
    ).flag()

    private val systemPrompt: String? by option(
        "--system",
        help = "Override system prompt"
    )

    private val instructions: String? by option(
        "-i", "--instructions",
        help = "Override user instructions"
    )

    override fun run() {
        val context: ContextTO = buildContext()

        val prompt: PromptTO = buildPromptUseCase.buildPrompt(
            buildPromptOverrideParameters(),
            context,
        )

        val xml = XML.v1 { setIndent(2) }.encodeToString(PromptTO.serializer(), prompt)

        val out = output
        if (out == null) {
            echo(xml)
        } else {
            out.parent?.createDirectories()
            out.writeText(xml)
        }
    }

    private fun buildContext(): ContextTO {
        return if (stdin) {
            val givenFiles = generateSequence { readlnOrNull() }
                .mapNotNull { it.trim() }
                .filter { it.isNotEmpty() }
                .map { Path.of(it) }
                .toList()

            val parameters = GivenFilesParameters(
                contextRoot = repo,
                givenFiles = givenFiles,
            )
            buildContextUseCase.buildContext(parameters)
        } else {
            buildContextUseCase.buildContext(buildTraverseParameters())
        }
    }

    private fun buildTraverseParameters(): TraverseParameters {
        return TraverseParameters(
            contextRoot = repo,
            includeGlobs = includeGlobs,
            excludeGlobs = excludeGlobs,
        )
    }

    private fun buildPromptOverrideParameters(): PromptOverrideParameters {
        return PromptOverrideParameters(
            instructions = instructions,
            systemPrompt = systemPrompt,
        )
    }
}