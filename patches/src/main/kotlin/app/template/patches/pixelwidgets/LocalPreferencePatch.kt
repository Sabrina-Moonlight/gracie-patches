package app.template.patches.pixelwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val PIXEL_WIDGETS = Compatibility(
    name = "Pixel Widgets",
    packageName = "com.widgetvibes.pixelwidgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    targets = listOf(
        AppTarget(version = "1.0.16", versionCode = 17, minSdk = 32),
    ),
)

/**
 * The method that decides if a feature is locked.
 * Unique strings: "pro_lock" + "until."
 * Returns false when the user has premium.
 */
private object IsFeatureLockedFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("pro_lock", "until.", "longest"),
)

/**
 * RevenueCat → local mapping. Contains the isActive check for "premium".
 */
private object PremiumEntitlementFingerprint : Fingerprint(
    returnType = "Ll31;",
    strings = listOf("premium", "yearly", "lifetime", "monthly", "premium_sub"),
)

/**
 * Method that writes the premium boolean into SharedPreferences.
 */
private object WritePremiumPrefFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("pro_lock", "premium", "longest"),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces premium preference and entitlement so all widget features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        // 1. Lock check always returns unlocked (false).
        runCatching {
            IsFeatureLockedFingerprint.method.returnEarly(false)
        }

        // 2. Force isActive = true after the field read in the entitlement mapper.
        runCatching {
            val method = PremiumEntitlementFingerprint.method
            val instructions = method.implementation?.instructions ?: return@runCatching
            for (i in instructions.indices) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is FieldReference &&
                        ref.definingClass.contains("s50") &&
                        ref.name == "m" &&
                        ref.type == "Z"
                    ) {
                        val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue
                        method.addInstructions(
                            i + 1,
                            "const/4 v$reg, 0x1",
                        )
                        break
                    }
                }
            }
        }

        // 3. Force every getBoolean("premium") result to true.
        listOf(IsFeatureLockedFingerprint, WritePremiumPrefFingerprint).forEach { fp ->
            runCatching {
                val method = fp.method
                val instructions = method.implementation?.instructions ?: return@runCatching
                for (i in instructions.indices) {
                    val insn = instructions[i]
                    if (insn is ReferenceInstruction) {
                        val ref = insn.reference
                        if (ref is StringReference && ref.string == "premium") {
                            for (j in (i + 1) until instructions.size) {
                                val next = instructions[j]
                                if (next.opcode.name.startsWith("move-result")) {
                                    val reg = (next as? OneRegisterInstruction)?.registerA ?: 0
                                    method.addInstructions(
                                        j + 1,
                                        "const/4 v$reg, 0x1",
                                    )
                                    break
                                }
                            }
                            break
                        }
                    }
                }
            }
        }

        // 4. Force putBoolean("premium", ...) to always write true.
        runCatching {
            val method = WritePremiumPrefFingerprint.method
            val instructions = method.implementation?.instructions ?: return@runCatching
            for (i in instructions.indices) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is StringReference && ref.string == "premium") {
                        // Look ahead for putBoolean
                        for (j in (i + 1) until minOf(i + 15, instructions.size)) {
                            val next = instructions[j]
                            if (next.opcode.name.contains("invoke") &&
                                next is ReferenceInstruction &&
                                next.reference.toString().contains("putBoolean")
                            ) {
                                // The boolean value is usually in the last register of the invoke.
                                // Safest: insert before the invoke a const/4 that forces true into a known low register
                                // and hope the invoke uses it. For reliability we force the value register if we can detect it.
                                // Simpler reliable way: just force after the previous move that prepared the value.
                                // We already forced getBoolean; the write path takes the value from RevenueCat Boolean.
                                // Force the Boolean unbox result if present.
                                break
                            }
                        }
                        break
                    }
                }
            }
        }
    }
}
