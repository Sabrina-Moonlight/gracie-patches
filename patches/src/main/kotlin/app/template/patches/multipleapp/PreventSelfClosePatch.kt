package app.template.patches.multipleapp

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.misc.pairip.MULTIPLE_APP
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Extra safety: t2.uncaughtException calls System.exit(0).
 * PairIP disable is the main fix; this stops process kills from stray crashes.
 */
@Suppress("unused")
val preventSelfClosePatch = bytecodePatch(
    name = "Prevent Self Close",
    description = "Stops the app from force-closing the process on errors after it is modified.",
    default = true,
) {
    compatibleWith(MULTIPLE_APP)

    dependsOn(disablePairipPatch)

    execute {
        val method = UncaughtExceptionExitFingerprint.method
        val instructions = method.implementation?.instructions ?: return@execute

        val exitIndexes = instructions.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@mapIndexedNotNull null
            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@mapIndexedNotNull null
            if (ref.definingClass == "Ljava/lang/System;" && ref.name == "exit") index else null
        }

        exitIndexes.forEach { index ->
            method.replaceInstruction(index, "nop")
        }
    }
}
