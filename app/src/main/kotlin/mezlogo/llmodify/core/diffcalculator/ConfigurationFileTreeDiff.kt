package mezlogo.llmodify.core.diffcalculator

import mezlogo.llmodify.core.models.Directory
import mezlogo.llmodify.core.models.Link
import mezlogo.llmodify.core.models.Path

/** $HOME/.config related fields. */
data class ConfigurationFileTreeDiff(
    val filesToRemoveBefore: List<Path>,
    val directoriesToCreate: List<Directory>,
    val linksToCreate: List<Link>,
)
