package app.template.patches.shared.misc.pairip

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.formatter.DexFormatter

private const val VM_RUNNER = "Lcom/pairip/VMRunner;"
private const val PAIRIP_PACKAGE = "Lcom/pairip/"

internal fun Method.callsVmRunner() = implementation?.instructions?.any {
    it.getReference<MethodReference>()?.definingClass == VM_RUNNER
} == true

internal fun ClassDef.virtualizedMethods() =
    if (type.startsWith(PAIRIP_PACKAGE)) emptyList() else methods.filter { it.callsVmRunner() }

internal fun methodDescriptor(method: Method): String =
    DexFormatter.INSTANCE.getMethodDescriptor(method)
