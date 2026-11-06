package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

internal val COLOR_WIDGETS = Compatibility(
    name = "Color Widgets - iWidgets",
    packageName = "colorwidgets.ios.widget.topwidgets",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0xFF6B9D,
    targets = listOf(
        AppTarget(version = "1.8.5", versionCode = 105, minSdk = 32),
    ),
)

/**
 * SharedPreferences key "is_vip".
 * Returning true keeps premium widgets and disables banner ads offline.
 */
private object IsVipFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("is_vip"),
)

/**
 * Product id / preference name used by BillingRepo for VIP status.
 */
private object ColorWidgetsPremiumFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("color_widgets_premium"),
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Local Premium",
    description = "Uses a local preference so premium widgets and no-ads stay available offline.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    execute {
        runCatching { IsVipFingerprint.method.returnEarly(true) }
        runCatching { ColorWidgetsPremiumFingerprint.method.returnEarly(true) }
    }
}
