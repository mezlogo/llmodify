package mezlogo.llmodify.core.models

/** Represents a hierarchy for building a tree of files and dirs RELATIVE to root of configs. */
sealed interface RelativeConfig

data class RelativeConfigFile(
    val absoluteFile: File,
    val relativeFile: File,
    val parent: Directory?,
) : RelativeConfig

data class RelativeConfigDirectory(
    val absoluteDirectory: Directory,
    val relativeDirectory: Directory,
    val children: List<RelativeConfig>,
    val parent: Directory?,
) : RelativeConfig

/** Only in real file this type of relative file could exist */
data class RelativeConfigLink(
    val absoluteLink: Link,
    val relativeLink: File,
    val parent: Directory?,
) : RelativeConfig
