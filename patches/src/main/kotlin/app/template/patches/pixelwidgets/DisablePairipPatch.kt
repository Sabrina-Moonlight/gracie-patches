package app.template.patches.pixelwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private val PIXEL_WIDGETS = Compatibility(
    name = "Pixel Widgets",
    packageName = "com.widgetvibes.pixelwidgets",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    targets = listOf(
        AppTarget(version = "1.0.16", versionCode = 17, minSdk = 32),
    ),
)

private object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
)

private object AttachBaseContextFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/application/Application;",
    name = "attachBaseContext",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Skips the local license check so the app can keep running offline.",
    default = true,
) {
    compatibleWith(PIXEL_WIDGETS)

    execute {
        InitializeLicenseCheckFingerprint.method.returnEarly()

        // Right after attachBaseContext starts, write premium = true into pro_lock.
        // p1 = Context. We only use v0,v1,v2 — method currently has .locals 0, so we must
        // let the patcher handle register allocation by using the high-level form carefully.
        // Safer: append at the end before return, using the existing context p1.
        runCatching {
            val method = AttachBaseContextFingerprint.method
            // Insert at index 0 (beginning): write preference, then original body runs.
            method.addInstructions(
                0,
                """
                    const-string v0, "pro_lock"
                    const/4 v1, 0x0
                    invoke-virtual {p1, v0, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                    move-result-object v0
                    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v0
                    const-string v1, "premium"
                    const/4 v2, 0x1
                    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v0
                    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
                """,
            )
        }
    }
}
