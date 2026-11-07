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
 * Method that reads pro_lock / premium and returns whether the feature is locked.
 * Returning false = unlocked (same logic the app uses when the preference is true).
 * No instruction rewriting — only returnEarly, to avoid verify crashes on open.
 */
private object ProLockCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Uses a local preference so optional widget features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        runCatching {
            ProLockCheckFingerprint.method.returnEarly(false)
        }
    }
}
