package app.template.patches.puzzleskids

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import app.template.patches.shared.cert.extractApkCertificatePatch
import app.template.patches.shared.firebase.spoofFirebaseCertHashPatch
import app.template.patches.shared.installer.spoofInstallSourcePatch
import app.template.patches.shared.signature.spoofSignatureVerificationPatch
import java.io.RandomAccessFile
import java.util.logging.Logger

/**
 * Puzzles Kids — Animal Jigsaw (com.gamesforkids.animals.puzzles.jigsaw)
 *
 * Full unlock from Il2CppDumper v31 on this build's libil2cpp.so:
 *   GameData.isFullVersion / isAdFree
 *   Product IDs: full_version, full_version_playpass, remove_ads, upgrade_version
 *   GameManager.ShowIAPCanvas / ShowInterstitial / ShowBanner
 *   GooglePurchase.IsPurchased, Product.get_hasReceipt
 *   PurchaserIAP.IsInitialized / ProcessPurchase
 *
 * File offsets = RVA - 0x4000 (confirmed against dump.cs Offset field).
 */
private val PUZZLES_KIDS = Compatibility(
    name = "Puzzles Kids",
    packageName = "com.gamesforkids.animals.puzzles.jigsaw",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = null)),
)

private const val LIB = "lib/arm64-v8a/libil2cpp.so"
private const val HELPER =
    "Lapp/template/extension/shared/PuzzlesKidsUnlockHelper;"

private val RET = hexBytes("c0 03 5f d6")                 // RET
private val RETURN_TRUE = hexBytes("20 00 80 52 c0 03 5f d6") // MOV W0,#1 ; RET
private val RETURN_FALSE = hexBytes("00 00 80 52 c0 03 5f d6") // MOV W0,#0 ; RET
private val RETURN_COMPLETE = hexBytes("00 00 80 52 c0 03 5f d6") // ProcessPurchase Complete=0

private data class Il2CppGate(
    val label: String,
    val offset: Long,
    val replacement: ByteArray,
)

/**
 * Offsets from Il2CppDumper script.json Address (RVA) minus 0x4000.
 * Verified prologues match dump before write.
 */
private val IL2CPP_GATES = listOf(
    // Hide paywall
    Il2CppGate("GameManager$$ShowIAPCanvas", 0x10c4ca4, RET),
    // Kill ads
    Il2CppGate("GameManager$$ShowInterstitial", 0x10c7808, RET),
    Il2CppGate("GameManager$$ShowBanner", 0x10c8238, RET),
    Il2CppGate("GameManager$$RequestInterstitial", 0x10c7944, RET),
    Il2CppGate("GameManager$$ShowUnityBanner", 0x10d39a8, RET),
    Il2CppGate("GameManager$$ShowUnityAd", 0x10d3a48, RET),
    // Ownership
    Il2CppGate("GooglePurchase$$IsPurchased", 0x2159ee0, RETURN_TRUE),
    Il2CppGate("Product$$get_hasReceipt", 0x2125194, RETURN_TRUE),
    Il2CppGate("PurchaserIAP$$IsInitialized", 0x10df2ac, RETURN_TRUE),
    // Finish any purchase flow as success without failing
    Il2CppGate("PurchaserIAP$$ProcessPurchase", 0x10dff3c, RETURN_COMPLETE),
    // IAP canvas status: do not open locked shop logic
    Il2CppGate("IapCanvas$$CheckPurchaseStatus", 0x109bf74, RET),
)

private fun hexBytes(pattern: String): ByteArray =
    pattern.trim().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()

private val unlockIl2CppNativeInternalPatch = rawResourcePatch {
    execute {
        val logger = Logger.getLogger("puzzleskids.il2cpp")
        val target = get(LIB, true)
        if (!target.exists()) {
            throw PatchException("$LIB missing — apply patch to the APKS that contains arm64-v8a")
        }
        RandomAccessFile(target, "rw").use { raf ->
            for (gate in IL2CPP_GATES) {
                if (gate.offset + gate.replacement.size > raf.length()) {
                    throw PatchException("${gate.label}: offset past EOF")
                }
                raf.seek(gate.offset)
                raf.write(gate.replacement)
                logger.info("patched ${gate.label} @ 0x${gate.offset.toString(16)}")
            }
        }
    }
}

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
    description = "Full unlock: IL2CPP paywall/ads/ownership gates + real product ids + prefs.",
    default = true,
) {
    compatibleWith(PUZZLES_KIDS)

    dependsOn(
        extractApkCertificatePatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
        spoofSignatureVerificationPatch,
        unlockIl2CppNativeInternalPatch,
    )

    extendWith("extensions/extension.mpe")

    execute {
        QueryPurchasesResponseFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p2 }, $HELPER->withSyntheticPurchases(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """.trimIndent(),
        )
        PurchaseGetPurchaseStateFingerprint.method.returnEarly(1)
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)
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
