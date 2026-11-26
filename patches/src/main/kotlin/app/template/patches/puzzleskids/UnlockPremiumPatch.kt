package app.template.patches.puzzleskids

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

/**
 * Puzzles Kids — Animal Jigsaw
 * Package: com.gamesforkids.animals.puzzles.jigsaw
 *
 * Unity IL2CPP + Play Billing. GameData flags: isFullVersion, isAdFree.
 * Paywall still showed after DEX-only Purchase force — app reads local flags.
 *
 * Layers:
 *  A) Inject synthetic owned purchases on query response (product id candidates)
 *  B) Purchase.getPurchaseState → 1, isAcknowledged → true
 *  C) On UnityPlayerActivity onCreate/onResume write prefs (isFullVersion / isAdFree / …)
 */
private val PUZZLES_KIDS = Compatibility(
    name = "Puzzles Kids",
    packageName = "com.gamesforkids.animals.puzzles.jigsaw",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = null)),
)

private const val HELPER =
    "Lapp/template/extension/shared/PuzzlesKidsUnlockHelper;"

private object QueryPurchasesResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/List;"),
    name = "onQueryPurchasesResponse",
)

private object PurchaseGetPurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

private object PurchaseIsAcknowledgedFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "isAcknowledged",
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

private object UnityPlayerActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/player/UnityPlayerActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

private object UnityPlayerActivityOnResumeFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/player/UnityPlayerActivity;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Full Version and Remove Ads (prefs + synthetic purchases + billing force).",
    default = true,
) {
    compatibleWith(PUZZLES_KIDS)

    dependsOn(
        extractApkCertificatePatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
        spoofSignatureVerificationPatch,
    )

    extendWith("extensions/extension.mpe")

    execute {
        // A) synthetic purchases on query
        QueryPurchasesResponseFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p2 }, $HELPER->withSyntheticPurchases(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """.trimIndent(),
        )

        // B) every Purchase looks owned
        PurchaseGetPurchaseStateFingerprint.method.returnEarly(1)
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)

        // C) write isFullVersion / isAdFree prefs before Unity runs
        UnityPlayerActivityOnCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, $HELPER->activate(Landroid/content/Context;)V
            """.trimIndent(),
        )
        runCatching {
            UnityPlayerActivityOnResumeFingerprint.method.addInstructions(
                0,
                """
                    invoke-static { p0 }, $HELPER->activate(Landroid/content/Context;)V
                """.trimIndent(),
            )
        }
    }
}
