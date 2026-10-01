package mezlogo.llmodify.modify.adapter.patch.impl

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*
import mezlogo.llmodify.modify.adapter.patch.PatchUseCase
import mezlogo.llmodify.modify.model.*

class PatchService : PatchUseCase {
  override fun patch(
      patchContext: PatchContext,
      patchTo: PatchTO,
  ) {
    val root = patchContext.contextRoot.toAbsolutePath().normalize()

    patchTo.write.forEach { write(root, it) }
    patchTo.modify.forEach { modify(root, it) }
    patchTo.move.forEach { move(root, it) }
    patchTo.delete.forEach { delete(root, it) }
  }

  private fun resolve(root: Path, path: String): Path {
    val resolved = Path.of(path)
    return if (resolved.isAbsolute) resolved.normalize() else root.resolve(resolved).normalize()
  }

  private fun write(root: Path, writeTO: WriteTO) {
    val target = resolve(root, writeTO.path)
    target.parent?.createDirectories()
    target.writeText(writeTO.content, Charsets.UTF_8)
  }

  private fun modify(root: Path, modifyTO: ModifyTO) {
    val target = resolve(root, modifyTO.path)
    val content = target.readText(Charsets.UTF_8)
    val lines = fileLines(content)
    val lineStart = modifyTO.lineStart.toInt()
    val replaceLines = modifyTO.replaceLines.toInt()
    require(lineStart >= 1) { "line_start must be >= 1: ${modifyTO.lineStart}" }
    require(replaceLines >= 0) { "replace_lines must be >= 0: ${modifyTO.replaceLines}" }

    val insertionIndex = (lineStart - 1).coerceIn(0, lines.size)
    val removeEnd = (insertionIndex + replaceLines).coerceAtMost(lines.size)
    if (removeEnd > insertionIndex) {
      lines.subList(insertionIndex, removeEnd).clear()
    }
    lines.addAll(insertionIndex, insertedLines(modifyTO.content))
    target.writeText(lines.joinToString("\n"), Charsets.UTF_8)
  }

  private fun move(root: Path, moveTO: MoveTO) {
    val from = resolve(root, moveTO.from)
    val to = resolve(root, moveTO.to)
    to.parent?.createDirectories()
    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
  }

  private fun delete(root: Path, deleteTO: DeleteTO) {
    val target = resolve(root, deleteTO.path)
    if (!target.exists()) return
    if (target.isDirectory()) {
      Files.walk(target).use { stream ->
        stream.sorted(Comparator.comparingInt<Path> { it.nameCount }.reversed()).forEach {
          it.deleteIfExists()
        }
      }
    } else {
      target.deleteIfExists()
    }
  }

  private fun fileLines(content: String): MutableList<String> {
    return if (content.isEmpty()) mutableListOf() else content.lines().toMutableList()
  }

  private fun insertedLines(content: String): List<String> {
    if (content.isEmpty()) return emptyList()
    val lines = content.lines().toMutableList()
    if (lines.isNotEmpty() && lines.last().isEmpty()) {
      lines.removeAt(lines.lastIndex)
    }
    return lines
  }
}
