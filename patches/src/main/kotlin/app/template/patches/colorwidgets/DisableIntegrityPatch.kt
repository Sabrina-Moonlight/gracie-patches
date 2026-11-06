package app.template.patches.colorwidgets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly

/**
 * keep-signal / SignalController: native integrity that fires after re-sign.
 * Skipping these entry points stops the "File corrupted, please reinstall..." path.
 */
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

@Suppress("unused")
val disableIntegrityPatch = bytecodePatch(
    name = "Disable Integrity",
    description = "Skips the keep-signal integrity check so the app can run after patching.",
    default = true,
) {
    compatibleWith(COLOR_WIDGETS)

    execute {
        runCatching { SignalErrorFingerprint.method.returnEarly() }
        runCatching { InitSignalFingerprint.method.returnEarly() }
        runCatching { CallNativeExceptionFingerprint.method.returnEarly() }
    }
}
