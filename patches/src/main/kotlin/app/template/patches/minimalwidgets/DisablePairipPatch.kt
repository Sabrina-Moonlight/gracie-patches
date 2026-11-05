package app.template.patches.minimalwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val MINIMAL_WIDGETS = Compatibility(
    name = "Minimal Widgets",
    packageName = "com.jndapp.minimal.widgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x607D8B,
    targets = listOf(AppTarget(version = "2.2.11")),
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
 * private bindToLicensingService(Z): fixed log string.
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
    compatibleWith(MINIMAL_WIDGETS)

    execute {
        CheckLicenseFingerprint.method.returnEarly()
        BindToLicensingServiceFingerprint.method.returnEarly()
    }
}
