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
 * Puzzles Kids — full unlock (Il2CppDumper offsets for this build).
 *
 * Forces GameData.isFullVersion / isAdFree, kills paywall and ads,
 * synthetic IAP with real product ids: full_version, full_version_playpass,
 * remove_ads, upgrade_version.
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

private val RET = hexBytes("c0 03 5f d6")
private val RETURN_TRUE = hexBytes("20 00 80 52 c0 03 5f d6")

/**
 * LDR X8,[X0,#0x20] ; CBZ X8,+3 ; MOV W9,#1 ; STRB W9,[X8,#0x16] ; STRB W9,[X8,#0x17]
 * Then falls into the original instruction at +0x14.
 */
private val FORCE_FLAGS_THEN_FALLTHROUGH = hexBytes(
    "08 10 40 f9" +
        "88 00 00 b4" +
        "29 00 80 52" +
        "09 59 00 39" +
        "09 5d 00 39",
)

/** Flag write then RET (IAP complete replacements). */
private val FORCE_FLAGS_RET = hexBytes(
    "08 10 40 f9" +
        "88 00 00 b4" +
        "29 00 80 52" +
        "09 59 00 39" +
        "09 5d 00 39" +
        "c0 03 5f d6",
)

private data class Il2CppGate(
    val label: String,
    val offset: Long,
    val replacement: ByteArray,
)

// Labels must NOT contain $ — Kotlin string templates treat $Name as a reference.
private val IL2CPP_GATES = listOf(
    Il2CppGate("GameManager.LoadLevel forceFlags", 0x10c4394, FORCE_FLAGS_THEN_FALLTHROUGH),
    Il2CppGate("GameManager.Awake forceFlags", 0x10d27e0, FORCE_FLAGS_THEN_FALLTHROUGH),
    Il2CppGate("GameManager.IAPFullVersionComplete", 0x10d346c, FORCE_FLAGS_RET),
    Il2CppGate("GameManager.IAPRemoveAdsComplete", 0x10d31e4, FORCE_FLAGS_RET),
    Il2CppGate("GameManager.IAPComplete", 0x10d32d0, FORCE_FLAGS_RET),
    Il2CppGate("GameManager.IAPUpgradeComplete", 0x10d3584, FORCE_FLAGS_RET),
    Il2CppGate("GameManager.ShowIAPCanvas", 0x10c4ca4, RET),
    Il2CppGate("BalloonPop.OpenIapCanvas", 0x10fafac, RET),
    Il2CppGate("UnlockIAP.DesignIAP", 0x10f9734, RET),
    Il2CppGate("UnlockIAP.Start", 0x10f9730, RET),
    Il2CppGate("IapCanvas.OnEnable", 0x109be20, RET),
    Il2CppGate("IapCanvas.CheckPurchaseStatus", 0x109bf74, RET),
    Il2CppGate("GameManager.ShowInterstitial", 0x10c7808, RET),
    Il2CppGate("GameManager.ShowBanner", 0x10c8238, RET),
    Il2CppGate("GameManager.RequestInterstitial", 0x10c7944, RET),
    Il2CppGate("GameManager.ShowUnityBanner", 0x10d39a8, RET),
    Il2CppGate("GameManager.ShowUnityAd", 0x10d3a48, RET),
    Il2CppGate("GooglePurchase.IsPurchased", 0x2159ee0, RETURN_TRUE),
    Il2CppGate("Product.get_hasReceipt", 0x2125194, RETURN_TRUE),
    Il2CppGate("PurchaserIAP.IsInitialized", 0x10df2ac, RETURN_TRUE),
)

private fun hexBytes(pattern: String): ByteArray =
    pattern.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .map { it.toInt(16).toByte() }.toByteArray()

private val unlockIl2CppNativeInternalPatch = rawResourcePatch {
    execute {
        val logger = Logger.getLogger("puzzleskids.il2cpp")
        val target = get(LIB, true)
        if (!target.exists()) {
            throw PatchException(
                "$LIB missing. Patch the full APKS (base + arm64 split), not base.apk alone.",
            )
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
    description = "Forces GameData isFullVersion/isAdFree, kills paywall/ads, real IAP product ids.",
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
