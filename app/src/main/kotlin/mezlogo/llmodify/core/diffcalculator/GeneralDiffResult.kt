package mezlogo.llmodify.core.diffcalculator

/**
 * Represent generalized result of diff calculation for more common handling. Each concrete type of
 * basic unit (pacman, service, user service, file, dir) has its own specifics which could be
 * generailize to:
 */
enum class GeneralDiffResult {

  /** Everything is ok, no need to do anything. */
  ALREADY_SYNCED,

  /** Not synced yet, has no problem to sync */
  COULD_BE_SYNCED,

  /**
   * Not synced yet, target has a problem could be fixed by overwriting. This means operation could
   * be dangerous - automatic remove files before writing needed could lead to terrible outcome.
   */
  COULD_BE_OVERWRITTEN,

  /** Not synced and could not be synced without user manual environment fixing. */
  ERROR_NEED_USER_INTERACTION,
}
