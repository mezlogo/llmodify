# Interactive support for pacman / yay

Right now `PacmanService` always invokes `pacman`/`yay` with `--noconfirm` and captures the whole output through `popen` (stdout only, stdin not connected). To make it interactive we need three pieces:

1. A new port method that spawns a child with **inherited** stdio (so `pacman`/`yay` can prompt and paint their progress bars on the real terminal).
2. `PacmanUseCase` / `SyncUseCase` need to carry an `interactive` flag through.
3. `SyncCommand` needs a `--interactive` CLI flag.

Below are the concrete diffs.

---

## 1. `PlatformPort` — add `executeProcessInteractive`

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.port/platform/PlatformPort.kt
interface PlatformPort {
  // ... existing methods ...

  fun executeProcess(command: String, args: List<String>): ProcessResult

  /**
   * Runs [command] [args] on the controlling terminal:
   *  - stdin/stdout/stderr are inherited from the parent process,
   *  - the child can prompt the user and render progress bars,
   *  - returns the child exit code.
   *
   * Unlike [executeProcess] the output is **not** captured; it goes straight to the tty.
   */
  fun executeProcessInteractive(command: String, args: List<String>): Int
}
```

## 2. `PosixPlatformAdapter` — implement it via `system(3)`

`system()` inherits the parent's fd 0/1/2 and goes through `/bin/sh -c`, which we already know how to quote safely via `buildShellCommand`.

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.port/platform/impl/PosixPlatformAdapter.kt
// add to imports:
import platform.posix.system

// add method to the class:
override fun executeProcessInteractive(command: String, args: List<String>): Int {
  val cmdLine = buildShellCommand(command, args)
  println("[exec] $cmdLine (interactive)")

  val status = system(cmdLine)
  val exitCode = if (status == -1) -1 else (status shr 8) and 0xFF

  println("[exec] $cmdLine -> exit=$exitCode")
  return exitCode
}
```

`system()` returns the raw `waitpid` status; the `(status shr 8) and 0xFF` decode matches what the existing `executeProcess` already does with `pclose`.

## 3. `PacmanUseCase` — interactive flag

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.core/pacman/PacmanUseCase.kt
interface PacmanUseCase {
  fun listExplicitlyInstalledPackages(): Set<String>
  fun listExplicitlyForeignInstalledPackages(): Set<String>

  /** Install packages. When [interactive] is true, omit --noconfirm and inherit tty. */
  fun installPackages(packages: Set<String>, interactive: Boolean = false)

  /** Install AUR packages. When [interactive] is true, omit --noconfirm and inherit tty. */
  fun installAurPackages(packages: Set<String>, interactive: Boolean = false)
}
```

## 4. `PacmanService` — branch on `interactive`

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.core/pacman/impl/PacmanService.kt
override fun installPackages(packages: Set<String>, interactive: Boolean) {
  if (packages.isEmpty()) return

  val args = buildList {
    add("-S")
    add("--needed")
    if (!interactive) add("--noconfirm")
    addAll(packages.sorted())
  }

  if (interactive) {
    val code = platformPort.executeProcessInteractive("sudo", listOf("pacman") + args)
    check(code == 0) { "sudo pacman -S failed with exit code $code" }
  } else {
    val result = platformPort.executeProcess("sudo", listOf("pacman") + args)
    check(result.code == 0) {
      "sudo pacman -S --needed --noconfirm failed with exit code ${result.code}: ${result.output.trim()}"
    }
  }
}

override fun installAurPackages(packages: Set<String>, interactive: Boolean) {
  if (packages.isEmpty()) return

  // `yay` must NOT be invoked through `sudo`: AUR helpers refuse to run as root and will call
  // `sudo` themselves for the underlying pacman operations.
  val args = buildList {
    add("-S")
    add("--needed")
    if (!interactive) add("--noconfirm")
    addAll(packages.sorted())
  }

  if (interactive) {
    val code = platformPort.executeProcessInteractive("yay", args)
    check(code == 0) { "yay -S failed with exit code $code" }
  } else {
    val result = platformPort.executeProcess("yay", args)
    check(result.code == 0) {
      "yay -S --needed --noconfirm failed with exit code ${result.code}: ${result.output.trim()}"
    }
  }
}
```

## 5. `SyncUseCase` — carry the flag

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.core/sync/SyncUseCase.kt
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
```

## 6. `SyncService` — forward to pacman calls

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.core/sync/impl/SyncService.kt
override fun sync(
    config: RepoConfig,
    diffResult: ConfigurationDiffResult,
    featureFlags: FeatureFlags,
    cleanup: Boolean,
    overwrite: Boolean,
    interactive: Boolean,
) {
  println("[sync] start host=${config.host} ...")
  println("[sync] options cleanup=$cleanup overwrite=$overwrite interactive=$interactive")
  // ... pre-flight unchanged ...

  if (featureFlags.pacmanEnabled) syncPacman(diffResult, interactive)
  if (featureFlags.aurEnabled)    syncAur(diffResult, interactive)
  // ... rest unchanged ...
}

private fun syncPacman(diffResult: ConfigurationDiffResult, interactive: Boolean) {
  val toInstall = diffResult.pacmanDiff.declaredNotInstalledPackages.toSet()
  if (toInstall.isEmpty()) { println("[sync] pacman: nothing to install"); return }

  println("[sync] pacman: installing ${toInstall.size} package(s): " +
      toInstall.sorted().joinToString(", "))
  pacmanUseCase.installPackages(toInstall, interactive)
  println("[sync] pacman: install finished")
}

private fun syncAur(diffResult: ConfigurationDiffResult, interactive: Boolean) {
  val toInstall = diffResult.aurDiff.declaredNotInstalledPackages.toSet()
  if (toInstall.isEmpty()) { println("[sync] aur: nothing to install"); return }

  println("[sync] aur: installing ${toInstall.size} package(s): " +
      toInstall.sorted().joinToString(", "))
  pacmanUseCase.installAurPackages(toInstall, interactive)
  println("[sync] aur: install finished")
}
```

## 7. `SyncCommand` — expose `--interactive`

```kotlin
// app/src/commonMain/kotlin/mezlogo.llmodify.app/command/SyncCommand.kt
val cleanup: Boolean by
    option(help = "Remove installed packages that are not declared. IT IS DANGEROUS!")
        .flag(default = false)

/** When set, pacman/yay run on the real tty: no --noconfirm, prompts and output pass through. */
val interactive: Boolean by
    option(help = "Run pacman/yay interactively (no --noconfirm, inherit tty).")
        .flag(default = false)

override fun runRepoCallback(config: RepoConfig, featureFlags: FeatureFlags) {
  val throwOnConflict = !merge
  val merged = repoUseCase.getMergedByHostRepresentation(config, throwOnConflict, featureFlags)
  val diffResult = diffCalculatorUseCase.calculateDiff(config, merged, featureFlags)

  syncUseCase.sync(
      config = config,
      diffResult = diffResult,
      featureFlags = featureFlags,
      cleanup = cleanup,
      overwrite = overwrite,
      interactive = interactive,
  )

  echo("Sync completed for host ${config.host}.")
}
```

---

## Behaviour summary

| `--interactive` | pacman command | stdout | stdin |
| --- | --- | --- | --- |
| off (default) | `sudo pacman -S --needed --noconfirm <pkgs>` | captured via `popen`, printed at the end | not connected |
| on | `sudo pacman -S --needed <pkgs>` | inherited tty, streamed live | inherited tty (prompts work) |

Same for `yay` (AUR), just without `sudo`.

