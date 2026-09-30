package mezlogo.llmodify.adapter.xml.impl

import mezlogo.llmodify.adapter.xml.BuildContextUseCase
import mezlogo.llmodify.adapter.xml.model.ContextTO
import mezlogo.llmodify.adapter.xml.model.FileTO
import mezlogo.llmodify.port.model.GivenFilesParameters
import mezlogo.llmodify.port.model.TraverseParameters
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * Use kotlin-jvm file io for traverse files, filter by globs, exclude by globs,
 */
class BuildContextService : BuildContextUseCase {
    override fun buildContext(traverseParameters: TraverseParameters): ContextTO {
        val contextRoot = traverseParameters.contextRoot
        val includeGlobs = traverseParameters.includeGlobs
        val excludeGlobs = traverseParameters.excludeGlobs

        val files = Files.walk(contextRoot).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { file ->
                    val fileName = file.fileName.toString()
                    val included = includeGlobs.isEmpty() || includeGlobs.any { testGlob(fileName, it) }
                    val excluded = excludeGlobs.isNotEmpty() && excludeGlobs.any { testGlob(fileName, it) }
                    included && !excluded
                }
                .sorted()
                .map { file -> buildFile(contextRoot, file) }
                .collect(Collectors.toList())
        }

        return ContextTO(
            repo = contextRoot.toAbsolutePath().normalize().toString().replace('\\', '/'),
            files = files,
        )
    }

    override fun buildContext(
        givenFilesParameters: GivenFilesParameters,
    ): ContextTO {
        val contextRoot = givenFilesParameters.contextRoot

        val files = givenFilesParameters.givenFiles
            .map { path -> if (path.isAbsolute) path else contextRoot.resolve(path) }
            .map { it.normalize() }
            .filter { Files.isRegularFile(it) }
            .map { file -> buildFile(contextRoot, file) }
            .sortedBy { it.path }

        return ContextTO(
            repo = contextRoot.toAbsolutePath().normalize().toString().replace('\\', '/'),
            files = files,
        )
    }

    /**
     * This function implements all glob related things.
     */
    fun testGlob(fileName: String, glob: String): Boolean {
        return FileSystems.getDefault()
            .getPathMatcher("glob:$glob")
            .matches(Paths.get(fileName))
    }

    private fun buildFile(contextRoot: Path, file: Path): FileTO {
        val relativePath = contextRoot.relativize(file).toString().replace('\\', '/')
        val content = Files.readAllBytes(file).toString(Charsets.UTF_8)

        return FileTO(
            content = withLineNumbers(content),
            path = relativePath,
            language = languageOf(file.fileName.toString()),
            module = "",
            scope = "",
        )
    }

    private fun withLineNumbers(content: String): String {
        if (content.isEmpty()) return content

        return content.lines()
            .mapIndexed { index, line -> "${index + 1}|$line" }
            .joinToString("\n")
    }

    private fun languageOf(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "md" -> "markdown"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            "json" -> "json"
            "gradle" -> "gradle"
            else -> if (extension.isEmpty()) "text" else extension
        }
    }
}