package mezlogo.llmodify.prompt.model

import java.nio.file.Path

/**
 * This class represents a configuration for building a context based on given files.
 */
data class GivenFilesParameters(

    /**
     * Path to traverse all files from this path. It's a root for all files and make it possible to build relative path for each file.
     */
    val contextRoot: Path,

    /**
     * Pathes for files to include in context. Could be relative to contextRoot or absolute.
     */
    val givenFiles: List<Path>,
)
