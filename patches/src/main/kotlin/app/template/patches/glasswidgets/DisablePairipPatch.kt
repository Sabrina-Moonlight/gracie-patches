package app.template.patches.glasswidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

private val GLASS_WIDGETS = Compatibility(
    name = "Glass Widgets",
    packageName = "com.jndapp.glass.widgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x00BCD4,
    targets = listOf(AppTarget(version = "1.0.2")),
)

/** ContentProvider runs before Application and also starts the license check. */
private object LicenseContentProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseContentProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = listOf(),
)

/** Entry from Application.attachBaseContext and ContentProvider. */
private object CheckLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)

/** Real license work after checkLicense posts to the main thread. */
private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = listOf(),
)

/** Opens Play Store / paywall when the check fails. */
private object StartPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startPaywallActivity",
    returnType = "V",
    parameters = listOf("Landroid/app/PendingIntent;"),
)

/** Error dialog path that also closes the app. */
private object StartErrorDialogFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startErrorDialogActivity",
    returnType = "V",
    parameters = listOf(),
)

/** Activity that receives the paywall intent and sends the user to Play Store. */
private object LicenseActivityOnStartFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "onStart",
    returnType = "V",
    parameters = listOf(),
)

/** Sends the PendingIntent that opens Play Store. */
private object ShowPaywallAndCloseAppFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "showPaywallAndCloseApp",
    returnType = "V",
    parameters = listOf(),
)

/**
 * Native VM bootstrap from CoreComponentFactory.
 * Only class + name + isStatic: the program name string is a field, not a const in this method.
 */
private object StartupLauncherFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    isStatic = true,
    returnType = "V",
    parameters = listOf(),
)

/** Signature check throws after the APK is re-signed by the patcher. */
private object VerifyIntegrityFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
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
        LicenseContentProviderOnCreateFingerprint.method.returnEarly(true)
        StartupLauncherFingerprint.method.returnEarly()
        VerifyIntegrityFingerprint.method.returnEarly()

        CheckLicenseFingerprint.method.returnEarly()
        InitializeLicenseCheckFingerprint.method.returnEarly()
        StartPaywallFingerprint.method.returnEarly()
        StartErrorDialogFingerprint.method.returnEarly()

        LicenseActivityOnStartFingerprint.method.returnEarly()
        ShowPaywallAndCloseAppFingerprint.method.returnEarly()
    }
}
