package app.template.patches.kidsdoctorgames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import java.util.logging.Logger

/**
 * Kids doctor games 1.12 (versionCode 466) — BB Pass / SharedSub unlock (Java side).
 *
 * Works together with [unlockIl2CppNativePatch] which forces the Unity IL2CPP gates
 * (HasThisAppPurchase, IsOwnedSharedActive, offline grants, IsFullAccess).
 *
 * Java coverage:
 * - SharedSubClient / SharedSubStore / TrustPolicy / BbId trust → always active
 * - Play Billing Purchase state → PURCHASED + acknowledged
 * - UnityPlayerActivity.onCreate → write local SharedSub active state via helper
 */
internal val KIDS_DOCTOR_GAMES = Compatibility(
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
    name = "Unlock subscription (Java / SharedSub)",
    description = "Unlocks BB Pass shared subscription on the Java/JNI side and marks " +
        "Play purchases as owned. Activates local SharedSub state when the game starts. " +
        "Must be used together with Unlock subscription (IL2CPP native).",
    default = true,
) {
    compatibleWith(KIDS_DOCTOR_GAMES)
    extendWith("extensions/extension.mpe")
    // Native IL2CPP gates are a separate patch (unlockIl2CppNativePatch).
    // Enable both in Morphe; order does not matter.

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
