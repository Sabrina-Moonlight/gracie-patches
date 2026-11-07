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
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val SOUND_ASSISTANT = Compatibility(
    name = "Sound Assistant",
    packageName = "com.samsung.android.soundassistant",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4CAF50,
    targets = listOf(AppTarget(version = "8.5.00.2")),
)

/**
 * Methods that load the restricted multi-sound package list.
 * We overwrite the array with an empty one right after the sget
 * so the rest of the method sees no restricted packages.
 * Safer than touching <clinit> (that caused the crash).
 */
private object RestrictedListFingerprint : Fingerprint(
    strings = listOf("restrictedPackageForMultiSound"),
)

@Suppress("unused")
val fixMultiSoundRestrictedPatch = bytecodePatch(
    name = "Fix Multi Sound Restricted",
    description = "Empties the restricted package list at every use so multi-sound works for all apps and does not pause on ads. Does not touch class init.",
    default = true,
) {
    compatibleWith(SOUND_ASSISTANT)

    execute {
        RestrictedListFingerprint.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions ?: return@forEach
            var i = 0
            val n = instructions.size
            while (i < n) {
                val insn = instructions[i]
                if (insn.opcode == Opcode.SGET_OBJECT && insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is FieldReference &&
                        ref.name == "r" &&
                        ref.type == "[Ljava/lang/String;"
                    ) {
                        val reg = if (insn is OneRegisterInstruction) insn.registerA else 0
                        method.addInstructions(
                            i + 1,
                            """
                                const/4 v$reg, 0x0
                                new-array v$reg, v$reg, [Ljava/lang/String;
                            """,
                        )
                        break
                    }
                }
                i++
            }
        }
    }
}
