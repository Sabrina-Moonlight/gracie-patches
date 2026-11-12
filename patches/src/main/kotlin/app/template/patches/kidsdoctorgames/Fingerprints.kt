package app.template.patches.kidsdoctorgames

import app.morphe.patcher.Fingerprint

/**
 * BB Pass shared subscription gates (com.bimiboo.pass).
 * Classes are not obfuscated in 1.12.
 */

/** SharedSubClient.hasTrustedActiveSharedSubscription(Context, String[], boolean)Z */
internal object HasTrustedActiveSharedSubscriptionFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubClient;",
    name = "hasTrustedActiveSharedSubscription",
    returnType = "Z",
    parameters = listOf(
        "Landroid/content/Context;",
        "[Ljava/lang/String;",
        "Z",
    ),
)

/** SharedSubClient.queryPackage(Context, String)Z */
internal object QueryPackageFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubClient;",
    name = "queryPackage",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
)

/** SharedSubClient.trustedActiveSharedSubscriptionExpiry(Context, String[], boolean)J */
internal object TrustedActiveSharedSubscriptionExpiryFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubClient;",
    name = "trustedActiveSharedSubscriptionExpiry",
    returnType = "J",
    parameters = listOf(
        "Landroid/content/Context;",
        "[Ljava/lang/String;",
        "Z",
    ),
)

/** SharedSubClient.queryPackageExpiry(Context, String)J */
internal object QueryPackageExpiryFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubClient;",
    name = "queryPackageExpiry",
    returnType = "J",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
)

/** SharedSubStore.isActive(Context)Z */
internal object SharedSubStoreIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubStore;",
    name = "isActive",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
)

/** SharedSubStore.expiresAt(Context)J */
internal object SharedSubStoreExpiresAtFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubStore;",
    name = "expiresAt",
    returnType = "J",
    parameters = listOf("Landroid/content/Context;"),
)

/** SharedSubTrustPolicy.isTrusted(Context, String, String[], boolean)Z */
internal object SharedSubTrustPolicyIsTrustedFingerprint : Fingerprint(
    definingClass = "Lcom/bimiboo/pass/SharedSubTrustPolicy;",
    name = "isTrusted",
    returnType = "Z",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "[Ljava/lang/String;",
        "Z",
    ),
)
