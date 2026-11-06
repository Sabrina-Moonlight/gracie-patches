package app.template.patches.shared.misc.pairip

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
    targets = listOf(AppTarget(version = "1.0.2", versionCode = 12)),
)

/**
 * Skips the local Pairip license check entry so the app does not open a store paywall.
 */
val removePairipProtectionPatch = bytecodePatch(
    name = "Remove Pairip Protection",
    description = "Skips the local license check entry point.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS)

    execute {
        if (classDefByOrNull(LICENSE_CLIENT_CLASS) == null) return@execute
        InitializeLicenseCheckFingerprint.method.returnEarly()
    }
}
