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
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val PIXEL_WIDGETS = Compatibility(
    name = "Pixel Widgets",
    packageName = "com.widgetvibes.pixelwidgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    targets = listOf(
        AppTarget(version = "1.0.16", versionCode = 17, minSdk = 32),
    ),
)

/** Local lock check: false = unlocked. */
private object IsFeatureLockedFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "e",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

/**
 * EntitlementInfo constructor (Ls50).
 * Field m is isActive. Force it true after the original write.
 */
private object EntitlementInfoConstructorFingerprint : Fingerprint(
    definingClass = "Ls50;",
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Ln81;",
        "Ljava/util/Date;",
        "Ljava/util/Date;",
        "Ljava/util/Date;",
        "Lfx1;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/util/Date;",
        "Ljava/util/Date;",
        "Ln31;",
        "Lorg/json/JSONObject;",
        "Lz92;",
    ),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces local premium preference and RevenueCat entitlement so all features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        // 1. Local pro_lock gate
        IsFeatureLockedFingerprint.method.returnEarly(false)

        // 2. Every EntitlementInfo is created with isActive = true
        val ctor = EntitlementInfoConstructorFingerprint.method
        val instructions: List<Instruction> = ctor.implementation!!.instructions
        for (i in instructions.indices) {
            val insn = instructions[i]
            if (insn is ReferenceInstruction) {
                val ref = insn.reference
                if (ref is FieldReference &&
                    ref.definingClass == "Ls50;" &&
                    ref.name == "m" &&
                    ref.type == "Z"
                ) {
                    ctor.addInstructions(
                        i + 1,
                        """
                            const/4 v0, 0x1
                            iput-boolean v0, p0, Ls50;->m:Z
                        """,
                    )
                    break
                }
            }
        }
    }
}
