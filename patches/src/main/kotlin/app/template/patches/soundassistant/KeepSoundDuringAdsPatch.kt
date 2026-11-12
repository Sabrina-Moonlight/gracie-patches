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
 * Compares OneUI version vs app versionName. If app is "older", MainActivity
 * shows a non-cancelable dialog and calls finish(). Return false always.
 */
private object ForceUpdateCheckFingerprint : Fingerprint(
    definingClass = "Ln3/k;",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf(
        "Force update check:",
        "SAT version : ",
    ),
)

/**
 * n3.k.n(Context) — reads Settings.System "multi_audio_focus_enabled".
 * Always return true so dual-app / multi audio focus is considered on.
 */
private object IsMultiAudioFocusEnabledFingerprint : Fingerprint(
    definingClass = "Ln3/k;",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("multi_audio_focus_enabled"),
)

/**
 * BootReceiver.a(Context) restores settings at boot.
 * We also force multi audio focus on via the real enable method.
 */
private object BootReceiverInitFingerprint : Fingerprint(
    definingClass = "Lcom/samsung/android/soundassistant/receivers/BootReceiver;",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf(
        "soundassistant_ignore_audio_focus_enabled",
        "soundassistant_ignore_audio_focus_uid",
        "SAT_BootReceiver",
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
        // Stop the app from closing on open (force-update dialog → finish).
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
