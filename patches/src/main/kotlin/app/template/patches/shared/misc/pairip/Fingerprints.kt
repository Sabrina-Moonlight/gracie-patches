package app.template.patches.shared.misc.pairip

import app.morphe.patcher.Fingerprint

internal const val LICENSE_CLIENT_CLASS = "Lcom/pairip/licensecheck/LicenseClient;"

internal object InitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT_CLASS,
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = emptyList(),
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
