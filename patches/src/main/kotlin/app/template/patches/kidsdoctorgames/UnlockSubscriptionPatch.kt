package app.template.patches.kidsdoctorgames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import java.util.logging.Logger

/**
 * Kids doctor games 1.12 (versionCode 466).
 *
 * Layers:
 * 1) BB Pass SharedSub + BbId trust gates → always active / trusted
 * 2) Google Play Billing Purchase.getPurchaseState → PURCHASED (1)
 * 3) Purchase.isAcknowledged → true
 *
 * Note: paywalls are also gated in Unity IL2CPP (IsFullAccess). Java-side
 * unlock covers SharedSub / Billing objects; if Unity still shows the wall,
 * native IL2CPP work is needed next.
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

@Suppress("unused")
val unlockSubscriptionPatch = bytecodePatch(
    name = "Unlock subscription",
    description = "Unlocks BB Pass / shared subscription and forces Play Billing purchases as owned.",
    default = true,
) {
    compatibleWith(KIDS_DOCTOR_GAMES)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // --- BB Pass SharedSub ---
        HasTrustedActiveSharedSubscriptionFingerprint.method.returnEarly(true)
        QueryPackageFingerprint.method.returnEarly(true)
        SharedSubStoreIsActiveFingerprint.method.returnEarly(true)
        SharedSubTrustPolicyIsTrustedFingerprint.method.returnEarly(true)
        runCatching {
            BbIdCrossAppTrustIsTrustedFingerprint.method.returnEarly(true)
        }

        forceFarExpiry(TrustedActiveSharedSubscriptionExpiryFingerprint.method)
        forceFarExpiry(QueryPackageExpiryFingerprint.method)
        forceFarExpiry(SharedSubStoreExpiresAtFingerprint.method)

        // --- Play Billing Purchase objects ---
        // PURCHASED = 1
        PurchaseGetPurchaseStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)

        logger.info("Kids doctor games: SharedSub + Purchase state forced owned")
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
