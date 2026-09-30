package mezlogo.llmodify.core.diffcalculator

import mezlogo.llmodify.core.models.FeatureFlags
import mezlogo.llmodify.core.repo.MergedByHostRepresentation
import mezlogo.llmodify.core.repo.RepoConfig

/**
 * This use case does the most valuable thing - calculates diff between declared and real
 * configurations.
 */
interface DiffCalculatorUseCase {
  fun calculateDiff(
      config: RepoConfig,
      mergedByHostRepresentation: MergedByHostRepresentation,
      featureFlags: FeatureFlags,
  ): ConfigurationDiffResult
}
