package mezlogo.llmodify.port.platform

import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.models.Link
import mezlogo.llmodify.core.models.Path

/**
 * Port helps to isolate platform specific file related operations for simplicity, testability and
 * portability.
 */
interface PlatformPort {
  fun writeFileUtf8(file: File, content: String)

  fun readFileUtf8(file: File): String

  fun getFile(path: String): Path?

  fun listFiles(dir: Directory): List<Path>

  fun traverseDirectory(dir: Directory): List<Path>

  fun parse(filePath: String): Path

  /** mkdir -p like functionality. */
  fun mkdirRecursive(dir: Directory): Directory

  /** For creating dir tree in /etc */
  fun sudoMkdirRecursive(dir: Directory): Directory

  /** For coping configuration files into /etc */
  fun sudoCopyFile(from: File, to: File)

  /** Compare two files content. Useful for /etc files */
  fun filesAreIdentical(a: File, b: File): Boolean

  fun getAbsolutePath(path: String): String

  fun createLink(link: Link)

  fun deleteFile(path: Path): Boolean

  fun getEnv(env: String): String?

  fun executeProcess(command: String, args: List<String>): ProcessResult

  /**
   * Runs [command] [args] on the controlling terminal:
   * - stdin/stdout/stderr are inherited from the parent process,
   * - the child can prompt the user and render progress bars,
   * - returns the child exit code.
   *
   * Unlike [executeProcess] the output is **not** captured; it goes straight to the tty.
   */
  fun executeProcessInteractive(command: String, args: List<String>): Int
}
