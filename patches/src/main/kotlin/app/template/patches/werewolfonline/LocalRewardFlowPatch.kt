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
 * show(requestId, adUnitId, showOptions, promise)
 * Fire opened + earned + closed, resolve promise, no real ad UI.
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

@Suppress("unused")
val localRewardFlowPatch = bytecodePatch(
    name = "Local reward flow",
    description = "Grants the rewarded-ad reward when show is requested, without a real ad.",
    default = true,
) {
    compatibleWith(WOLVESVILLE)

    execute {
        // show: p0=this, p1=requestId, p2=adUnitId, p3=options, p4=promise
        FullScreenShowFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Lcom/facebook/react/bridge/Arguments;->createMap()Lcom/facebook/react/bridge/WritableMap;
                move-result-object v5
                const-string v0, "type"
                const-string v1, "gold"
                invoke-interface {v5, v0, v1}, Lcom/facebook/react/bridge/WritableMap;->putString(Ljava/lang/String;Ljava/lang/String;)V
                const-string v0, "amount"
                const/4 v1, 0x1
                invoke-interface {v5, v0, v1}, Lcom/facebook/react/bridge/WritableMap;->putInt(Ljava/lang/String;I)V
                const-string v0, "google_mobile_ads_rewarded_event"
                move v1, p1
                const-string v2, "opened"
                move-object v3, p2
                const/4 v4, 0x0
                const/4 v5, 0x0
                invoke-static/range {v0 .. v5}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                invoke-static {}, Lcom/facebook/react/bridge/Arguments;->createMap()Lcom/facebook/react/bridge/WritableMap;
                move-result-object v5
                const-string v0, "type"
                const-string v1, "gold"
                invoke-interface {v5, v0, v1}, Lcom/facebook/react/bridge/WritableMap;->putString(Ljava/lang/String;Ljava/lang/String;)V
                const-string v0, "amount"
                const/4 v1, 0x1
                invoke-interface {v5, v0, v1}, Lcom/facebook/react/bridge/WritableMap;->putInt(Ljava/lang/String;I)V
                const-string v0, "google_mobile_ads_rewarded_event"
                move v1, p1
                const-string v2, "rewarded_earned_reward"
                move-object v3, p2
                const/4 v4, 0x0
                invoke-static/range {v0 .. v5}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                const-string v0, "google_mobile_ads_rewarded_event"
                move v1, p1
                const-string v2, "closed"
                move-object v3, p2
                const/4 v4, 0x0
                const/4 v5, 0x0
                invoke-static/range {v0 .. v5}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                const/4 v0, 0x0
                invoke-interface {p4, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
    }
}
