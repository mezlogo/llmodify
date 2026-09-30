# Context data model

- Here an example of xml. I expect this format is good for LLM consuming.
```xml
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
```

- Here my theoretical context related data declaration.
```kt
import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("Context")
data class ContextTO(
    @XmlElement(false)
    val repo: String,
    val files: List<FileTO>,
)

@Serializable
@XmlSerialName("file")
data class FileTO(
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