package app.template.patches.ntiles

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val QUICK_SETTINGS = Compatibility(
    name = "Quick Settings",
    packageName = "it.simonesestito.ntiles",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x2196F3,
    targets = listOf(AppTarget(version = "12.0.3-17888438")),
)

/**
 * LicenseClient.initializeLicenseCheck().
 * PairIP entry point. Early return skips the local check so the app can keep running.
 */
private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
)

/**
 * LicenseClient.connectToLicensingService() (private).
 * Fixed log string. Extra path coverage.
 */
private object ConnectToLicensingServiceFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("Connecting to the licensing service..."),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local PairIP license exit check so the app can keep running.",
    default = true,
) {
    compatibleWith(QUICK_SETTINGS)

    execute {
        InitializeLicenseCheckFingerprint.method.returnEarly()
        ConnectToLicensingServiceFingerprint.method.returnEarly()
    }
}
