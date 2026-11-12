package app.template.patches.hamsterescape

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import app.template.patches.shared.cert.extractApkCertificatePatch
import app.template.patches.shared.firebase.spoofFirebaseCertHashPatch
import app.template.patches.shared.installer.spoofInstallSourcePatch
import app.template.patches.shared.signature.spoofSignatureVerificationPatch

/**
 * Package from AndroidManifest of build 5.2.0: gg.sunday.hamsterescape
 *
 * targets = emptyList() → Morphe shows this patch for ANY version of that
 * package. A pinned AppTarget(version=...) that does not match the APK
 * versionName exactly makes the patch invisible in the UI.
 */
private val HAMSTER_ESCAPE = Compatibility(
    name = "Hamster Escape",
    packageName = "gg.sunday.hamsterescape",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xFF8C42,
    targets = emptyList(),
)

/** Purchase#getPurchaseState()I → force PURCHASED (1) */
private object PurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

/** Purchase#isAcknowledged()Z → true */
private object PurchaseAcknowledgedFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "isAcknowledged",
    returnType = "Z",
    parameters = emptyList(),
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
        PurchaseStateFingerprint.method.returnEarly(1)
        PurchaseAcknowledgedFingerprint.method.returnEarly(true)
    }
}
