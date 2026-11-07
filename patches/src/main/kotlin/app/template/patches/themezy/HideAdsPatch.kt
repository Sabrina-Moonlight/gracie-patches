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

// AppLovin MAX interstitial
private object MaxInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "showAd",
    returnType = "V",
)

private object MaxInterstitialLoadFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "loadAd",
    returnType = "V",
)

// AppLovin MAX rewarded (the ones that ask before applying)
private object MaxRewardedShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    returnType = "V",
)

private object MaxRewardedLoadFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "loadAd",
    returnType = "V",
)

// isReady → always false so UI thinks no ad is ready
private object MaxInterstitialIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "isReady",
    returnType = "Z",
)

private object MaxRewardedIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
)

// Fallback by fixed log strings (in case class names shift)
private object ShowAdStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("showAd must be called on the main UI thread."),
)

private object LoadAdStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("loadAd must be called on the main UI thread."),
)

private object ShowInterstitialStringFingerprint : Fingerprint(
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
        runCatching { MaxInterstitialShowFingerprint.method.returnEarly() }
        runCatching { MaxInterstitialLoadFingerprint.method.returnEarly() }
        runCatching { MaxRewardedShowFingerprint.method.returnEarly() }
        runCatching { MaxRewardedLoadFingerprint.method.returnEarly() }
        runCatching { MaxInterstitialIsReadyFingerprint.method.returnEarly(false) }
        runCatching { MaxRewardedIsReadyFingerprint.method.returnEarly(false) }
        runCatching { ShowAdStringFingerprint.method.returnEarly() }
        runCatching { LoadAdStringFingerprint.method.returnEarly() }
        runCatching { ShowInterstitialStringFingerprint.method.returnEarly() }
    }
}
