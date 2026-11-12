package app.template.patches.multipleapp

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.misc.pairip.MULTIPLE_APP
import app.template.patches.shared.misc.pairip.removePairipProtectionPatch

/**
 * Multiple App only ships PairIP LicenseClient (no VMRunner / KeyImport).
 * Disabling the license check stops the app from closing after re-signing.
 *
 * Also enable "Spoof app signature" from Doom (or similar) if the app still
 * checks its own certificate after patching.
 */
@Suppress("unused")
val disablePairipPatch = bytecodePatch(
    name = "Disable Pairip",
    description = "Disables PairIP license checks so the app keeps running after it is modified.",
    default = true,
) {
    compatibleWith(MULTIPLE_APP)

    dependsOn(removePairipProtectionPatch)

    execute {
        // Work is done in removePairipProtectionPatch.
    }
}
