package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

private object SignalClinitFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/lib_signal/SignalController;",
    name = "<clinit>",
    returnType = "V",
)

private object SignalErrorFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/lib_signal/SignalController;",
    name = "signalError",
    returnType = "V",
)

private object InitSignalFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/lib_signal/SignalController;",
    name = "initSignal",
    returnType = "V",
)

private object CallNativeExceptionFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/lib_signal/SignalController;",
    name = "callNativeException",
    returnType = "V",
)

private object BuildCrashInfoFingerprint : Fingerprint(
    definingClass = "Lcolorwidgets/ios/widget/topwidgets/lib_signal/SignalController;",
    name = "buildCrashInfo",
    returnType = "V",
)

@Suppress("unused")
val disableIntegrityPatch = bytecodePatch(
    name = "Disable Integrity",
    description = "Skips keep-signal load and integrity error paths so the app can run after patching.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    execute {
        runCatching { SignalClinitFingerprint.method.returnEarly() }
        runCatching { SignalErrorFingerprint.method.returnEarly() }
        runCatching { InitSignalFingerprint.method.returnEarly() }
        runCatching { CallNativeExceptionFingerprint.method.returnEarly() }
        runCatching { BuildCrashInfoFingerprint.method.returnEarly() }
    }
}
