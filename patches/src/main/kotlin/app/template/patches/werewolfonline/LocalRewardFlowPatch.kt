package app.template.patches.werewolfonline

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private val WOLVESVILLE = Compatibility(
    name = "Wolvesville",
    packageName = "com.werewolfapps.online",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x8B4513,
    targets = listOf(AppTarget(version = "2.7.188", versionCode = 4194949)),
)

/**
 * FullScreenAdModule.show(requestId, adUnitId, showOptions, promise)
 * When the app asks to show a rewarded ad, we grant the reward at once
 * and resolve the promise so the gold / spin is given without a real ad.
 */
private object FullScreenShowFingerprint : Fingerprint(
    definingClass = "Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsFullScreenAdModule;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "I",
        "Ljava/lang/String;",
        "Lcom/facebook/react/bridge/ReadableMap;",
        "Lcom/facebook/react/bridge/Promise;",
    ),
)

/**
 * FullScreenAdModule.load(requestId, adUnitId, adRequestOptions)
 * Skip the real network load and tell the app the ad is ready.
 */
private object FullScreenLoadFingerprint : Fingerprint(
    definingClass = "Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsFullScreenAdModule;",
    name = "load",
    returnType = "V",
    parameters = listOf(
        "I",
        "Ljava/lang/String;",
        "Lcom/facebook/react/bridge/ReadableMap;",
    ),
)

@Suppress("unused")
val localRewardFlowPatch = bytecodePatch(
    name = "Local reward flow",
    description = "Confirms the reward as soon as a rewarded ad is requested, without showing a real ad.",
    default = true,
) {
    compatibleWith(WOLVESVILLE)

    execute {
        // load: send "loaded" and return. No real ad is requested.
        // p1 = requestId (I), p2 = adUnitId (String)
        FullScreenLoadFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Lcom/facebook/react/bridge/Arguments;->createMap()Lcom/facebook/react/bridge/WritableMap;
                move-result-object v0
                const-string v1, "google_mobile_ads_rewarded_event"
                const-string v2, "loaded"
                const/4 v3, 0x0
                invoke-static {v1, p1, v2, p2, v3, v0}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                return-void
            """,
        )

        // show: send rewarded_earned_reward, resolve promise, return.
        // p1 = requestId, p2 = adUnitId, p4 = Promise
        FullScreenShowFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Lcom/facebook/react/bridge/Arguments;->createMap()Lcom/facebook/react/bridge/WritableMap;
                move-result-object v0
                const-string v1, "type"
                const-string v2, "gold"
                invoke-interface {v0, v1, v2}, Lcom/facebook/react/bridge/WritableMap;->putString(Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "amount"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Lcom/facebook/react/bridge/WritableMap;->putInt(Ljava/lang/String;I)V
                const-string v1, "google_mobile_ads_rewarded_event"
                const-string v2, "rewarded_earned_reward"
                const/4 v3, 0x0
                invoke-static {v1, p1, v2, p2, v3, v0}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                const/4 v0, 0x0
                invoke-interface {p4, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
    }
}
