package mezlogo.llmodify.core.sync

import mezlogo.llmodify.core.diffcalculator.ConfigurationDiffResult
import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.repo.RepoConfig

interface SyncUseCase {
  fun sync(
      config: RepoConfig,
      diffResult: ConfigurationDiffResult,
      featureFlags: FeatureFlags,
      cleanup: Boolean,
      overwrite: Boolean,
      interactive: Boolean,
  )
}
