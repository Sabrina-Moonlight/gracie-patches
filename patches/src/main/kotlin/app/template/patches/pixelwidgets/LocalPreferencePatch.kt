package app.template.patches.pixelwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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
 * Method e: reads pro_lock / premium and returns whether the feature is locked.
 * Returning false = unlocked path.
 */
private object ProLockCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

/**
 * Method b: updates the pro_lock preference.
 * Contains getBoolean("premium").
 */
private object ProLockUpdateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("pro_lock", "premium"),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces the local premium preference so optional widget features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        // Force the lock-check to return false (unlocked).
        runCatching {
            ProLockCheckFingerprint.method.returnEarly(false)
        }

        // After every getBoolean("premium") force the result register to true.
        listOf(ProLockCheckFingerprint, ProLockUpdateFingerprint).forEach { fp ->
            runCatching {
                val method = fp.method
                val instructions = method.implementation?.instructions ?: return@runCatching
                var i = 0
                val n = instructions.size
                while (i < n) {
                    val insn = instructions[i]
                    if (insn is ReferenceInstruction) {
                        val ref = insn.reference
                        if (ref is StringReference && ref.string == "premium") {
                            var j = i + 1
                            while (j < n) {
                                val next = instructions[j]
                                if (next.opcode.name.startsWith("move-result")) {
                                    val reg = if (next is OneRegisterInstruction) next.registerA else 0
                                    // Force true right after the read.
                                    method.addInstructions(
                                        j + 1,
                                        """
                                            const/4 v0, 0x1
                                            move v$reg, v0
                                        """,
                                    )
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
        }
    }
}
