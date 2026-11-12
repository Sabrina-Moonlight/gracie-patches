package app.template.patches.kidsdoctorgames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import java.util.logging.Logger

/**
 * Kids doctor games (com.bimiboo.kids.doctor.games) 1.12 / versionCode 466.
 *
 * Forces BB Pass SharedSub gates so the app treats the shared subscription as active:
 * - hasTrustedActiveSharedSubscription / queryPackage / isActive / isTrusted → true
 * - expiry getters → far-future timestamp
 *
 * This build has no PairIP / VMRunner, so no protection dependsOn are required.
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
    description = "Unlocks BB Pass / shared subscription so all content stays available.",
    default = true,
) {
    compatibleWith(KIDS_DOCTOR_GAMES)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Boolean gates → always true
        HasTrustedActiveSharedSubscriptionFingerprint.method.returnEarly(true)
        QueryPackageFingerprint.method.returnEarly(true)
        SharedSubStoreIsActiveFingerprint.method.returnEarly(true)
        SharedSubTrustPolicyIsTrustedFingerprint.method.returnEarly(true)

        // Expiry → far future (~year 2286)
        forceFarExpiry(TrustedActiveSharedSubscriptionExpiryFingerprint.method)
        forceFarExpiry(QueryPackageExpiryFingerprint.method)
        forceFarExpiry(SharedSubStoreExpiresAtFingerprint.method)

        logger.info("Kids doctor games: BB Pass / SharedSub gates unlocked")
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
