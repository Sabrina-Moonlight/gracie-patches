package app.template.patches.puzzleskids

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Optional PairIP / signature integrity (Doom-style).
 * All fingerprints are best-effort: if the class is missing, the gate is skipped.
 */
private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
)

private object ConnectToLicensingServiceFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("Connecting to the licensing service..."),
)

private object VerifyIntegrityFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Apk signature is invalid."),
)

private object StartupLauncherFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    isStatic = true,
    returnType = "V",
    parameters = listOf(),
)

private object ScheduleAppShutdownFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "scheduleAppShutdown",
    returnType = "V",
    parameters = listOf(),
)

internal val disablePairipPatch = bytecodePatch {
    compatibleWith(PUZZLES_KIDS)

    execute {
        runCatching { InitializeLicenseCheckFingerprint.method.returnEarly() }
        runCatching { ConnectToLicensingServiceFingerprint.method.returnEarly() }
        runCatching { VerifyIntegrityFingerprint.method.returnEarly() }
        runCatching { StartupLauncherFingerprint.method.returnEarly() }
        runCatching { ScheduleAppShutdownFingerprint.method.returnEarly() }
    }
}
