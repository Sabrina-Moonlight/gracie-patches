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
 * MaxInterstitialAd / MaxRewardedAd show and load entry points.
 * Fixed UI-thread log strings keep the match stable.
 */
private object ShowAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("showAd must be called on the main UI thread."),
)

private object LoadAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("loadAd must be called on the main UI thread."),
)

private object ShowInterstitialFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("showInterstitial must be called on the main UI thread."),
)

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide Ads",
    description = "Stops interstitial and rewarded ads from loading or showing, including the ones that ask before applying.",
    default = true,
) {
    compatibleWith(THEMEZY)

    execute {
        runCatching { ShowAdFingerprint.method.returnEarly() }
        runCatching { LoadAdFingerprint.method.returnEarly() }
        runCatching { ShowInterstitialFingerprint.method.returnEarly() }
    }
}
