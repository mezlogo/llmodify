package mezlogo.llmodify.core.configtree.impl

import mezlogo.llmodify.core.configtree.CompareFileResult
import mezlogo.llmodify.core.configtree.CompareFileStatus
import mezlogo.llmodify.core.configtree.ConfigTreeUseCase
import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.models.Path
import mezlogo.llmodify.core.models.RelativeConfig
import mezlogo.llmodify.core.models.RelativeConfigDirectory
import mezlogo.llmodify.core.models.RelativeConfigFile
import mezlogo.llmodify.core.models.RelativeConfigLink

class ConfigTreeService : ConfigTreeUseCase {

  override fun merge(
      configs: List<RelativeConfigDirectory>,
      throwOnConflict: Boolean,
  ): RelativeConfigDirectory {
    require(configs.isNotEmpty()) { "Cannot merge an empty list of configs" }

    val base = configs.first()
    val mergedChildren = mergeChildren(configs.map { it.children }, throwOnConflict)
    return base.copy(children = mergedChildren)
  }

  override fun compareRelativeFileTree(
      desire: RelativeConfigDirectory,
      real: RelativeConfigDirectory,
  ): List<CompareFileResult> {
    // Index every node in the real tree by its relative path.
    val realByPath = mutableMapOf<String, RelativeConfig>()
    collectReal(real, "", realByPath)

    val results = mutableListOf<CompareFileResult>()
    compareDesired(desire, "", real, realByPath, results)
    return results
  }

  private fun collectReal(
      dir: RelativeConfigDirectory,
      prefix: String,
      into: MutableMap<String, RelativeConfig>,
  ) {
    for (child in dir.children) {
      when (child) {
        is RelativeConfigFile -> {
          val name = child.relativeFile.path
          into[joinRelative(prefix, name)] = child
        }
        is RelativeConfigDirectory -> {
          val name = child.relativeDirectory.path
          val path = joinRelative(prefix, name)
          into[path] = child
          collectReal(child, path, into)
        }
        is RelativeConfigLink -> {
          val name = child.relativeLink.path
          into[joinRelative(prefix, name)] = child
        }
      }
    }
  }

  private fun compareDesired(
      dir: RelativeConfigDirectory,
      prefix: String,
      realRoot: RelativeConfigDirectory,
      realByPath: Map<String, RelativeConfig>,
      results: MutableList<CompareFileResult>,
  ) {
    for (child in dir.children) {
      when (child) {
        is RelativeConfigFile -> {
          val name = child.relativeFile.path
          val relPath = joinRelative(prefix, name)
          val declared = child.absoluteFile
          val realNode = realByPath[relPath]

          if (realNode == null) {
            val expectedReal = resolveRelative(realRoot.absoluteDirectory, relPath)
            results.add(
                CompareFileResult(
                    declared,
                    expectedReal,
                    CompareFileStatus.FILE_DOES_NOT_EXIST,
                )
            )
          } else {
            when (realNode) {
              is RelativeConfigDirectory -> {
                results.add(
                    CompareFileResult(
                        declared,
                        realNode.absoluteDirectory,
                        CompareFileStatus.EXPECTED_FILE_IS_A_DIRECTORY,
                    )
                )
              }
              is RelativeConfigFile -> {
                results.add(
                    CompareFileResult(
                        declared,
                        realNode.absoluteFile,
                        CompareFileStatus.FILE_IS_REGULAR_FILE,
                    )
                )
              }
              is RelativeConfigLink -> {
                val link = realNode.absoluteLink
                val status =
                    if (link.target.path == declared.path) {
                      CompareFileStatus.FILE_IS_LINK_TO_DECLARED_FILE
                    } else {
                      CompareFileStatus.FILE_IS_LINK_TO_ANOTHER_FILE
                    }
                results.add(CompareFileResult(declared, link, status))
              }
            }
          }
        }
        is RelativeConfigDirectory -> {
          val name = child.relativeDirectory.path
          compareDesired(
              child,
              joinRelative(prefix, name),
              realRoot,
              realByPath,
              results,
          )
        }
        is RelativeConfigLink -> {
          // The desired tree normally contains only regular files.
          // If a link ever appears here, treat it like a file.
        }
      }
    }
  }

  private fun resolveRelative(root: Directory, relativePath: String): Path {
    val base = root.path.trimEnd('/')
    val full = if (base.isEmpty()) "/$relativePath" else "$base/$relativePath"
    return File(full)
  }

  private fun joinRelative(prefix: String, name: String): String =
      if (prefix.isEmpty()) name else "$prefix/$name"

  private fun mergeChildren(
      childrenLists: List<List<RelativeConfig>>,
      throwOnConflict: Boolean,
  ): List<RelativeConfig> {
    val result = LinkedHashMap<String, RelativeConfig>()

    for (children in childrenLists) {
      for (child in children) {
        val name = nameOf(child)
        val existing = result[name]
        if (existing == null) {
          result[name] = child
        } else {
          if (throwOnConflict) {
            error("Conflict for '$name'")
          }
          result[name] = mergeConflict(existing, child, throwOnConflict)
        }
      }
    }

    return result.values.sortedBy { nameOf(it) }
  }

  private fun mergeConflict(
      existing: RelativeConfig,
      new: RelativeConfig,
      throwOnConflict: Boolean,
  ): RelativeConfig {
    return if (existing is RelativeConfigDirectory && new is RelativeConfigDirectory) {
      val mergedChildren = mergeChildren(listOf(existing.children, new.children), throwOnConflict)
      new.copy(children = mergedChildren)
    } else {
      new
    }
  }

  private fun nameOf(config: RelativeConfig): String =
      when (config) {
        is RelativeConfigFile -> config.relativeFile.path
        is RelativeConfigDirectory -> config.relativeDirectory.path
        is RelativeConfigLink -> config.relativeLink.path
      }
}
