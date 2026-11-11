package app.template.patches.multipleapp

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

private val MULTIPLE_APP = Compatibility(
    name = "Multiple App",
    packageName = "com.multipleapp.clonespace",
    apkFileType = ApkFileType.APKS,
    appIconColor = 0x4CAF50,
    targets = listOf(
        AppTarget(version = "1.0.9.260.20", versionCode = 9),
    ),
)

/**
 * Ads when entering a clone go through rt (AdMob interstitial / app-open).
 *
 * Flow:
 *   h() / k4 → c() "is ready?" → g() requestShowInternal → e() real show
 *
 * We:
 * 1. Force c() = false so the app treats the ad as not ready.
 * 2. No-op g() so nothing is ever shown even if state is wrong.
 * 3. No-op interstitial + app-open load so nothing becomes ready later.
 *
 * When not ready, h() already continues into the clone via TB.n without blocking.
 */
@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide Ads",
    description = "Removes interstitial and app-open ads that appear when opening a clone.",
    default = true,
) {
    compatibleWith(MULTIPLE_APP)

    execute {
        // Never "ready" → h() skips show and continues into the clone.
        AdReadyFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // Hard block the actual show path (requestShowInternal).
        RequestShowAdFingerprint.method.returnEarly()

        // Do not load interstitial / app-open units.
        InterstitialLoadFingerprint.method.returnEarly()
        AppOpenLoadFingerprint.method.returnEarly()
    }
}
