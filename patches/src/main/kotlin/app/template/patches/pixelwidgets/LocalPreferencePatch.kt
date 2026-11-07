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

/** Lfc1.e — returns false when premium is active. Force false. */
private object IsFeatureLockedFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "e",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

/** Lfc1.c — used by widgets. Force false (unlocked path). */
private object WidgetLockFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "c",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "[I"),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces local premium preference so all features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        IsFeatureLockedFingerprint.method.returnEarly(false)
        runCatching { WidgetLockFingerprint.method.returnEarly(false) }
    }
}
