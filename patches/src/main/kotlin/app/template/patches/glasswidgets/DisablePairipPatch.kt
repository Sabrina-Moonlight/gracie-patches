package app.template.patches.glasswidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.template.patches.shared.misc.pairip.removePairipKeyImportPatch
import app.template.patches.shared.misc.pairip.removePairipProtectionPatch
import app.template.patches.shared.misc.pairip.removePairipVirtualizationPatch

private val GLASS_WIDGETS = Compatibility(
    name = "Glass Widgets",
    packageName = "com.jndapp.glass.widgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x00BCD4,
    targets = listOf(AppTarget(version = "1.0.2", versionCode = 12)),
)

/** ContentProvider also starts the license check very early. */
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

/**
 * Glass Widgets entry for Pairip: restores the real app entry, skips the local
 * license path, removes the native runtime, and neutralises virtualized stubs.
 */
@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Restores the normal app entry, skips local checks, and neutralises the offline runtime so the app can keep running.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS)

    dependsOn(
        removePairipKeyImportPatch,
        removePairipProtectionPatch,
        removePairipVirtualizationPatch,
    )

    execute {
        runCatching { LicenseContentProviderOnCreateFingerprint.method.returnEarly(true) }
        runCatching { StartupLauncherFingerprint.method.returnEarly() }
        runCatching { VerifyIntegrityFingerprint.method.returnEarly() }
        runCatching { CheckLicenseFingerprint.method.returnEarly() }
        runCatching { StartPaywallFingerprint.method.returnEarly() }
        runCatching { StartErrorDialogFingerprint.method.returnEarly() }
        runCatching { LicenseActivityOnStartFingerprint.method.returnEarly() }
        runCatching { ShowPaywallAndCloseAppFingerprint.method.returnEarly() }
    }
}
