package mezlogo.llmodify.core.models

/** Right now only posix systems supported. */
const val FS_SEPARATOR: Char = '/'

/**
 * This is a simple representation for file system, cos in kotlin multiplatform there is not
 * java.Path, java.File, e.t.c
 */
sealed interface Path {
  val path: String
  val type: FileType

  fun resolveChild(name: String): String {
    require(name.isNotEmpty()) { "Child name must not be empty" }

    if (this !is Directory) {
      error("Cannot create a child of a $type path: $path")
    }

    val childName = name.trimEnd(FS_SEPARATOR)
    require(childName.isNotEmpty()) { "Child name must not be empty" }

    val base = path.trimEnd('/')
    val childPath =
        when {
          base.isEmpty() -> "$FS_SEPARATOR$childName"
          else -> "$base$FS_SEPARATOR$childName"
        }

    return childPath
  }

  fun parent(): Directory {
    val normalized = path.trimEnd('/')

    if (normalized.isEmpty()) {
      return if (path.isNotEmpty()) {
        Directory(FS_SEPARATOR.toString())
      } else {
        error("Path has no parent: '$path'")
      }
    }

    val lastSeparator = normalized.lastIndexOf(FS_SEPARATOR)
    val parentPath =
        when {
          lastSeparator < 0 -> "."
          lastSeparator == 0 -> FS_SEPARATOR.toString()
          else -> normalized.substring(0, lastSeparator)
        }

    return Directory(parentPath)
  }

  fun filename(): String = path.substringAfterLast(FS_SEPARATOR)
}

enum class FileType {
  LINK,
  FILE,
  DIRECTORY,
}

data class File(override val path: String, override val type: FileType = FileType.FILE) : Path

data class Link(
    override val path: String,
    override val type: FileType = FileType.LINK,
    val target: Path,
) : Path

data class Directory(override val path: String, override val type: FileType = FileType.DIRECTORY) :
    Path
