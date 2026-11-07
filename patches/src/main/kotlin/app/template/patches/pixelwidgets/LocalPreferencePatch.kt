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

/** Exact method: Lfc1.e — returns true when feature is locked. */
private object IsFeatureLockedFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "e",
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
)

/** Exact method: Lyl1.c — maps RevenueCat premium entitlement. */
private object PremiumEntitlementFingerprint : Fingerprint(
    definingClass = "Lyl1;",
    name = "c",
    returnType = "Ll31;",
    parameters = listOf("Lhw;"),
)

/** Exact method: Lfc1.b — updates pro_lock preference. */
private object UpdateProLockFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "b",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

/** Exact method: Lfs0.e — writes premium boolean from RevenueCat. */
private object WritePremiumFingerprint : Fingerprint(
    definingClass = "Lfs0;",
    name = "e",
    returnType = "V",
    parameters = listOf("Lu34;", "Lvy0;"),
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
        IsFeatureLockedFingerprint.method.returnEarly(false)

        // 2. Force isActive = true in the RevenueCat mapper.
        runCatching {
            val method = PremiumEntitlementFingerprint.method
            val instructions = method.implementation?.instructions ?: return@runCatching
            for (i in instructions.indices) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction) {
                    val ref = insn.reference
                    if (ref is FieldReference && ref.name == "m" && ref.type == "Z") {
                        val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue
                        method.addInstructions(i + 1, "const/4 v$reg, 0x1")
                        break
                    }
                }
            }
        }

        // 3. Force getBoolean("premium") → true in the lock-check and update methods.
        listOf(IsFeatureLockedFingerprint, UpdateProLockFingerprint).forEach { fp ->
            runCatching {
                val method = fp.method
                val instructions = method.implementation?.instructions ?: return@runCatching
                for (i in instructions.indices) {
                    val insn = instructions[i]
                    if (insn is ReferenceInstruction &&
                        insn.reference is StringReference &&
                        (insn.reference as StringReference).string == "premium"
                    ) {
                        for (j in (i + 1) until instructions.size) {
                            val next = instructions[j]
                            if (next.opcode.name.startsWith("move-result")) {
                                val reg = (next as? OneRegisterInstruction)?.registerA ?: 0
                                method.addInstructions(j + 1, "const/4 v$reg, 0x1")
                                break
                            }
                        }
                        break
                    }
                }
            }
        }

        // 4. Force the value written by putBoolean("premium", ...) to true.
        runCatching {
            val method = WritePremiumFingerprint.method
            val instructions = method.implementation?.instructions ?: return@runCatching
            for (i in instructions.indices) {
                val insn = instructions[i]
                if (insn is ReferenceInstruction &&
                    insn.reference is StringReference &&
                    (insn.reference as StringReference).string == "premium"
                ) {
                    // The Boolean value is unboxed shortly before putBoolean.
                    // Force a true into v0 and rely on the existing flow, or
                    // more reliably: after the Boolean.booleanValue() move-result, force true.
                    for (j in (i + 1) until minOf(i + 20, instructions.size)) {
                        val next = instructions[j]
                        if (next.opcode.name.startsWith("move-result") && next is OneRegisterInstruction) {
                            val reg = next.registerA
                            method.addInstructions(j + 1, "const/4 v$reg, 0x1")
                            break
                        }
                    }
                    break
                }
            }
        }
    }
}
