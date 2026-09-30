package mezlogo.llmodify.app.command

import com.github.ajalt.clikt.core.CliktCommand

class ContextCommand : CliktCommand(name = "context") {
  override fun run() {
    echo("Hello")
  }
}
