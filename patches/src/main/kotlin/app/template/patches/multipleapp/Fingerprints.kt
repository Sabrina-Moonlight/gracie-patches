package app.template.patches.multipleapp

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * rt.c()Z — "is ad ready to show?" (state >= 2).
 * Stable: class holds ad unit + requestShowInternal log string nearby in g().
 */
internal object AdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/rt;",
    name = "c",
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

/**
 * rt.g(View)V — requestShowInternal: actually shows the loaded interstitial / app-open ad.
 * Fingerprint by unique log strings in the method body.
 */
internal object RequestShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/rt;",
    name = "g",
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("requestShowInternal", "moon internal error"),
)

/**
 * rt.h(View, long, Xq, Wn)V — entry used when opening a clone / dialog;
 * may load then show, or continue without ad.
 * Unique error string "ad can not show".
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
 * t2.uncaughtException — logs and calls System.exit(0).
 * That kills the whole process on any uncaught crash (including after a bad patch).
 */
internal object UncaughtExceptionExitFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/t2;",
    name = "uncaughtException",
    returnType = "V",
    parameters = listOf("Ljava/lang/Thread;", "Ljava/lang/Throwable;"),
)

/**
 * Interstitial load entry (sn.d).
 * Belt-and-suspenders: never load interstitial so state never becomes "ready".
 */
internal object InterstitialLoadFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/sn;",
    name = "d",
    returnType = "V",
    parameters = emptyList(),
)

/**
 * App-open ad load entry (un.d).
 */
internal object AppOpenLoadFingerprint : Fingerprint(
    definingClass = "Lcom/multipleapp/clonespace/un;",
    name = "d",
    returnType = "V",
    parameters = emptyList(),
)
