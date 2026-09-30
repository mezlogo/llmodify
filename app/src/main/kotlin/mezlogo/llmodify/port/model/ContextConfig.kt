package mezlogo.llmodify.port.model

import java.nio.file.Path

/**
 * This class represents a configuration for searching and building a context of files.
 */
data class ContextConfig(

    /**
     * Path to traverse all files from this path. It's a root for all files and make it possible to build relative path for each file.
     */
    val contextRoot: Path,

    /**
     * This is a filename only glob rules. IF filename test at least one glob it's ok.
     * Simple file type globs: '*.kt', '*.y*ml', '*.java'
     * Simple file name types: '33-*' - all files starts with 33, useful for linux configuration files
     * When empty - include all files.
     */
    val includeGlobs: List<String>,

    /**
     * Filter included filenames by glob.
     * When empty - do not exclude anything.
     */
    val excludeGlobs: List<String>,
)
