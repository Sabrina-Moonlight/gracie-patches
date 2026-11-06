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
 * Native VM bootstrap from CoreComponentFactory and ApplicationClass static init.
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

/**
 * KeyStoreManager reports whether the local encryption key exists.
 * Returning true skips the KeyImport flow that opens the store.
 */
private object IsEncryptionKeyPresentFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/vmencryption/KeyStoreManager;",
    name = "isEncryptionKeyPresent",
    returnType = "Z",
    parameters = listOf("I"),
)

/**
 * VM entry used for protected methods. Without a real key, decrypt crashes the process.
 * Returning null avoids the decrypt path so the app can stay open.
 */
private object VmRunnerInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "invoke",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/Object;"),
)

/** Extra license bind path (same idea as other apps in this repo). */
private object BindToLicensingServiceFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "bindToLicensingService",
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
    compatibleWith(GLASS_WIDGETS)

    execute {
        LicenseContentProviderOnCreateFingerprint.method.returnEarly(true)
        StartupLauncherFingerprint.method.returnEarly()
        VerifyIntegrityFingerprint.method.returnEarly()

        CheckLicenseFingerprint.method.returnEarly()
        InitializeLicenseCheckFingerprint.method.returnEarly()
        StartPaywallFingerprint.method.returnEarly()
        StartErrorDialogFingerprint.method.returnEarly()
        BindToLicensingServiceFingerprint.method.returnEarly()

        LicenseActivityOnStartFingerprint.method.returnEarly()
        ShowPaywallAndCloseAppFingerprint.method.returnEarly()

        // Skip KeyImport → store path, and avoid decrypt crash without a real key.
        IsEncryptionKeyPresentFingerprint.method.returnEarly(true)
        VmRunnerInvokeFingerprint.method.returnEarly(null)
    }
}
