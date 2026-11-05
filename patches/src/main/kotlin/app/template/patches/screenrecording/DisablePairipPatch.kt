package app.template.patches.screenrecording

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val SCREEN_RECORDING_APP = Compatibility(
    name = "Screen Recording App",
    packageName = "de.twokit.screen.recording.app",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE53935,
    targets = listOf(AppTarget(version = "1.11")),
)

/**
 * LicenseClient.checkLicense(Context).
 * Called by Pairip at start (Application.attachBaseContext).
 * Fixed log string → stable fingerprint.
 */
private object CheckLicenseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)

/**
 * bindToLicensingService: fixed log string.
 * Extra path coverage.
 */
private object BindToLicensingServiceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("Connecting to the main licensing service..."),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local Pairip license check so the app can keep running.",
    default = true,
) {
    compatibleWith(SCREEN_RECORDING_APP)

    execute {
        CheckLicenseFingerprint.method.returnEarly()
        BindToLicensingServiceFingerprint.method.returnEarly()
    }
}
