/*
 * Copyright (C) 2026 Sabrina-Moonlight / Gracie Patches
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.template.extension.shared;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Method;

/**
 * Activates Bimi Boo BB Pass / SharedSub local state so Unity JNI checks see an
 * active shared subscription. Safe no-op if classes or methods are missing.
 */
@SuppressWarnings("unused")
public final class BbPassUnlockHelper {

    private static final long FAR_EXPIRY_MS = 4102444800000L; // ~2100-01-01

    private BbPassUnlockHelper() {
    }

    /** Call from Activity/Application onCreate with a Context. */
    public static void activate(Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        trySetLocalActive(app);
        trySetStoreActive(app);
        tryWritePrefs(app);
    }

    private static void trySetLocalActive(Context ctx) {
        try {
            Class<?> client = Class.forName("com.bimiboo.pass.SharedSubClient");
            Method m = client.getMethod(
                    "setLocalActiveSharedSubscription",
                    Context.class,
                    boolean.class,
                    long.class
            );
            m.invoke(null, ctx, true, FAR_EXPIRY_MS);
        } catch (Throwable ignored) {
            try {
                Class<?> client = Class.forName("com.bimiboo.pass.SharedSubClient");
                Object instance = client.getDeclaredConstructor().newInstance();
                Method m = client.getMethod(
                        "setLocalActiveSharedSubscription",
                        Context.class,
                        boolean.class,
                        long.class
                );
                m.invoke(instance, ctx, true, FAR_EXPIRY_MS);
            } catch (Throwable ignored2) {
            }
        }
    }

    private static void trySetStoreActive(Context ctx) {
        try {
            Class<?> store = Class.forName("com.bimiboo.pass.SharedSubStore");
            Method m = store.getMethod(
                    "setActive",
                    Context.class,
                    boolean.class,
                    long.class
            );
            try {
                m.invoke(null, ctx, true, FAR_EXPIRY_MS);
            } catch (Throwable notStatic) {
                Object instance = store.getDeclaredConstructor().newInstance();
                m.invoke(instance, ctx, true, FAR_EXPIRY_MS);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Best-effort write of known preference keys used by SharedSubStore. */
    private static void tryWritePrefs(Context ctx) {
        String[] prefNames = {
                "bbpass_shared_sub",
                "bbpass.sharedsub",
                ".bbpass.sharedsub",
                "SharedSubStore",
                "com.bimiboo.pass.SharedSubStore",
        };
        for (String name : prefNames) {
            try {
                SharedPreferences prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("KEY_ACTIVE", true)
                        .putBoolean("active", true)
                        .putBoolean("isActive", true)
                        .putBoolean("KEY_TRUSTED", true)
                        .putBoolean("trusted", true)
                        .putLong("KEY_EXPIRES", FAR_EXPIRY_MS)
                        .putLong("expiresAt", FAR_EXPIRY_MS)
                        .putLong("expires_at", FAR_EXPIRY_MS)
                        .apply();
            } catch (Throwable ignored) {
            }
        }
    }
}
