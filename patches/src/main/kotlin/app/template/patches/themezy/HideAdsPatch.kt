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

// ── AppLovin MAX ──────────────────────────────────────────────
private object MaxInterstitialShow : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "showAd",
    returnType = "V",
)
private object MaxInterstitialLoad : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "loadAd",
    returnType = "V",
)
private object MaxRewardedShow : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    returnType = "V",
)
private object MaxRewardedLoad : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "loadAd",
    returnType = "V",
)
private object MaxInterstitialIsReady : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "isReady",
    returnType = "Z",
)
private object MaxRewardedIsReady : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
)

// ── App show / preload (leansoft + themezy BaseFragment) ─────
// Names taken from the dex string table of this exact build.
private object ShowInterAd : Fingerprint(name = "showInterAd", returnType = "V")
private object ShowInterNativeAd : Fingerprint(name = "showInterNativeAd", returnType = "V")
private object ShowInterRewardAd : Fingerprint(name = "showInterRewardAd", returnType = "V")
private object ShowRewardAd : Fingerprint(name = "showRewardAd", returnType = "V")
private object ShowNativeFull : Fingerprint(name = "showNativeFull", returnType = "V")
private object ShowNativeFullDialog : Fingerprint(name = "showNativeFullDialog", returnType = "V")
private object ShowAdNativeCollapsible : Fingerprint(
    definingClass = "Lcom/themezy/theme/iconwidgets/base/BaseFragment;",
    name = "showAdNativeCollapsible",
    returnType = "V",
)
private object ShowAppOpen : Fingerprint(name = "showAppOpen", returnType = "V")
private object PreloadAppOpenAd : Fingerprint(name = "preloadAppOpenAd", returnType = "V")
private object PreloadBannerAd : Fingerprint(name = "preloadBannerAd", returnType = "V")
private object PreloadInterAd : Fingerprint(name = "preloadInterAd", returnType = "V")
private object PreloadNativeAd : Fingerprint(name = "preloadNativeAd", returnType = "V")
private object PreloadRewardAd : Fingerprint(name = "preloadRewardAd", returnType = "V")
private object PreloadInterRewardAd : Fingerprint(name = "preloadInterRewardAd", returnType = "V")
private object LoadAds : Fingerprint(name = "loadAds", returnType = "V")

// ── Fallback by fixed log strings ─────────────────────────────
private object ShowAdString : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("showAd must be called on the main UI thread."),
)
private object LoadAdString : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("loadAd must be called on the main UI thread."),
)
private object ShowInterstitialString : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("showInterstitial must be called on the main UI thread."),
)

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide Ads",
    description = "Stops every ad window: interstitial, rewarded, native, banner, app-open and the ones that ask before applying.",
    default = true,
) {
    compatibleWith(THEMEZY)

    execute {
        // AppLovin
        runCatching { MaxInterstitialShow.method.returnEarly() }
        runCatching { MaxInterstitialLoad.method.returnEarly() }
        runCatching { MaxRewardedShow.method.returnEarly() }
        runCatching { MaxRewardedLoad.method.returnEarly() }
        runCatching { MaxInterstitialIsReady.method.returnEarly(false) }
        runCatching { MaxRewardedIsReady.method.returnEarly(false) }

        // App-level show / preload
        runCatching { ShowInterAd.method.returnEarly() }
        runCatching { ShowInterNativeAd.method.returnEarly() }
        runCatching { ShowInterRewardAd.method.returnEarly() }
        runCatching { ShowRewardAd.method.returnEarly() }
        runCatching { ShowNativeFull.method.returnEarly() }
        runCatching { ShowNativeFullDialog.method.returnEarly() }
        runCatching { ShowAdNativeCollapsible.method.returnEarly() }
        runCatching { ShowAppOpen.method.returnEarly() }
        runCatching { PreloadAppOpenAd.method.returnEarly() }
        runCatching { PreloadBannerAd.method.returnEarly() }
        runCatching { PreloadInterAd.method.returnEarly() }
        runCatching { PreloadNativeAd.method.returnEarly() }
        runCatching { PreloadRewardAd.method.returnEarly() }
        runCatching { PreloadInterRewardAd.method.returnEarly() }
        runCatching { LoadAds.method.returnEarly() }

        // String fallbacks
        runCatching { ShowAdString.method.returnEarly() }
        runCatching { LoadAdString.method.returnEarly() }
        runCatching { ShowInterstitialString.method.returnEarly() }
    }
}
