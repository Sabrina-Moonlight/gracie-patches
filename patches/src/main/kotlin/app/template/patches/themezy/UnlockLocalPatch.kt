package app.template.patches.themezy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val THEMEZY = Compatibility(
    name = "Themezy",
    packageName = "com.themezy.theme.iconwidgets",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0x7C4DFF,
    targets = listOf(AppTarget(version = "1.0.9", versionCode = 9)),
)

/**
 * Boolean checks that decide if an icon/theme needs a rewarded ad.
 * Returning true marks them already unlocked offline.
 */
private object IsIconUnlockedFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("_isIconUnlockedAll"),
)

private object IsUnlockedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("isUnlocked"),
)

@Suppress("unused")
val unlockLocalPatch = bytecodePatch(
    name = "Unlock Local",
    description = "Marks icons and themes as already unlocked so the rewarded-ad screen does not appear.",
    default = true,
) {
    compatibleWith(THEMEZY)

    execute {
        runCatching { IsIconUnlockedFingerprint.method.returnEarly(true) }
        runCatching { IsUnlockedFingerprint.method.returnEarly(true) }
    }
}
