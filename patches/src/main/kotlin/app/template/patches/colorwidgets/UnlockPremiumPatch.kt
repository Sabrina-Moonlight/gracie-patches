package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

internal val COLOR_WIDGETS = Compatibility(
    name = "Color Widgets - iWidgets",
    packageName = "colorwidgets.ios.widget.topwidgets",
    apkFileType = ApkFileType.APKS_REQUIRED,
    appIconColor = 0xFF6B9D,
    targets = listOf(
        AppTarget(version = "1.8.5", versionCode = 105, minSdk = 32),
    ),
)

/**
 * Paywall activity. Closing it on create stops the "Unlock All Features" screen.
 */
private object SubscribeOnCreateFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/ui/activity/subscribe3/SubscribeActivity3;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

/**
 * Any method that loads the is_vip preference key.
 * Boolean ones are forced to true (VIP active).
 */
private object IsVipKeyFingerprint : Fingerprint(
    strings = listOf("is_vip"),
)

/**
 * Product / entitlement id used for premium.
 */
private object PremiumProductFingerprint : Fingerprint(
    strings = listOf("color_widgets_premium"),
)

/**
 * Log path when VIP is active (banner ads off).
 * Ensures the VIP branch is taken where this helper is used.
 */
private object UserIsVipFingerprint : Fingerprint(
    strings = listOf("User is VIP, disabling banner ads"),
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Local Premium",
    description = "Skips the paywall and treats is_vip / premium product as active offline.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    execute {
        // Close paywall immediately.
        runCatching {
            SubscribeOnCreateFingerprint.method.addInstructions(
                0,
                """
                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }

        // Force boolean gates that read is_vip.
        runCatching {
            IsVipKeyFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }

        // Force boolean gates that mention the premium product id.
        runCatching {
            PremiumProductFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }

        // Ads helper that branches on VIP.
        runCatching {
            UserIsVipFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                } else if (ret == "V") {
                    // void helpers: skip body so they cannot re-enable ads from a false VIP flag
                    match.method.returnEarly()
                }
            }
        }
    }
}
