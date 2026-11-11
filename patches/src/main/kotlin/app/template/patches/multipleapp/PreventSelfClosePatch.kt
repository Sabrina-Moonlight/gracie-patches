package app.template.patches.multipleapp

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val MULTIPLE_APP = Compatibility(
    name = "Multiple App",
    packageName = "com.multipleapp.clonespace",
    apkFileType = ApkFileType.APKS,
    appIconColor = 0x4CAF50,
    targets = listOf(
        AppTarget(version = "1.0.9.260.20", versionCode = 9),
    ),
)

/**
 * After re-signing / patching, any uncaught exception in this app calls
 * System.exit(0) from t2.uncaughtException and kills the process (looks like
 * "the app closed itself").
 *
 * We nop the System.exit call so a crash only ends that thread instead of
 * killing the whole process.
 */
@Suppress("unused")
val preventSelfClosePatch = bytecodePatch(
    name = "Prevent Self Close",
    description = "Stops the app from force-closing the process on errors after it is modified.",
    default = true,
) {
    compatibleWith(MULTIPLE_APP)

    execute {
        val method = UncaughtExceptionExitFingerprint.method
        val instructions = method.implementation?.instructions ?: return@execute

        // Collect indices first — avoid concurrent modification while iterating.
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
