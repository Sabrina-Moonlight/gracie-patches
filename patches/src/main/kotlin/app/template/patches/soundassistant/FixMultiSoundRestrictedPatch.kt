package app.template.patches.soundassistant

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val SOUND_ASSISTANT = Compatibility(
    name = "Sound Assistant",
    packageName = "com.samsung.android.soundassistant",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = "8.5.00.2")),
)

/**
 * Method that decides if a package is restricted for multi-sound.
 * 1) Forces every List.contains() result to false (not restricted).
 * 2) Overwrites the loaded restricted array with an empty one so other paths also see no restrictions.
 * Does not touch <clinit>. Minimal and safe.
 */
private object RestrictedDecisionFingerprint : Fingerprint(
    strings = listOf("restrictedPackageForMultiSound"),
)

@Suppress("unused")
val fixMultiSoundRestrictedPatch = bytecodePatch(
    name = "Fix Multi Sound Restricted",
    description = "Forces multi-sound restricted-package checks to always allow. Ads and restricted packages no longer pause audio when multi-sound is set for all apps.",
    default = true,
) {
    compatibleWith(SOUND_ASSISTANT)

    execute {
        RestrictedDecisionFingerprint.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions ?: return@forEach

            // Pass 1: force every List.contains result to false
            var i = 0
            val n = instructions.size
            while (i < n) {
                val insn = instructions[i]
                if (insn.opcode == Opcode.INVOKE_INTERFACE && insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is MethodReference &&
                        ref.name == "contains" &&
                        ref.definingClass == "Ljava/util/List;" &&
                        ref.returnType == "Z"
                    ) {
                        if (i + 1 < n) {
                            val next = instructions[i + 1]
                            if (next.opcode.name.startsWith("move-result") && next is OneRegisterInstruction) {
                                val reg = next.registerA
                                method.addInstructions(
                                    i + 2,
                                    """
                                        const/4 v$reg, 0x0
                                    """,
                                )
                            }
                        }
                    }
                }
                i++
            }

            // Pass 2: find the const-string "restrictedPackageForMultiSound"
            // and the sget just before it, then overwrite that register with empty array
            i = 0
            while (i < n) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is StringReference && ref.string == "restrictedPackageForMultiSound") {
                        // Look a few instructions back for the sget-object of the array
                        var j = i - 1
                        while (j >= 0 && j >= i - 8) {
                            val prev = instructions[j]
                            if (prev.opcode == Opcode.SGET_OBJECT && prev is OneRegisterInstruction) {
                                val reg = prev.registerA
                                method.addInstructions(
                                    j + 1,
                                    """
                                        const/4 v$reg, 0x0
                                        new-array v$reg, v$reg, [Ljava/lang/String;
                                    """,
                                )
                                break
                            }
                            j--
                        }
                        break
                    }
                }
                i++
            }
        }
    }
}
