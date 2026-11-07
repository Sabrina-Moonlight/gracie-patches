package app.template.patches.shared.misc.signature

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.Base64

internal const val SPOOF_SIGNATURE_CLASS = "Lapp/template/extension/shared/SpoofSignature;"

private const val ANDROID_APPLICATION_CLASS = "Landroid/app/Application;"
private const val PACKAGE_NAME_PLACEHOLDER = "<package-name>"
private const val CERTIFICATE_PLACEHOLDER = "<certificate>"

/**
 * Injects the original signing certificate into SpoofSignature and
 * makes the given Application class extend it.
 *
 * If the extension class or placeholders are missing, skips the spoof
 * instead of failing the whole patch (multi-sound can still apply).
 */
internal fun BytecodePatchContext.spoofSignature(applicationClass: String) {
    val classDef = try {
        mutableClassDefBy(SPOOF_SIGNATURE_CLASS)
    } catch (e: Exception) {
        // Extension not merged – skip spoof
        return
    }

    val certMap = packageMetadata.signingCertificates
    if (certMap.isNullOrEmpty()) {
        return
    }
    val certificate = certMap.values
        .asSequence()
        .flatten()
        .firstOrNull()
        ?: return

    val certBase64 = Base64.getEncoder().encodeToString(certificate.encoded)
    val packageName = packageMetadata.packageName

    var replacedPackage = false
    var replacedCert = false

    // Search ALL methods (clinit, constructors, etc.) for the placeholders
    for (method in classDef.methods) {
        val impl = method.implementation ?: continue
        var i = 0
        for (insn in impl.instructions) {
            if (insn is ReferenceInstruction) {
                val ref = insn.reference
                if (ref is StringReference) {
                    when (ref.string) {
                        PACKAGE_NAME_PLACEHOLDER -> {
                            val reg = if (insn is OneRegisterInstruction) insn.registerA else 0
                            val escaped = packageName.replace("\\", "\\\\").replace("\"", "\\\"")
                            method.replaceInstruction(i, "const-string v$reg, \"$escaped\"")
                            replacedPackage = true
                        }
                        CERTIFICATE_PLACEHOLDER -> {
                            val reg = if (insn is OneRegisterInstruction) insn.registerA else 0
                            val escaped = certBase64.replace("\\", "\\\\").replace("\"", "\\\"")
                            method.replaceInstruction(i, "const-string v$reg, \"$escaped\"")
                            replacedCert = true
                        }
                    }
                }
            }
            i++
        }
    }

    // Only change Application superclass if both placeholders were found
    if (replacedPackage && replacedCert) {
        applicationRoot(applicationClass).setSuperClass(SPOOF_SIGNATURE_CLASS)
    }
}

private fun BytecodePatchContext.applicationRoot(applicationClass: String): MutableClass {
    var classDef = mutableClassDefBy(applicationClass)
    val walked = mutableSetOf(classDef.type)

    while (classDef.superclass != ANDROID_APPLICATION_CLASS) {
        val superclass = classDef.superclass
            ?: throw PatchException("Application hierarchy ended before android.app.Application")
        if (!walked.add(superclass)) {
            throw PatchException("Application hierarchy loops at $superclass")
        }
        classDef = mutableClassDefBy(superclass)
    }

    if (classDef.type == SPOOF_SIGNATURE_CLASS) {
        throw PatchException("Application hierarchy already extends SpoofSignature")
    }

    return classDef
}
