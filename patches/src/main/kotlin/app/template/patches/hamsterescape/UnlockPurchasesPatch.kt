package app.template.patches.hamsterescape

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
 * Build 5.2.0 split APKs: base (assets/dex) + config.arm64_v8a (libil2cpp.so).
 * IL2CPP offsets from Il2CppDumper on this exact SO + metadata v31.
 */
private val HAMSTER_ESCAPE = Compatibility(
    name = "Hamster Escape",
    packageName = "gg.sunday.hamsterescape",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xFF8C42,
    targets = listOf(AppTarget(version = null)),
)

private const val LIB = "lib/arm64-v8a/libil2cpp.so"
private const val EXTENSION_CLASS =
    "Lapp/template/extension/shared/HamsterPlayPurchases;"

/** ARM64: mov w0, #1 ; ret */
private val RETURN_TRUE = hexBytes("20 00 80 52 c0 03 5f d6")

private data class Il2CppGate(val label: String, val offset: Long)

private val IL2CPP_GATES = listOf(
    Il2CppGate("get_NoAdsUnlocked", 0x22E1678),
    Il2CppGate("IsNoAdsUnlocked", 0x22E1874),
    Il2CppGate("HasAnyNoAdsOfferBought", 0x22E1CCC),
    Il2CppGate("HasPurchasedNoAds", 0x3DAB508),
    Il2CppGate("IsProductPurchased", 0x3D5EA34),
    Il2CppGate("IsProductPurchasedRaw", 0x3D5F04C),
)

private fun hexBytes(pattern: String): ByteArray =
    pattern.trim().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()

private val unlockIl2CppNativeInternalPatch = rawResourcePatch {
    execute {
        val logger = Logger.getLogger("hamsterescape.il2cpp")
        val target = get(LIB, true)
        if (!target.exists()) {
            logger.warning("$LIB not in this APK — skipping native gates (patch the arm64 split too)")
            return@execute
        }
        RandomAccessFile(target, "rw").use { raf ->
            for (gate in IL2CPP_GATES) {
                if (gate.offset + RETURN_TRUE.size > raf.length()) {
                    throw PatchException("${gate.label}: offset ${gate.offset} past EOF")
                }
                raf.seek(gate.offset)
                raf.write(RETURN_TRUE)
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

private object PurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

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
    description = "Unlocks No Ads and store offers via IL2CPP gates plus synthetic Play purchases.",
    default = true,
) {
    compatibleWith(HAMSTER_ESCAPE)

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
                invoke-static { p2 }, $EXTENSION_CLASS->withSyntheticPurchases(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """.trimIndent(),
        )
        PurchaseStateFingerprint.method.returnEarly(1)
        PurchaseAcknowledgedFingerprint.method.returnEarly(true)
    }
}
