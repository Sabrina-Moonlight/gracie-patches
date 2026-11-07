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
 * OkHttpClientProvider.createClient() — attach network hook before build.
 */
private object CreateClientFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/OkHttpClientProvider;",
    name = "createClient",
    returnType = "Lokhttp3/OkHttpClient;",
    parameters = listOf(),
)

/**
 * createClient(Context) overload.
 */
private object CreateClientContextFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/OkHttpClientProvider;",
    name = "createClient",
    returnType = "Lokhttp3/OkHttpClient;",
    parameters = listOf("Landroid/content/Context;"),
)

/**
 * show on UI thread lambda — grant ad reward without real ad UI.
 */
private object ShowOnUiThreadFingerprint : Fingerprint(
    definingClass = "Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsFullScreenAdModule;",
    name = "show\$lambda\$2",
    returnType = "V",
    parameters = listOf(
        "Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsFullScreenAdModule;",
        "I",
        "Lcom/facebook/react/bridge/ReadableMap;",
        "Landroid/app/Activity;",
        "Lcom/facebook/react/bridge/Promise;",
        "Ljava/lang/String;",
    ),
)

@Suppress("unused")
val localRewardFlowPatch = bytecodePatch(
    name = "Local reward flow",
    description = "Grants rewarded ads locally and rewrites golden-wheel API responses so the spin can succeed.",
    default = true,
) {
    compatibleWith(WOLVESVILLE)

    extendWith("extensions/extension.mpe")

    execute {
        // createClient(): Builder + interceptor + build
        CreateClientFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Lcom/facebook/react/modules/network/OkHttpClientProvider;->createClientBuilder()Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v0
                sget-object v1, Lapp/template/extension/werewolf/WheelNetworkHook;->INSTANCE:Lapp/template/extension/werewolf/WheelNetworkHook;
                invoke-virtual {v0, v1}, Lokhttp3/OkHttpClient${'$'}Builder;->addInterceptor(Lokhttp3/Interceptor;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v0
                invoke-virtual {v0}, Lokhttp3/OkHttpClient${'$'}Builder;->build()Lokhttp3/OkHttpClient;
                move-result-object v0
                return-object v0
            """,
        )

        // createClient(Context): same with context builder
        CreateClientContextFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, Lcom/facebook/react/modules/network/OkHttpClientProvider;->createClientBuilder(Landroid/content/Context;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v0
                sget-object v1, Lapp/template/extension/werewolf/WheelNetworkHook;->INSTANCE:Lapp/template/extension/werewolf/WheelNetworkHook;
                invoke-virtual {v0, v1}, Lokhttp3/OkHttpClient${'$'}Builder;->addInterceptor(Lokhttp3/Interceptor;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v0
                invoke-virtual {v0}, Lokhttp3/OkHttpClient${'$'}Builder;->build()Lokhttp3/OkHttpClient;
                move-result-object v0
                return-object v0
            """,
        )

        // Ad show on UI thread: opened + earned + closed
        ShowOnUiThreadFingerprint.method.addInstructions(
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
                move-object v3, p5
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
                move-object v3, p5
                const/4 v4, 0x0
                invoke-static/range {v0 .. v5}, Lio/invertase/googlemobileads/ReactNativeGoogleMobileAdsCommon;->sendAdEvent(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lcom/facebook/react/bridge/WritableMap;Lcom/facebook/react/bridge/WritableMap;)V
                const-string v0, "google_mobile_ads_rewarded_event"
                move v1, p1
                const-string v2, "closed"
                move-object v3, p5
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
