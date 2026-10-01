# Patch result data model

- Here an example of xml for whole prompt
```xml

<patch>
    <modify path="src/main/java/mycompany/Calculator.java" line_start="6" replace_lines="0">
<![CDATA[    if (null == fraction) return "";]]>
    </modify>
    <move from="src/main/java/mycompany/Utils.java" to="src/main/java/mycompany/utils/Utils.java" />
    <move from="src/main/java/mycompany/Helpers.java" to="src/main/java/mycompany/utils/Helpers.java" />
    <delete path="src/main/java/mycompany/usunsed" />
    <write path="src/main/resources/wiremock/__files/greet.json">
<![CDATA[
{"name":"Bob"}
]]>        
    </write>
</patch>
```

- Here my theoretical this xml represents in kotlin data class declaration.
```kt
import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable

@Serializable
@XmlSerialName("patch")
data class PatchTO(
    @XmlElement(true)
    val modify: List<ModifyTO> = emptyList(),
    
    @XmlElement(true)
    val write: List<WriteTO> = emptyList(),

    @XmlElement(true)
    val move: List<MoveTO> = emptyList(),

    @XmlElement(true)
    val delete: List<DeleteTO> = emptyList(),
)

@Serializable
@XmlSerialName("move")
data class MoveTO(
    @XmlElement(false)
    val from: String,
    
    @XmlElement(false)
    val to: String,
)

@Serializable
@XmlSerialName("write")
data class WriteTO(
    @XmlCData
    val content: String,

    @XmlElement(false)
    val path: String,
)

@Serializable
@XmlSerialName("delete")
data class DeleteTO(
    @XmlElement(false)
    val path: String,
)

@Serializable
@XmlSerialName("modify")
data class ModifyTO(
    @XmlCData
    val content: String,
    
    @XmlElement(false)
    val path: String,
    
    @XmlElement(false)
    @XmlSerialName("line_start")
    val lineStart: String,
    
    @XmlElement(false)
    @XmlSerialName("replace_lines")
    val replaceLines: String,
)
```