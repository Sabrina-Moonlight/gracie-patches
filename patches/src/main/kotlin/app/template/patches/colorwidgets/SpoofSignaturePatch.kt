package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SPOOF_CLASS = "Lapp/template/extension/shared/SpoofSignature;"
private const val ANDROID_APPLICATION = "Landroid/app/Application;"
private const val APPLICATION_CLASS = "Lcolorwidgets/ios/widget/topwidgets/BaseApp;"

private const val PACKAGE_NAME = "colorwidgets.ios.widget.topwidgets"
private const val CERTIFICATE =
    "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" +
    "Gnktl3NGxD0HvUipKwCxfPAOLw+gvAA3MAn4UPU="

private object SpoofClinitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == SPOOF_CLASS &&
            method.implementation?.instructions
                ?.any {
                    val ref = (it as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)
                        ?.reference
                    (ref as? com.android.tools.smali.dexlib2.iface.reference.StringReference)
                        ?.string == "<package-name>"
                } == true
    },
)

private fun MutableMethod.replaceString(placeholder: String, value: String) {
    val index = indexOfFirstStringInstructionOrThrow(placeholder)
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    replaceInstruction(index, "const-string v, """)
}

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof Signature",
    description = "Makes the app see the original Play Store signature after patching.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    extendWith("extensions/extension.mpe")

    execute {
        // Seed package name + original cert into the extension static init.
        val clinit = SpoofClinitFingerprint.method
        clinit.replaceString("<package-name>", PACKAGE_NAME)
        clinit.replaceString("<certificate>", CERTIFICATE)

        // BaseApp (or the class just above Application) must extend SpoofSignature.
        var classDef = mutableClassDefBy(APPLICATION_CLASS)
        val walked = mutableSetOf(classDef.type)
        while (classDef.superclass != ANDROID_APPLICATION) {
            val superclass = classDef.superclass
                ?: error("Application hierarchy ended before android.app.Application")
            if (!walked.add(superclass)) {
                error("Application hierarchy loops at ")
            }
            classDef = mutableClassDefBy(superclass)
        }
        classDef.setSuperClass(SPOOF_CLASS)
    }
}
