/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.template.patches.shared.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

/**
 * Skips PairIP license checks and the paths that close the app on failure.
 * Safe when only LicenseClient is present (no VMRunner / KeyImport).
 */
val removePairipProtectionPatch = bytecodePatch(
    name = "Remove Pairip Protection",
    description = "Skips the local license check entry point and closes that would kill the app.",
    default = true,
) {
    compatibleWith(GLASS_WIDGETS, MULTIPLE_APP)

    execute {
        if (classDefByOrNull(LICENSE_CLIENT_CLASS) == null) return@execute

        // attachBaseContext → checkLicense → initializeLicenseCheck
        CheckLicenseFingerprint.method.returnEarly()
        InitializeLicenseCheckFingerprint.method.returnEarly()

        // Failure paths that open paywall / exit
        runCatching { ProcessResponseFingerprint.method.returnEarly() }
        runCatching { HandleErrorFingerprint.method.returnEarly() }
        runCatching { StartPaywallActivityFingerprint.method.returnEarly() }
    }
}
