package mezlogo.llmodify.prompt.adapter.prompt.impl

import mezlogo.llmodify.prompt.adapter.prompt.BuildPromptUseCase
import mezlogo.llmodify.prompt.adapter.xmlmodel.*
import mezlogo.llmodify.prompt.model.PromptOverrideParameters

/** Builds a prompt from default classpath resources, unless the caller provides overrides. */
class BuildPromptService : BuildPromptUseCase {

  private val defaultSystemPrompt: String by lazy {
    readClasspathResource("/system_prompt.txt")
  }

  private val defaultInstructions: String by lazy {
    readClasspathResource("/instruction_prompt.txt")
  }

  override fun buildPrompt(
      promptOverrideParameters: PromptOverrideParameters,
      context: ContextTO,
  ): PromptTO {
    val systemPrompt = promptOverrideParameters.systemPrompt ?: defaultSystemPrompt
    val instructions = promptOverrideParameters.instructions ?: defaultInstructions

    return PromptTO(
        system =
            SystemPromptTO(
                content = systemPrompt,
            ),
        user =
            UserPromptTO(
                context = context,
                instructions =
                    InstructionsTO(
                        content = instructions,
                    ),
            ),
    )
  }

  private fun readClasspathResource(path: String): String {
    val stream = javaClass.getResourceAsStream(path) ?: error("Classpath resource not found: $path")

    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
  }
}
