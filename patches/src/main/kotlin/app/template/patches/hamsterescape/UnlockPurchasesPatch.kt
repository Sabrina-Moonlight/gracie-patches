package app.template.patches.hamsterescape

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import app.template.patches.shared.cert.extractApkCertificatePatch
import app.template.patches.shared.firebase.spoofFirebaseCertHashPatch
import app.template.patches.shared.installer.spoofInstallSourcePatch
import app.template.patches.shared.signature.spoofSignatureVerificationPatch

private val HAMSTER_ESCAPE = Compatibility(
    name = "Hamster Escape",
    packageName = "gg.sunday.hamsterescape",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xFF8C42,
    targets = listOf(
        AppTarget(version = "5.2.0"),
    ),
)

/**
 * com.android.billingclient.api.Purchase#getPurchaseState()I
 *
 * Play Billing states: 0 UNSPECIFIED_STATE, 1 PURCHASED, 2 PENDING.
 * Forcing 1 makes every Purchase object report as owned when Unity IAP
 * (com.unity.purchasing 5.0.2) and Sunday.NoAdsSystem process query/restore results.
 *
 * Verified present in classes5.dex / classes8.dex of build 5.2.0.
 */
private object PurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

/**
 * com.android.billingclient.api.Purchase#isAcknowledged()Z
 *
 * Unity Purchasing ignores purchases that are not acknowledged.
 * Returning true keeps restore + entitlement paths complete.
 */
private object PurchaseAcknowledgedFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "isAcknowledged",
    returnType = "Z",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

@Suppress("unused")
val unlockPurchasesPatch = bytecodePatch(
    name = "Unlock purchases",
    description = "Marks every Play Billing Purchase as PURCHASED and acknowledged so Hamster Escape treats offers as owned on restore and query.",
    default = true,
) {
    compatibleWith(HAMSTER_ESCAPE)

    dependsOn(
        extractApkCertificatePatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
        spoofSignatureVerificationPatch,
    )

    execute {
        // getPurchaseState() → 1 (PURCHASED)
        PurchaseStateFingerprint.method.returnEarly(1)

        // isAcknowledged() → true
        PurchaseAcknowledgedFingerprint.method.returnEarly(true)
    }
}
