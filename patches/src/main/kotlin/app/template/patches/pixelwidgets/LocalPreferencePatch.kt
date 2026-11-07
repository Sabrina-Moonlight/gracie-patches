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
 * Lfc1.e — decides if a feature is locked.
 * Returns false when the user has premium. We force false.
 */
private object IsFeatureLockedFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "e",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lfc1;", "Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("pro_lock", "premium", "until."),
)

/**
 * Lfc1.c — used by widget providers. Returns the lock state for a feature.
 * Same polarity as e (false = unlocked path in callers after the check).
 */
private object WidgetLockFingerprint : Fingerprint(
    definingClass = "Lfc1;",
    name = "c",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "[I"),
)

/**
 * Lyl1.c — maps RevenueCat CustomerInfo to local premium status.
 * We force the isActive field read to true.
 */
private object PremiumEntitlementFingerprint : Fingerprint(
    definingClass = "Lyl1;",
    name = "c",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ll31;",
    parameters = listOf("Lhw;"),
    strings = listOf("premium", "yearly", "lifetime"),
)

@Suppress("unused")
val localPreferencePatch = bytecodePatch(
    name = "Local Preference",
    description = "Forces local premium preference and entitlement so all features stay available offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        // Force lock checks to unlocked
        IsFeatureLockedFingerprint.method.returnEarly(false)
        runCatching { WidgetLockFingerprint.method.returnEarly(false) }

        // Force isActive = true in RevenueCat mapper
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
    }
}
