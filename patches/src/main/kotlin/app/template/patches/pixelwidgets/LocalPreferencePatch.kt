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

/** Method e: returns whether the feature is locked. false = unlocked. */
private object ProLockCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

/** Method that maps RevenueCat CustomerInfo to local premium status. */
private object RevenueCatPremiumMapFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ll31;",
    parameters = listOf("Lhw;"),
    strings = listOf("premium", "yearly", "lifetime", "monthly"),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces local premium preference and RevenueCat entitlement so features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        // 1. Lock-check always returns unlocked (false).
        runCatching {
            ProLockCheckFingerprint.method.returnEarly(false)
        }

        // 2. Force isActive = true right after the field read in the RevenueCat map method.
        runCatching {
            val method = RevenueCatPremiumMapFingerprint.method
            val instructions = method.implementation?.instructions ?: return@runCatching
            var i = 0
            val n = instructions.size
            while (i < n) {
                val insn = instructions[i]
                // Look for iget-boolean of the isActive field (s50.m)
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is FieldReference &&
                        ref.definingClass == "Ls50;" &&
                        ref.name == "m" &&
                        ref.type == "Z"
                    ) {
                        val reg = if (insn is OneRegisterInstruction) insn.registerA else 0
                        // Force true after the read
                        method.addInstructions(
                            i + 1,
                            """
                                const/4 v$reg, 0x1
                            """,
                        )
                        break
                    }
                }
                i++
            }
        }

        // 3. After every getBoolean("premium") force the result to true.
        runCatching {
            val method = ProLockCheckFingerprint.method
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
                                method.addInstructions(
                                    j + 1,
                                    """
                                        const/4 v$reg, 0x1
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
