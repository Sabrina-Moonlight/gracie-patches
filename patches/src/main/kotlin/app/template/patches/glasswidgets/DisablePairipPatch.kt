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

// ── Pairip key-import gate ────────────────────────────────────────────────────

/**
 * KeyImportAppComponentFactory is the actual appComponentFactory in the manifest.
 * When isEncryptionKeyPresent() = false it routes ALL components to placeholder
 * stubs and boots KeyImportService to fetch the decryption key from the server.
 * Returning true skips that phase entirely and routes to the real app components.
 */
private object KeyImportIsEncryptionKeyPresentFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/vmencryption/KeyImportAppComponentFactory;",
    name = "isEncryptionKeyPresent",
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("Failed to check if encryption key is present."),
)

// ── VMRunner native lib ───────────────────────────────────────────────────────

/**
 * VMRunner.<clinit> calls System.loadLibrary("pairipcore") — the native lib
 * whose JNI_OnLoad verifies APK signature and aborts if it doesn't match.
 * This fires the moment any code first references VMRunner (e.g. the
 * setContext call at the top of com.pairip.application.Application
 * .attachBaseContext, before any other patch can intercept).
 * Returning early prevents the .so from ever loading.
 */
private object VMRunnerStaticInitFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "<clinit>",
    returnType = "V",
    parameters = listOf(),
    strings = listOf("pairipcore"),
)

// ── Pairip license SDK ────────────────────────────────────────────────────────

private object LicenseContentProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseContentProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = listOf(),
)

private object CheckLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)

private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = listOf(),
)

private object StartPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startPaywallActivity",
    returnType = "V",
    parameters = listOf("Landroid/app/PendingIntent;"),
)

private object StartErrorDialogFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startErrorDialogActivity",
    returnType = "V",
    parameters = listOf(),
)

private object LicenseActivityOnStartFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "onStart",
    returnType = "V",
    parameters = listOf(),
)

private object ShowPaywallAndCloseAppFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "showPaywallAndCloseApp",
    returnType = "V",
    parameters = listOf(),
)

private object StartupLauncherFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    isStatic = true,
    returnType = "V",
    parameters = listOf(),
)

private object VerifyIntegrityFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Apk signature is invalid."),
)

private object BindToLicensingServiceFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "bindToLicensingService",
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("Connecting to the main licensing service..."),
)

// ── VMRunner invoke path ──────────────────────────────────────────────────────

private object VmRunnerInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "invoke",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/Object;"),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local Pairip license check so the app can keep running.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS)

    execute {
        // Skip key-import phase — factory always routes to real components.
        KeyImportIsEncryptionKeyPresentFingerprint.method.returnEarly(true)

        // Block native lib load — must come before setContext triggers classloading.
        VMRunnerStaticInitFingerprint.method.returnEarly()

        // Nop remaining Pairip entry and exit points.
        LicenseContentProviderOnCreateFingerprint.method.returnEarly(true)
        StartupLauncherFingerprint.method.returnEarly()
        VerifyIntegrityFingerprint.method.returnEarly()

        CheckLicenseFingerprint.method.returnEarly()
        InitializeLicenseCheckFingerprint.method.returnEarly()
        BindToLicensingServiceFingerprint.method.returnEarly()
        StartPaywallFingerprint.method.returnEarly()
        StartErrorDialogFingerprint.method.returnEarly()

        LicenseActivityOnStartFingerprint.method.returnEarly()
        ShowPaywallAndCloseAppFingerprint.method.returnEarly()

        VmRunnerInvokeFingerprint.method.returnEarly(null)
    }
}
