package app.template.patches.soundassistant

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
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

// Force-update check
private object ForceUpdateCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Force update check: OneUI=", "SAT version : "),
)

// Restricted multi-sound list
private object RestrictedDecisionFingerprint : Fingerprint(
    strings = listOf("restrictedPackageForMultiSound"),
)

@Suppress("unused")
val fixMultiSoundRestrictedPatch = bytecodePatch(
    name = "Fix Multi Sound Restricted",
    description = "Local signature spoof + disable force-update + allow multi-sound for all packages.",
    default = true,
) {
    compatibleWith(SOUND_ASSISTANT)

    execute {
        // 1. Disable force-update that closes the app
        runCatching {
            ForceUpdateCheckFingerprint.method.returnEarly(false)
        }

        // 2. Local signature spoof: force Signature.equals to always return true
        runCatching {
            val providerClass = "Lcom/samsung/systemui/volumestar/provider/VolumeStarContentProvider;"
            val methods = classDefBy(providerClass).methods
            methods.forEach { method ->
                if (method.name != "call") return@forEach
                val impl = method.implementation ?: return@forEach
                val instructions: List<Instruction> = ArrayList(impl.instructions)
                var i = 0
                val n = instructions.size
                while (i < n) {
                    val insn = instructions[i]
                    if (insn.opcode == Opcode.INVOKE_VIRTUAL && insn is ReferenceInstruction) {
                        val ref = insn.reference
                        if (ref is MethodReference &&
                            ref.definingClass == "Landroid/content/pm/Signature;" &&
                            ref.name == "equals" &&
                            ref.returnType == "Z"
                        ) {
                            if (i + 1 < n) {
                                val next = instructions[i + 1]
                                if (next.opcode.name.startsWith("move-result") && next is OneRegisterInstruction) {
                                    val reg = next.registerA
                                    method.addInstructions(
                                        i + 2,
                                        """
                                            const/4 v$reg, 0x1
                                        """,
                                    )
                                }
                            }
                        }
                    }
                    i++
                }
            }
        }

        // 3. Multi-sound: force restricted contains to false + empty the list load
        RestrictedDecisionFingerprint.matchAll().forEach { match ->
            val method = match.method
            val impl = method.implementation ?: return@forEach
            val instructions: List<Instruction> = ArrayList(impl.instructions)
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

            i = 0
            while (i < n) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is StringReference && ref.string == "restrictedPackageForMultiSound") {
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
