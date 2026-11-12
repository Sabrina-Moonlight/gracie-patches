package app.template.patches.kidsdoctorgames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import java.util.logging.Logger

/**
 * Kids doctor games 1.12 (versionCode 466) — BB Pass / SharedSub unlock.
 *
 * What this covers (Java / JNI side):
 * - SharedSubClient / SharedSubStore / TrustPolicy / BbId trust → always active
 * - Play Billing Purchase state → PURCHASED + acknowledged
 * - On UnityPlayerActivity.onCreate → write local SharedSub active state
 *
 * What still lives in Unity IL2CPP (not patched here):
 * - IsFullAccess / HasThisAppPurchase / ResolveAccessState pure C# logic
 * - Server receipt validation (api.bimiboo.net / api.kodomobi.ru)
 *
 * If the paywall remains after a clean install of this patch, the remaining
 * gate is Unity-native and needs IL2CPP work.
 */
private val KIDS_DOCTOR_GAMES = Compatibility(
    name = "Kids doctor games",
    packageName = "com.bimiboo.kids.doctor.games",
    apkFileType = ApkFileType.APKS,
    appIconColor = 0x2196F3,
    targets = listOf(
        AppTarget(version = "1.12", versionCode = 466, minSdk = 32),
    ),
)

private const val HELPER =
    "Lapp/template/extension/shared/BbPassUnlockHelper;"

@Suppress("unused")
val unlockSubscriptionPatch = bytecodePatch(
    name = "Unlock subscription",
    description = "Unlocks BB Pass shared subscription and marks Play purchases as owned. " +
        "Activates local SharedSub state when the game starts.",
    default = true,
) {
    compatibleWith(KIDS_DOCTOR_GAMES)
    extendWith("extensions/extension.mpe")

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Boolean gates
        HasTrustedActiveSharedSubscriptionFingerprint.method.returnEarly(true)
        QueryPackageFingerprint.method.returnEarly(true)
        SharedSubStoreIsActiveFingerprint.method.returnEarly(true)
        SharedSubTrustPolicyIsTrustedFingerprint.method.returnEarly(true)
        runCatching { BbIdCrossAppTrustIsTrustedFingerprint.method.returnEarly(true) }

        // Expiry far future
        forceFarExpiry(TrustedActiveSharedSubscriptionExpiryFingerprint.method)
        forceFarExpiry(QueryPackageExpiryFingerprint.method)
        forceFarExpiry(SharedSubStoreExpiresAtFingerprint.method)

        // Billing Purchase
        PurchaseGetPurchaseStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)

        // Activate SharedSub when Unity activity starts (p0 = Activity extends Context)
        UnityPlayerActivityOnCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $HELPER->activate(Landroid/content/Context;)V
            """.trimIndent(),
        )

        logger.info("Kids doctor games: SharedSub + Purchase + startup activate applied")
    }
}

private fun forceFarExpiry(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) {
    method.addInstructions(
        0,
        """
            const-wide v0, 0x7ffffffffffffL
            return-wide v0
        """.trimIndent(),
    )
}
