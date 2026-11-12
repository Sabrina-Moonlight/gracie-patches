package app.template.patches.kidsdoctorgames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.returnEarly
import app.template.patches.shared.byteArrayOf
import java.io.RandomAccessFile
import java.util.logging.Logger

/**
 * Kids doctor games 1.12 (versionCode 466) — full BB Pass unlock.
 *
 * Single visible patch. Internally:
 * 1. Hex-patches Unity IL2CPP gates in lib/arm64-v8a/libil2cpp.so
 * 2. Forces SharedSub / Billing Java methods
 * 3. Activates local SharedSub on Activity onCreate + onResume
 *
 * Open the full APKS (not only base.apk) so the arm64 lib is present.
 */
internal val KIDS_DOCTOR_GAMES = Compatibility(
    name = "Kids doctor games",
    packageName = "com.bimiboo.kids.doctor.games",
    apkFileType = ApkFileType.APKS,
    appIconColor = 0x2196F3,
    targets = listOf(
        AppTarget(version = "1.12", versionCode = 466, minSdk = 32),
    ),
)

private const val HELPER =
    "Lapp/template/extension/shared/BbPassUnlockHelper;"

private const val LIB = "lib/arm64-v8a/libil2cpp.so"

/** ARM64: mov w0, #1 ; ret */
private val RETURN_TRUE = byteArrayOf("20 00 80 52 c0 03 5f d6")

/**
 * ARM64: mov w0, #3 ; ret  (ResolveAccessState enum — full/pass-style access)
 * Values seen in method body: 1 and 3; 3 is the higher grant path.
 */
private val RETURN_ACCESS_FULL = byteArrayOf("60 00 80 52 c0 03 5f d6")

private data class NativeGate(
    val name: String,
    val pattern: ByteArray,
    val replacement: ByteArray = RETURN_TRUE,
)

/**
 * Unique prologues verified against this APK's libil2cpp.so (metadata v39).
 * Offsets are file offsets in the SO (not RVAs).
 */
private val IL2CPP_GATES = listOf(
    // PassEntitlement.HasThisAppPurchase @ 0x2AF7DEC
    NativeGate(
        "HasThisAppPurchase",
        byteArrayOf("fe 0f 1e f8 f4 4f 01 a9 d4 92 01 d0 53 79 01 b0"),
    ),
    // PassEntitlement.IsOwnedSharedActive @ 0x2B006A8
    NativeGate(
        "IsOwnedSharedActive",
        byteArrayOf("fe 0f 1e f8 f4 4f 01 a9 94 92 01 b0 f3 03 00 aa"),
    ),
    // PassEntitlement.IsActive @ 0x2AFF2B8
    NativeGate(
        "IsActive",
        byteArrayOf("ff c3 00 d1 fe 57 01 a9 f4 4f 02 a9 94 92 01 d0"),
    ),
    // SubscriptionStatusService.get_HasNonServerAccessGrant @ 0x2BC2D20
    NativeGate(
        "get_HasNonServerAccessGrant",
        byteArrayOf("08 a0 40 39 68 00 00 34 20 00 80 52 c0 03 5f d6"),
    ),
    // SubscriptionStatusService.get_HasNonServerPassGrant @ 0x2BC2D44
    NativeGate(
        "get_HasNonServerPassGrant",
        byteArrayOf("08 a0 40 39 68 00 00 35 08 e0 40 39 68 00 00 34"),
    ),
    // BbiapSessionService.get_IsFullAccess @ 0x2CC49C4
    NativeGate(
        "get_IsFullAccess",
        byteArrayOf("00 20 40 f9 01 00 00 14 fe 57 be a9 f4 4f 01 a9"),
    ),
    // PassEntitlement.ResolveAccessState @ 0x2B004D4 → return enum 3
    NativeGate(
        "ResolveAccessState",
        byteArrayOf("fe 0f 1f f8 78 fb ff 97 c0 00 00 36 0b 00 00 94"),
        RETURN_ACCESS_FULL,
    ),
)

/**
 * Private resource patch — not listed alone in Morphe UI.
 * Runs automatically because [unlockSubscriptionPatch] depends on it.
 */
private val unlockIl2CppNativeInternalPatch = rawResourcePatch {
    execute {
        val logger = Logger.getLogger("kidsdoctorgames.il2cpp")
        val target = get(LIB, true)
        if (!target.exists()) {
            throw PatchException(
                "Native library not found: $LIB. Open the full APKS (with arm64 split), not only base.apk.",
            )
        }

        RandomAccessFile(target, "rw").use { raf ->
            for (gate in IL2CPP_GATES) {
                val offset = indexOfPattern(raf, gate.pattern)
                if (offset < 0L) {
                    throw PatchException(
                        "IL2CPP pattern not found for ${gate.name}: " +
                            gate.pattern.joinToString(" ") { "%02x".format(it) },
                    )
                }
                val padded = gate.replacement +
                    ByteArray((gate.pattern.size - gate.replacement.size).coerceAtLeast(0))
                raf.seek(offset)
                raf.write(padded)
                logger.info("IL2CPP ${gate.name} patched @ 0x${offset.toString(16)}")
            }
        }
        logger.info("IL2CPP: ${IL2CPP_GATES.size} native gates applied")
    }
}

@Suppress("unused")
val unlockSubscriptionPatch = bytecodePatch(
    name = "Unlock subscription",
    description = "Unlocks BB Pass: forces Unity IL2CPP access gates in libil2cpp.so, " +
        "SharedSub/Billing Java methods, and activates local pass state on start. " +
        "Requires the full APKS (arm64).",
    default = true,
) {
    compatibleWith(KIDS_DOCTOR_GAMES)
    dependsOn(unlockIl2CppNativeInternalPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // --- SharedSub boolean gates ---
        HasTrustedActiveSharedSubscriptionFingerprint.method.returnEarly(true)
        QueryPackageFingerprint.method.returnEarly(true)
        SharedSubStoreIsActiveFingerprint.method.returnEarly(true)
        SharedSubTrustPolicyIsTrustedFingerprint.method.returnEarly(true)
        runCatching { BbIdCrossAppTrustIsTrustedFingerprint.method.returnEarly(true) }

        // --- Expiry far future ---
        forceFarExpiry(TrustedActiveSharedSubscriptionExpiryFingerprint.method)
        forceFarExpiry(QueryPackageExpiryFingerprint.method)
        forceFarExpiry(SharedSubStoreExpiresAtFingerprint.method)

        // --- Play Billing ---
        PurchaseGetPurchaseStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        PurchaseIsAcknowledgedFingerprint.method.returnEarly(true)

        // --- Activate SharedSub early and again on resume (Unity may overwrite) ---
        UnityPlayerActivityOnCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $HELPER->activate(Landroid/content/Context;)V
            """.trimIndent(),
        )
        runCatching {
            UnityPlayerActivityOnResumeFingerprint.method.addInstructions(
                0,
                """
                    invoke-static {p0}, $HELPER->activate(Landroid/content/Context;)V
                """.trimIndent(),
            )
        }

        logger.info("Kids doctor games: Java SharedSub + Billing + activate applied (native via dependsOn)")
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

private fun indexOfPattern(file: RandomAccessFile, needle: ByteArray): Long {
    val right = IntArray(256) { -1 }
    for ((i, element) in needle.withIndex()) {
        right[element.toInt() and 0xFF] = i
    }

    val bufferSize = 65536
    val buffer = ByteArray(bufferSize + needle.size)
    var fileOffset = 0L
    val fileLength = file.length()

    while (fileOffset < fileLength) {
        file.seek(fileOffset)
        val bytesRead = file.read(buffer)
        if (bytesRead < needle.size) break

        var i = 0
        while (i <= bytesRead - needle.size) {
            var j = needle.size - 1
            while (j >= 0 && buffer[i + j] == needle[j]) j--
            if (j < 0) return fileOffset + i
            val shift = j - right[buffer[i + j].toInt() and 0xFF]
            i += maxOf(1, shift)
        }
        fileOffset += (bytesRead - needle.size + 1).toLong()
    }
    return -1L
}
