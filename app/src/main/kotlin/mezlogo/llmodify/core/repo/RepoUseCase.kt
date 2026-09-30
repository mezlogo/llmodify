package mezlogo.llmodify.core.repo

import mezlogo.llmodify.core.models.FeatureFlags

interface RepoUseCase {
  fun readRepository(config: RepoConfig): RepoRepresentation

  fun getMergedByHostRepresentation(
      config: RepoConfig,
      throwOnConflict: Boolean,
      featureFlags: FeatureFlags,
  ): MergedByHostRepresentation
}
