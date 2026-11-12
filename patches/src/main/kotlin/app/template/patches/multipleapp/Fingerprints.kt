package app.template.patches.multipleapp

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * rt.g(View)V — shows the loaded interstitial / app-open (requestShowInternal).
 * Only the SHOW path for the host ad gate — does not touch AdMob loaders.
 */
internal object RequestShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/rt;",
    name = "g",
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("requestShowInternal", "moon internal error"),
)

/**
 * rt.h(View, long, Xq, Wn)V — used when opening a clone / dialog.
 * Must still call TB.n so the clone opens; we only skip the ad show.
 */
internal object RequestShowAdWithCallbackFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/rt;",
    name = "h",
    returnType = "V",
    parameters = listOf(
        "Landroid/view/View;",
        "J",
        "Lcom/multipleapp/clonespace/Xq;",
        "Lcom/multipleapp/clonespace/Wn;",
    ),
    strings = listOf("ad can not show"),
)

/**
 * t2.uncaughtException — System.exit(0) on crashes.
 */
internal object UncaughtExceptionExitFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/t2;",
    name = "uncaughtException",
    returnType = "V",
    parameters = listOf("Ljava/lang/Thread;", "Ljava/lang/Throwable;"),
)
