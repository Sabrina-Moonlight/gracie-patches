package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
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
 * Local cache key written when the app believes the user is VIP.
 * Boolean-returning methods that touch this key are forced to true.
 */
private object IsVipKeyFingerprint : Fingerprint(
    strings = listOf("is_vip"),
)

/**
 * Play product / entitlement id for premium.
 */
private object PremiumProductFingerprint : Fingerprint(
    strings = listOf("color_widgets_premium"),
)

/**
 * Path that writes the local VIP cache as true after a successful check.
 */
private object UpdateIsVipCacheFingerprint : Fingerprint(
    strings = listOf("updateIsVipCache true"),
)

/**
 * Server subscription result log: "subscribe result: isVip: …"
 * Used when the app applies the server answer locally.
 */
private object SubscribeResultIsVipFingerprint : Fingerprint(
    strings = listOf("subscribe result: isVip: "),
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Local Premium",
    description = "Forces local VIP cache gates to true. Server subscription check may still limit some features.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    execute {
        // Boolean methods that read is_vip → always true
        runCatching {
            IsVipKeyFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }

        // Boolean methods that mention the premium product id → always true
        runCatching {
            PremiumProductFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }

        // Prefer the "cache = true" write path when it is a gate
        runCatching {
            UpdateIsVipCacheFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }

        // Server result applicator: if it returns boolean, force VIP
        runCatching {
            SubscribeResultIsVipFingerprint.matchAll().forEach { match ->
                val ret = match.method.returnType
                if (ret == "Z" || ret == "Ljava/lang/Boolean;") {
                    match.method.returnEarly(true)
                }
            }
        }
    }
}
