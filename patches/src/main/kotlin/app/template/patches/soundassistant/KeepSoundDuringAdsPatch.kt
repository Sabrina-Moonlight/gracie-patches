package app.template.patches.soundassistant

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.template.patches.shared.cert.extractApkCertificatePatch
import app.template.patches.shared.firebase.spoofFirebaseCertHashPatch
import app.template.patches.shared.installer.spoofInstallSourcePatch
import app.template.patches.shared.signature.spoofSignatureVerificationPatch

private val SOUND_ASSISTANT = Compatibility(
    name = "Sound Assistant",
    packageName = "com.samsung.android.soundassistant",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x2196F3,
    targets = listOf(
        AppTarget(version = "8.5.00.2"),
    ),
)

/**
 * n3.k.H(Context) — force-update gate.
 * Real string in dex: "Force update check: OneUI="
 * When true, MainActivity shows dialog and calls finish().
 */
private object ForceUpdateCheckFingerprint : Fingerprint(
    definingClass = "Ln3/k;",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Force update check: OneUI="),
)

/**
 * n3.k.n(Context) — is multi_audio_focus_enabled.
 * Only the getter has a single Context param and this string.
 */
private object IsMultiAudioFocusEnabledFingerprint : Fingerprint(
    definingClass = "Ln3/k;",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("multi_audio_focus_enabled"),
)

/**
 * BootReceiver.a(Context) — restores settings at boot.
 */
private object BootReceiverInitFingerprint : Fingerprint(
    definingClass = "Lcom/samsung/android/soundassistant/receivers/BootReceiver;",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf(
        "SAT_BootReceiver",
        "soundassistant_ignore_audio_focus_enabled",
    ),
)

@Suppress("unused")
val keepSoundDuringAdsPatch = bytecodePatch(
    name = "Keep sound during ads",
    description = "Prevents media from pausing when ads play. Also blocks the force-update close.",
    default = true,
) {
    compatibleWith(SOUND_ASSISTANT)

    dependsOn(
        extractApkCertificatePatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
        spoofSignatureVerificationPatch,
    )

    execute {
        // Stop close-on-open (force-update dialog → finish).
        ForceUpdateCheckFingerprint.method.returnEarly(false)

        // Always report multi audio focus as enabled.
        IsMultiAudioFocusEnabledFingerprint.method.returnEarly(true)

        // At boot force multi audio focus on (third arg true skips write-settings prompt).
        BootReceiverInitFingerprint.method.addInstructions(
            0,
            """
            const/4 v0, 0x1
            const/4 v1, 0x1
            invoke-static {p0, v0, v1}, Ln3/k;->F(Landroid/content/Context;ZZ)Z
            """.trimIndent(),
        )
    }
}
