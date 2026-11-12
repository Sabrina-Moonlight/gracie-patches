/*
 * Copyright (C) 2026 Sabrina-Moonlight / Gracie Patches
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.template.extension.shared;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Injects synthetic Play Billing purchases for Hamster Escape product IDs so
 * Unity IAP restore/query sees owned offers (No Ads, subscription, care bundle).
 */
@SuppressWarnings("unused")
public final class HamsterPlayPurchases {

    private static final String TAG = "HamsterPlayPurchases";
    private static final String PURCHASE_CLASS = "com.android.billingclient.api.Purchase";
    private static final String PACKAGE_NAME = "gg.sunday.hamsterescape";

    // Product IDs recovered from global-metadata.dat of build 5.2.0
    private static final String[] PRODUCT_IDS = new String[] {
        "no_ads_purchase",
        "remove_ads",
        "gg.sunday.hamsterescape.subscription",
        "hamsterCareBundleOffer",
    };

    private HamsterPlayPurchases() {
    }

    /**
     * Prepends synthetic owned purchases to the list passed to
     * PurchasesResponseListener.onQueryPurchasesResponse.
     */
    public static List<Object> withSyntheticPurchases(List<Object> purchases) {
        try {
            List<Object> result = new ArrayList<>();
            if (purchases != null) {
                result.addAll(purchases);
            }
            long purchaseTime = System.currentTimeMillis() - 24L * 60L * 60L * 1000L;
            for (String productId : PRODUCT_IDS) {
                Object purchase = Class.forName(PURCHASE_CLASS)
                    .getConstructor(String.class, String.class)
                    .newInstance(purchaseJson(productId, purchaseTime), "");
                result.add(purchase);
            }
            return result;
        } catch (Throwable t) {
            Log.w(TAG, "synthetic purchase inject failed", t);
            return purchases;
        }
    }

    private static String purchaseJson(String productId, long purchaseTime) throws Exception {
        // purchaseState 0 in the JSON constructor maps to owned in BillingClient 6/8 parse path
        return new JSONObject()
            .put("orderId", "GPA.3370-" + Math.abs(productId.hashCode()))
            .put("packageName", PACKAGE_NAME)
            .put("productId", productId)
            .put("productIds", new JSONArray().put(productId))
            .put("purchaseTime", purchaseTime)
            .put("purchaseState", 0)
            .put("purchaseToken", UUID.randomUUID().toString())
            .put("quantity", 1)
            .put("autoRenewing", productId.contains("subscription"))
            .put("acknowledged", true)
            .toString();
    }
}
