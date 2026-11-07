package app.template.patches.soundassistant

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val SOUND_ASSISTANT = Compatibility(
    name = "Sound Assistant",
    packageName = "com.samsung.android.soundassistant",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = "8.5.00.2")),
)

/**
 * The static list of packages blocked from full multi-sound.
 * We empty it so "for all apps" really ignores focus from ads and restricted packages.
 */
private object RestrictedListClinitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "com.samsung.android.soundassistant",
        "com.sec.android.app.launcher",
        "com.android.systemui",
    ),
)

@Suppress("unused")
val fixMultiSoundRestrictedPatch = bytecodePatch(
    name = "Fix Multi Sound Restricted",
    description = "Empties the restricted package list so multi-sound works for all apps and does not pause on ads.",
    default = true,
) {
    compatibleWith(SOUND_ASSISTANT)

    execute {
        val method = RestrictedListClinitFingerprint.method
        val instructions = method.implementation!!.instructions

        // Find the sput-object that writes the big restricted array to field r
        var sputIndex = -1
        var i = 0
        val n = instructions.size
        while (i < n) {
            val insn = instructions[i]
            if (insn.opcode == Opcode.SPUT_OBJECT && insn is ReferenceInstruction) {
                val ref = insn.reference
                if (ref is FieldReference && ref.name == "r" && ref.type == "[Ljava/lang/String;") {
                    sputIndex = i
                    break
                }
            }
            i++
        }

        if (sputIndex < 0) {
            // Fallback: look for the string that is near the end of the array and take the next sput
            i = 0
            while (i < n) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is StringReference && ref.string == "com.samsung.android.soundassistant") {
                        var j = i + 1
                        while (j < n && j < i + 30) {
                            val next = instructions[j]
                            if (next.opcode == Opcode.SPUT_OBJECT) {
                                sputIndex = j
                                break
                            }
                            j++
                        }
                        break
                    }
                }
                i++
            }
        }

        if (sputIndex < 0) {
            error("Could not find the restricted package list sput in clinit")
        }

        // Just before the sput, force an empty array into the same register that the sput will use.
        // Most of these sput use v0 after move-result-object v0.
        // We insert a clean empty array so the previous filled-new-array is ignored.
        method.addInstructions(
            sputIndex,
            """
                const/4 v0, 0x0
                new-array v0, v0, [Ljava/lang/String;
            """,
        )
    }
}
