package app.template.patches.pixelwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val PIXEL_WIDGETS = Compatibility(
    name = "Pixel Widgets",
    packageName = "com.widgetvibes.pixelwidgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    targets = listOf(
        AppTarget(version = "1.0.16", versionCode = 17, minSdk = 32),
    ),
)

/**
 * Only skip the real entry. Other creators (hxreborn / morphe shared) do the same:
 * returnEarly on initializeLicenseCheck. That prevents the error dialog and the close.
 */
private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local license check so the app can keep running offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        runCatching {
            InitializeLicenseCheckFingerprint.method.returnEarly()
        }
    }
}
