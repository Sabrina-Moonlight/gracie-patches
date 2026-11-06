package app.template.patches.glasswidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val GLASS_WIDGETS = Compatibility(
    name = "Glass Widgets",
    packageName = "com.jndapp.glass.widgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x00BCD4,
    targets = listOf(AppTarget(version = "1.0.2")),
)

/**
 * LicenseClient.checkLicense(Context).
 * Called from Application.attachBaseContext.
 * Fixed log string → stable fingerprint.
 */
private object CheckLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)

/**
 * LicenseClient.initializeLicenseCheck().
 * Real work starts here after checkLicense posts to the main thread.
 */
private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
)

/**
 * LicenseClient.startPaywallActivity(PendingIntent).
 * Opens Play Store / paywall when the license check fails.
 */
private object StartPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startPaywallActivity",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Landroid/app/PendingIntent;"),
)

/**
 * SignatureCheck.verifyIntegrity(Context).
 * Throws if the APK signature no longer matches after patching.
 */
private object VerifyIntegrityFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Apk signature is invalid."),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local Pairip license check so the app can keep running.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS)

    execute {
        CheckLicenseFingerprint.method.returnEarly()
        InitializeLicenseCheckFingerprint.method.returnEarly()
        StartPaywallFingerprint.method.returnEarly()
        VerifyIntegrityFingerprint.method.returnEarly()
    }
}
