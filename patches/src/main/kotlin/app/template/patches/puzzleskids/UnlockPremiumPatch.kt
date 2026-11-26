package app.template.patches.puzzleskids

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

/**
 * Puzzles Kids — Animal Jigsaw (com.gamesforkids.animals.puzzles.jigsaw)
 *
 * Unity IL2CPP + Google Play Billing + Unity IAP.
 * Product IDs (metadata):
 *   kProductIDNonConsumableRemoveAds
 *   kProductIDNonConsumableFullVersion
 *   kProductIDNonConsumableUpgradeVersion
 *   kProductIDNonConsumablePlaypass
 * Completes: IAPRemoveAdsComplete / IAPFullVersionComplete → isFullVersion + RemoveAds.
 *
 * Forces Billing Library Purchase objects to report PURCHASED + acknowledged so
 * Unity IAP restores ownership and sets PlayerPrefs flags.
 */
private val PUZZLES_KIDS = Compatibility(
    name = "Puzzles Kids",
    packageName = "com.gamesforkids.animals.puzzles.jigsaw",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = null)),
)

/** Lcom/android/billingclient/api/Purchase;->getPurchaseState()I → PURCHASED (1) */
private object PurchaseGetPurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = listOf(),
)

/** Lcom/android/billingclient/api/Purchase;->isAcknowledged()Z → true */
private object PurchaseIsAcknowledgedFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "isAcknowledged",
    returnType = "Z",
    parameters = listOf(),
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Full Version and Remove Ads (forces Play Billing purchases as owned).",
    default = true,
) {
    compatibleWith(PUZZLES_KIDS)

    execute {
        PurchaseGetPurchaseStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)
    }
}
