package mezlogo.llmodify.core.configtree

import mezlogo.llmodify.core.models.RelativeConfigDirectory

interface ConfigTreeUseCase {

  /** make multiple relative config to one, when conflict overwrite. */
  fun merge(
      configs: List<RelativeConfigDirectory>,
      throwOnConflict: Boolean,
  ): RelativeConfigDirectory

  /** Compare desired config files with real using only relative structure */
  fun compareRelativeFileTree(
      desire: RelativeConfigDirectory,
      real: RelativeConfigDirectory,
  ): List<CompareFileResult>
}
