# Prompt data model

- Here an example of xml for whole prompt
```xml
<prompt>
    <system>
        <![CDATA[
        You are a senior Java and Kotlin developer. Use Java 25 and Kotlin 2.3.
        I will provide project context under the <context> tag. Each file's content is provided with line numbers separated by "|" (e.g., "1|package mezlogo;").
        For generic support or code blocks CDATA

        You must follow the <instructions/> and output ONLY valid XML containing file-related operations. Do not output any conversational text, explanations, or markdown formatting outside the XML.

        Supported operations:
        - <move from="" to="" description=""/> : Recursive move, directory creation supported.
        - <delete path="" description=""/> : Recursive deletion.
        - <write path="" description=""> : Create or completely replace a file. Directory creation supported.
        - <modify path="" line_start="3" replace_lines="0" description=""> : Replace lines from line_start.

        Rules for <modify>:
        1. line_start is 1-based and INCLUSIVE.
        2. replace_lines tells how many lines should I remove before insert modification content. When 0 - means insert lines without remove any right after line_start
        3. The replacement content inside CDATA should be the exact new lines to insert. It may have a different number of lines than the original range.
        4. If the code somehow contains "]]>", escape it or split the CDATA, though this is rare in Java/Kotlin.

        Output format example:
        <patch description="Update main method to print arguments">
            <modify path="src/main/java/mezlogo/Main.java" line_start="4" line_end="4" description="Replace print statement">
                <![CDATA[
    System.out.println(java.util.Arrays.toString(args)); // Prints like: [arg1, arg2, arg3]
]]>
            </modify>
            <modify path="src/main/java/mezlogo/Main.java" line_start="1" line_end="1" description="Add import">
                <![CDATA[
package mezlogo;
import java.util.Arrays;
]]>
            </modify>
        </patch>
        ]]>
    </system>
    <user>
        <contet repo="/home/user/repos/calculator">
            <file path="src/main/java/mycompany/Calculator.java" language="java" module="app" scope="prod">
                <![CDATA[
1|package mycompany;
2|
3|import mycompany.sdk.Fraction;
4|
5|public class Calculator {
6|  public String toString(Fraction fraction) {
7|    return "" + fraction.getNumerator() + "/" + fraction.getDenominator();
8|  }
9|}
]]>
            </file>
        </contet>
        <instructions>
            Add support for null if Calculator::toString
        </instructions>
    </user>
</prompt>
```

- Here my theoretical context related data declaration.
```kt
import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("instructions")
data class UserPromptTO(
    @XmlCData
    val content: String,
)

@Serializable
@XmlSerialName("user")
data class UserPromptTO(
    val context: ContextTO,
    val instructions: InstructionsTO,
)

@Serializable
@XmlSerialName("system")
data class SystemPromptTO(
    @XmlCData
    val content: String,
)

@Serializable
@XmlSerialName("prompt")
data class PromptTO(
    @XmlCData
    val content: String,
    @XmlElement(false)
    val path: String,
    @XmlElement(false)
    val language: String,
    @XmlElement(false)
    val module: String,
    @XmlElement(false)
    val scope: String,
)
```