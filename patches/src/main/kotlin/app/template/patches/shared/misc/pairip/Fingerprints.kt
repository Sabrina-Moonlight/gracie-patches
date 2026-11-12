/*
 * Copyright (C) 2026 hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * VMRunner fingerprints ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/d3c02d333b9477ec50039093fc3b3641e3f1cbd0
 */
package app.template.patches.shared.misc.pairip

import app.morphe.patcher.Fingerprint

internal const val LICENSE_CLIENT_CLASS = "Lcom/pairip/licensecheck/LicenseClient;"

internal object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = emptyList(),
)

/** Entry called from pairip Application.attachBaseContext. */
internal object CheckLicenseFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object ProcessResponseFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "processResponse",
    returnType = "V",
    parameters = listOf("I", "Landroid/os/Bundle;"),
)

internal object HandleErrorFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "handleError",
    returnType = "V",
    parameters = listOf("Lcom/pairip/licensecheck/LicenseCheckException;"),
)

internal object StartPaywallActivityFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "startPaywallActivity",
    returnType = "V",
    parameters = listOf("Landroid/app/PendingIntent;"),
)

internal object VMRunnerStaticInitializerFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "<clinit>",
)

internal object VMRunnerInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "invoke",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
)
