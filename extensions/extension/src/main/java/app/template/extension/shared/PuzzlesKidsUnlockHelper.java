/*
 * Copyright (C) 2026 Sabrina-Moonlight / Gracie Patches
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.template.extension.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Forces Full Version + Ad-Free for com.gamesforkids.animals.puzzles.jigsaw.
 * GameData fields: isFullVersion, isAdFree (Unity PlayerPrefs).
 */
@SuppressWarnings("unused")
public final class PuzzlesKidsUnlockHelper {

    private static final String TAG = "PuzzlesKidsUnlock";
    private static final String PKG = "com.gamesforkids.animals.puzzles.jigsaw";
    private static final String PURCHASE_CLASS = "com.android.billingclient.api.Purchase";

    /** Likely store product ids + constant names seen in metadata. */
    private static final String[] PRODUCT_IDS = new String[] {
        "full_version",
        "remove_ads",
        "fullversion",
        "removeads",
        "no_ads",
        "noads",
        "upgrade_version",
        "playpass",
        "kProductIDNonConsumableFullVersion",
        "kProductIDNonConsumableRemoveAds",
        "kProductIDNonConsumableUpgradeVersion",
        "kProductIDNonConsumablePlaypass",
        PKG + ".full_version",
        PKG + ".remove_ads",
        PKG + ".fullversion",
        PKG + ".removeads",
    };

    private static final String[] BOOL_KEYS = new String[] {
        "isFullVersion",
        "isAdFree",
        "is_full_version",
        "is_ad_free",
        "FullVersion",
        "AdFree",
        "RemoveAds",
        "remove_ads",
        "no_ads",
        "NoAds",
        "premium",
        "isPremium",
        "unlocked",
        "isUnlocked",
        "hasFullVersion",
        "hasRemoveAds",
    };

    private PuzzlesKidsUnlockHelper() {
    }

    /** Called from UnityPlayerActivity.onCreate / onResume. */
    public static void activate(Context ctx) {
        if (ctx == null) {
            return;
        }
        Context app = ctx.getApplicationContext();
        writeSharedPrefs(app);
        writeUnityPlayerPrefsXml(app);
        Log.i(TAG, "activate: prefs written");
    }

    /**
     * Inject synthetic owned purchases into BillingClient query results.
     * Same pattern as HamsterPlayPurchases.
     */
    public static List<Object> withSyntheticPurchases(List<Object> purchases) {
        try {
            List<Object> result = new ArrayList<>();
            if (purchases != null) {
                result.addAll(purchases);
            }
            long purchaseTime = System.currentTimeMillis() - 86_400_000L;
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

    private static void writeSharedPrefs(Context app) {
        String[] files = new String[] {
            PKG + ".v2.playerprefs",
            PKG,
            "GameData",
            "Unity",
            "unity",
            "IAP",
            "iap",
        };
        for (String name : files) {
            try {
                SharedPreferences prefs = app.getSharedPreferences(name, Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                for (String key : BOOL_KEYS) {
                    ed.putInt(key, 1);
                    ed.putBoolean(key, true);
                }
                ed.apply();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Unity sometimes persists PlayerPrefs as a plain XML under shared_prefs.
     * Writing both int=1 and the classic Unity format covers more builds.
     */
    private static void writeUnityPlayerPrefsXml(Context app) {
        try {
            File dir = new File(app.getApplicationInfo().dataDir, "shared_prefs");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File f = new File(dir, PKG + ".v2.playerprefs.xml");
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n");
            sb.append("<map>\n");
            for (String key : BOOL_KEYS) {
                sb.append("    <int name=\"").append(key).append("\" value=\"1\" />\n");
            }
            sb.append("</map>\n");
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }

    private static String purchaseJson(String productId, long purchaseTime) throws Exception {
        // purchaseState 0 = purchased in legacy JSON; getPurchaseState() is also forced to 1 in DEX
        return new JSONObject()
                .put("orderId", "GPA.3370-" + Math.abs(productId.hashCode()))
                .put("packageName", PKG)
                .put("productId", productId)
                .put("productIds", new JSONArray().put(productId))
                .put("purchaseTime", purchaseTime)
                .put("purchaseState", 0)
                .put("purchaseToken", UUID.randomUUID().toString())
                .put("quantity", 1)
                .put("acknowledged", true)
                .toString();
    }
}
