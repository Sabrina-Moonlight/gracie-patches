package app.template.patches.werewolfonline

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val WOLVESVILLE = Compatibility(
    name = "Wolvesville",
    packageName = "com.werewolfapps.online",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x8B4513,
    targets = listOf(AppTarget(version = "2.7.188", versionCode = 4194949)),
)

private object CreateClientBuilderFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/OkHttpClientProvider;",
    name = "createClientBuilder",
    returnType = "Lokhttp3/OkHttpClient\$Builder;",
    parameters = listOf(),
)

private object CreateClientBuilderContextFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/OkHttpClientProvider;",
    name = "createClientBuilder",
    returnType = "Lokhttp3/OkHttpClient\$Builder;",
    parameters = listOf("Landroid/content/Context;", "I"),
)

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
    description = "Local ad reward + rewrite goldenWheelSpin API response only.",
    default = true,
) {
    compatibleWith(WOLVESVILLE)

    extendWith("extensions/extension.mpe")

    execute {
        // Network: install interceptor just before return of createClientBuilder()
        val builderMethod = CreateClientBuilderFingerprint.method
        val returnIndex = builderMethod.instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        if (returnIndex < 0) {
            throw IllegalStateException("RETURN_OBJECT not found in createClientBuilder")
        }
        val reg = builderMethod.getInstruction<OneRegisterInstruction>(returnIndex).registerA
        builderMethod.addInstructions(
            returnIndex,
            """
                invoke-static {v$reg}, Lapp/template/extension/werewolf/WheelNetworkHook;->install(Lokhttp3/OkHttpClient${'$'}Builder;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v$reg
            """,
        )

        val builderCtxMethod = CreateClientBuilderContextFingerprint.method
        val returnIndexCtx = builderCtxMethod.instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        if (returnIndexCtx < 0) {
            throw IllegalStateException("RETURN_OBJECT not found in createClientBuilder(Context,I)")
        }
        val regCtx = builderCtxMethod.getInstruction<OneRegisterInstruction>(returnIndexCtx).registerA
        builderCtxMethod.addInstructions(
            returnIndexCtx,
            """
                invoke-static {v$regCtx}, Lapp/template/extension/werewolf/WheelNetworkHook;->install(Lokhttp3/OkHttpClient${'$'}Builder;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v$regCtx
            """,
        )

        // Ads: grant on show without real UI
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
