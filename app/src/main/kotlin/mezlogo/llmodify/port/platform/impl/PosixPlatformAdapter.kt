package mezlogo.llmodify.port.platform.impl

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.File
import mezlogo.llmodify.core.models.Link
import mezlogo.llmodify.core.models.Path
import mezlogo.llmodify.port.platform.PlatformPort
import mezlogo.llmodify.port.platform.ProcessResult
import platform.posix.EEXIST
import platform.posix.ENOENT
import platform.posix.FILE
import platform.posix.S_IFDIR
import platform.posix.S_IFLNK
import platform.posix.S_IFMT
import platform.posix.S_IFREG
import platform.posix.closedir
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.lstat
import platform.posix.memcmp
import platform.posix.mkdir
import platform.posix.opendir
import platform.posix.pclose
import platform.posix.popen
import platform.posix.readdir
import platform.posix.readlink
import platform.posix.remove
import platform.posix.rmdir
import platform.posix.stat
import platform.posix.symlink
import platform.posix.system

@OptIn(ExperimentalForeignApi::class)
class PosixPlatformAdapter : PlatformPort {
  override fun writeFileUtf8(file: File, content: String) {
    val filePath = file.path
    val stream = fopen(filePath, "wb") ?: error("Failed to open file for writing: $filePath")

    try {
      val bytes = content.encodeToByteArray()
      if (bytes.isEmpty()) return

      bytes.usePinned { pinned ->
        val written =
            fwrite(
                pinned.addressOf(0),
                1.convert(),
                bytes.size.convert(),
                stream,
            )

        val totalBytesToWrite = bytes.size.toULong()

        if (written != totalBytesToWrite) {
          error("Failed to write $filePath: wrote $written of ${bytes.size} bytes")
        }
      }
    } finally {
      fclose(stream)
    }
  }

  override fun readFileUtf8(file: File): String {
    val filePath = file.path
    val builder = StringBuilder()

    val filePtr =
        fopen(filePath, "r") ?: throw IllegalArgumentException("Could not open file at: $filePath")

    try {
      memScoped {
        val bufferLength = 64 * 1024
        val buffer = allocArray<ByteVar>(bufferLength)

        var line = fgets(buffer, bufferLength, filePtr)?.toKString()
        while (line != null) {
          builder.append(line)
          line = fgets(buffer, bufferLength, filePtr)?.toKString()
        }
      }
    } finally {
      fclose(filePtr)
    }

    return builder.toString()
  }

  override fun getFile(path: String): Path? = memScoped {
    val statBuf = alloc<stat>()
    if (lstat(path, statBuf.ptr) != 0) {
      null
    } else {
      parse(path)
    }
  }

  override fun listFiles(dir: Directory): List<Path> {
    val dirPath = dir.path
    val dirPtr = opendir(dirPath) ?: error("Failed to open directory: $dirPath")
    val result = mutableListOf<Path>()

    try {
      while (true) {
        val entry = readdir(dirPtr) ?: break
        val name = entry.pointed.d_name.toKString()
        if (name == "." || name == "..") continue

        val fullPath = if (dirPath.endsWith("/")) "$dirPath$name" else "$dirPath/$name"
        result.add(parse(fullPath))
      }
    } finally {
      closedir(dirPtr)
    }

    return result
  }

  override fun traverseDirectory(dir: Directory): List<Path> {
    val result = mutableListOf<Path>()
    val children = listFiles(dir)
    for (child in children) {
      result.add(child)
      if (child is Directory) {
        result.addAll(traverseDirectory(child))
      }
    }
    return result
  }

  override fun parse(filePath: String): Path = memScoped {
    val statBuf = alloc<stat>()
    if (lstat(filePath, statBuf.ptr) != 0) {
      return@memScoped File(filePath)
    }

    val mode = statBuf.st_mode.toInt() and S_IFMT
    when {
      mode == S_IFDIR -> Directory(filePath)
      mode == S_IFREG -> File(filePath)
      mode == S_IFLNK -> {
        val buffer = allocArray<ByteVar>(4096)
        val len = readlink(filePath, buffer, 4096.convert())
        val target = if (len > 0) buffer.toKString() else ""
        Link(filePath, target = File(target))
      }
      else -> File(filePath)
    }
  }

  override fun mkdirRecursive(dir: Directory): Directory {
    val path = dir.path
    if (path.isEmpty()) return dir

    // Try to create the directory directly.
    if (mkdir(path, 0x1FFu) == 0) return dir

    when (errno) {
      EEXIST -> {
        // Path already exists – ensure it is a directory.
        val isDirectory = memScoped {
          val statBuf = alloc<stat>()
          stat(path, statBuf.ptr) == 0 && (statBuf.st_mode.toInt() and S_IFMT) == S_IFDIR
        }
        if (isDirectory) return dir
        error("Path exists but is not a directory: $path")
      }
      ENOENT -> {
        // Parent does not exist – create it recursively, then retry.
        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isNotEmpty()) {
          mkdirRecursive(Directory(parentPath))
          if (mkdir(path, 0x1FFu) == 0) return dir
        }
        error("Failed to create directory: $path (errno=$errno)")
      }
      else -> error("Failed to create directory: $path (errno=$errno)")
    }
  }

  override fun sudoMkdirRecursive(dir: Directory): Directory {
    val result = executeProcess("sudo", listOf("mkdir", "-p", dir.path))
    check(result.code == 0) {
      "sudo mkdir -p failed for ${dir.path} with exit code ${result.code}: ${result.output.trim()}"
    }
    return dir
  }

  override fun sudoCopyFile(from: File, to: File) {
    val result = executeProcess("sudo", listOf("cp", from.path, to.path))
    check(result.code == 0) {
      "sudo cp failed from ${from.path} to ${to.path} with exit code ${result.code}: ${result.output.trim()}"
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  override fun filesAreIdentical(a: File, b: File): Boolean = memScoped {
    val sa = alloc<stat>()
    val sb = alloc<stat>()
    if (lstat(a.path, sa.ptr) != 0 || lstat(b.path, sb.ptr) != 0) return false

    if (sa.st_size != sb.st_size) return false
    if (sa.st_size == 0L) return true

    val fa = fopen(a.path, "rb") ?: return false
    val fb = fopen(b.path, "rb") ?: return false
    try {
      val bufA = ByteArray(64 * 1024)
      val bufB = ByteArray(64 * 1024)

      var nA = readChunk(fa, bufA)
      var nB = readChunk(fb, bufB)

      // At least one stream still has bytes → keep going.
      while (nA > 0 || nB > 0) {
        // Either one hit EOF before the other, or a read failed.
        if (nA != nB) return false

        // Same length chunk on both sides → compare contents.
        // Safe: nA == nB, and at least one is > 0 (loop condition),
        // so both are > 0 here.
        if (memcmp(bufA.refTo(0), bufB.refTo(0), nA.convert()) != 0) return false

        nA = readChunk(fa, bufA)
        nB = readChunk(fb, bufB)
      }

      // Both streams hit EOF on the same iteration.
      return true
    } finally {
      fclose(fa)
      fclose(fb)
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun readChunk(f: CPointer<FILE>, buf: ByteArray): Int =
      buf.usePinned { fread(it.addressOf(0), 1u, buf.size.convert(), f) }.toInt()

  override fun getAbsolutePath(path: String): String {
    // Already absolute – nothing to do.
    if (path.startsWith('/')) return path

    return memScoped {
      val bufferSize = 4096 // POSIX PATH_MAX on Linux
      val buffer = allocArray<ByteVar>(bufferSize)

      val cwd =
          getcwd(buffer, bufferSize.convert())?.toKString()
              ?: error("Failed to resolve current working directory (errno=$errno)")

      when {
        path.isEmpty() || path == "." -> cwd
        cwd == "/" -> "/$path"
        else -> "$cwd/$path"
      }
    }
  }

  override fun createLink(link: Link) {
    val result = symlink(link.target.path, link.path)
    if (result != 0) {
      error("Failed to create symlink '${link.path}' -> '${link.target.path}' " + "(errno=$errno)")
    }
  }

  override fun deleteFile(path: Path): Boolean =
      when (path) {
        is Directory -> {
          // Recursively delete all children first
          val children = listFiles(path)
          for (child in children) {
            val result = deleteFile(child)
            if (!result) return false
          }
          // Now the directory should be empty – remove it
          rmdir(path.path) == 0
        }
        is File,
        is Link -> remove(path.path) == 0
      }

  override fun getEnv(env: String): String? = getenv(env)?.toKString()

  override fun executeProcess(
      command: String,
      args: List<String>,
  ): ProcessResult {
    val cmdLine = buildShellCommand(command, args)
    println("[exec] $cmdLine")

    val stream = popen("$cmdLine 2>&1", "r") ?: error("Failed to execute process: $cmdLine")

    val output = StringBuilder()
    try {
      memScoped {
        val bufferLength = 64 * 1024
        val buffer = allocArray<ByteVar>(bufferLength)

        while (true) {
          val line = fgets(buffer, bufferLength, stream)?.toKString() ?: break
          output.append(line)
        }
      }
    } catch (t: Throwable) {
      // Make sure we reap the child process even if reading failed.
      pclose(stream)
      println("[exec] $cmdLine -> aborted: ${t.message}")
      throw t
    }

    val status = pclose(stream)
    val exitCode = if (status == -1) -1 else (status shr 8) and 0xFF

    println("[exec] $cmdLine -> exit=$exitCode")
    if (exitCode != 0) {
      val trimmed = output.toString().trim()
      if (trimmed.isNotEmpty()) {
        println("[exec] stderr/stdout:")
        trimmed.lineSequence().forEach { println("[exec]   $it") }
      }
    }

    return ProcessResult(output = output.toString(), code = exitCode)
  }

  override fun executeProcessInteractive(command: String, args: List<String>): Int {
    val cmdLine = buildShellCommand(command, args)
    println("[exec] $cmdLine (interactive)")

    val status = system(cmdLine)
    val exitCode = if (status == -1) -1 else (status shr 8) and 0xFF

    println("[exec] $cmdLine -> exit=$exitCode")
    return exitCode
  }

  /**
   * Builds a single command line suitable for `/bin/sh -c`, shell-quoting every token so
   * whitespace, quotes, `$`, etc. inside arguments are passed through literally.
   */
  private fun buildShellCommand(command: String, args: List<String>): String {
    val quotedArgs = args.joinToString(" ") { shellQuote(it) }
    return "$command $quotedArgs"
  }

  private fun shellQuote(arg: String): String {
    if (arg.isEmpty()) return "''"

    // Fast path: characters that are safe unquoted in POSIX sh.
    val safe = arg.all { it.isLetterOrDigit() || it in "._-/:=@%+," }
    if (safe) return arg

    // Wrap in single quotes, escaping embedded single quotes as '\''.
    return "'" + arg.replace("'", "'\\''") + "'"
  }
}
