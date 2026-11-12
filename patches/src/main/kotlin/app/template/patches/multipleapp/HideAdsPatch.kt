package app.template.patches.multipleapp

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.template.patches.shared.misc.pairip.MULTIPLE_APP

/**
 * Only skips the full-screen ad Multiple App shows when you open a clone.
 *
 * Does NOT disable AdMob loaders, so ads inside the cloned apps can still load
 * and show. PairIP is handled by Disable Pairip.
 *
 * How:
 * - h(): always continue into the clone (TB.n) without waiting for / showing an ad
 * - g(): no-op so requestShowInternal never presents an interstitial
 */
@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide Entry Ads",
    description = "Skips the full-screen ad when opening a clone. Ads inside the cloned apps still work.",
    default = true,
) {
    compatibleWith(MULTIPLE_APP)

    dependsOn(disablePairipPatch)

    execute {
        // Open clone immediately — same as the "already showing" branch in original h().
        RequestShowAdWithCallbackFingerprint.method.addInstructions(
            0,
            """
                iget-object v0, p0, Lcom/multipleapp/clonespace/rt;->b:Lcom/multipleapp/clonespace/TB;
                invoke-virtual {v0, p4, p5}, Lcom/multipleapp/clonespace/TB;->n(Lcom/multipleapp/clonespace/Xq;Lcom/multipleapp/clonespace/Aw;)V
                return-void
            """,
        )

        // If anything still calls g() directly, do not present the ad.
        RequestShowAdFingerprint.method.returnEarly()
    }
}
