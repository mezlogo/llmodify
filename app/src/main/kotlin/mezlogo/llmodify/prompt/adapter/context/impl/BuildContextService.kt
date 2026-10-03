package mezlogo.llmodify.prompt.adapter.context.impl

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import mezlogo.llmodify.prompt.adapter.context.BuildContextUseCase
import mezlogo.llmodify.prompt.adapter.xmlmodel.ContextTO
import mezlogo.llmodify.prompt.adapter.xmlmodel.FileTO
import mezlogo.llmodify.prompt.model.GivenFilesParameters
import mezlogo.llmodify.prompt.model.TraverseParameters

/** Use kotlin-jvm file io for traverse files, filter by globs, exclude by globs, */
class BuildContextService : BuildContextUseCase {
  override fun buildContext(traverseParameters: TraverseParameters): ContextTO {
    val contextRoot = traverseParameters.contextRoot
    val includeGlobs = traverseParameters.includeGlobs
    val excludeGlobs = traverseParameters.excludeGlobs
    val includePaths = traverseParameters.includePaths
    val gitignorePatterns =
        if (traverseParameters.respectGitignore) readGitignore(contextRoot) else emptyList()

    val files =
        Files.walk(contextRoot).use { stream ->
          stream
              .filter { Files.isRegularFile(it) }
              .filter { file ->
                val relativePath = contextRoot.relativize(file).toString().replace('\\', '/')
                val fileName = file.fileName.toString()
                val absoluteFile = file.toAbsolutePath().normalize()
                val root = contextRoot.toAbsolutePath().normalize()
                val includedByPath =
                    includePaths.isEmpty() ||
                        includePaths.any { includePath ->
                          val resolved =
                              if (includePath.isAbsolute) includePath.normalize()
                              else root.resolve(includePath).normalize()
                          absoluteFile.startsWith(resolved)
                        }
                val included = includeGlobs.isEmpty() || includeGlobs.any { testGlob(fileName, it) }
                val excluded =
                    excludeGlobs.isNotEmpty() &&
                        excludeGlobs.any {
                          testGlob(fileName, it) || testGlob(relativePath, it)
                        }
                val ignoredByGitignore =
                    gitignorePatterns.isNotEmpty() &&
                        isIgnoredByGitignore(relativePath, fileName, gitignorePatterns)
                includedByPath && included && !excluded && !ignoredByGitignore
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

    val files =
        givenFilesParameters.givenFiles
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

  /** This function implements all glob related things. */
  fun testGlob(fileName: String, glob: String): Boolean {
    return FileSystems.getDefault().getPathMatcher("glob:$glob").matches(Paths.get(fileName))
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

    return content.lines().mapIndexed { index, line -> "${index + 1}|$line" }.joinToString("\n")
  }

  private fun languageOf(fileName: String): String {
    return when (val extension = fileName.substringAfterLast('.', "").lowercase()) {
      "kt",
      "kts" -> "kotlin"
      "java" -> "java"
      "py" -> "python"
      "js" -> "javascript"
      "ts" -> "typescript"
      "md" -> "markdown"
      "xml" -> "xml"
      "yaml",
      "yml" -> "yaml"
      "json" -> "json"
      "gradle" -> "gradle"
      else -> if (extension.isEmpty()) "text" else extension
    }
  }

  private fun readGitignore(root: Path): List<String> {
    val gitignore = root.resolve(".gitignore")
    if (!Files.isRegularFile(gitignore)) return emptyList()
    return Files.readAllLines(gitignore, Charsets.UTF_8)
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
  }

  private fun isIgnoredByGitignore(
      relativePath: String,
      fileName: String,
      patterns: List<String>,
  ): Boolean {
    var ignored = false
    for (raw in patterns) {
      var pattern = raw
      val negate = pattern.startsWith("!")
      if (negate) pattern = pattern.removePrefix("!")
      if (pattern.startsWith("/")) pattern = pattern.removePrefix("/")
      val matches =
          if (pattern.endsWith("/")) {
            val dir = pattern.removeSuffix("/")
            relativePath == dir || relativePath.startsWith("$dir/")
          } else {
            testGlob(fileName, pattern) || testGlob(relativePath, pattern)
          }
      if (matches) {
        ignored = !negate
      }
    }
    return ignored
  }
}
