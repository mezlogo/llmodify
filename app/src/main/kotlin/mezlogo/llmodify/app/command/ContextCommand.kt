package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand

class ContextCommand: CliktCommand(name = "context") {
  // Add options for includeGlob, excludeGlobs, root with defaults
  
  override fun run() {
    echo("Hello")
  }

  fun buildContext(): ContextCommand {
    TODO("IMPLEMENT")
  }
}
