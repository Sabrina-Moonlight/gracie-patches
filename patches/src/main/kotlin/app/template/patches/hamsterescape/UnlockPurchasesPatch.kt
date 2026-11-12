package app.template.patches.hamsterescape

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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
    targets = listOf(AppTarget(version = null)),
)

private const val EXTENSION_CLASS =
    "Lapp/template/extension/shared/HamsterPlayPurchases;"

/**
 * BillingClient / Unity GPBL callback:
 * onQueryPurchasesResponse(BillingResult, List<Purchase>)
 * Inject synthetic purchases before the listener body runs.
 */
private object QueryPurchasesResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/List;"),
    name = "onQueryPurchasesResponse",
)

/** Purchase#getPurchaseState()I → 1 PURCHASED */
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
    description = "Injects synthetic owned Play purchases and forces PURCHASED/acknowledged so Hamster Escape unlocks No Ads and store offers.",
    default = true,
) {
    compatibleWith(HAMSTER_ESCAPE)

    dependsOn(
        extractApkCertificatePatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
        spoofSignatureVerificationPatch,
    )

    extendWith("extensions/extension.mpe")

    execute {
        // Fill empty query results with synthetic owned products.
        // p2 = List<Purchase> on onQueryPurchasesResponse(BillingResult, List)
        QueryPurchasesResponseFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p2 }, $EXTENSION_CLASS->withSyntheticPurchases(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """.trimIndent(),
        )

        PurchaseStateFingerprint.method.returnEarly(1)
        PurchaseAcknowledgedFingerprint.method.returnEarly(true)
    }
}
