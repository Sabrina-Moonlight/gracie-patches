package app.template.extension.werewolf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Runtime OkHttp interceptor via reflection (no okhttp compile dependency).
 * Never throws out of intercept; on any failure falls through to the real chain.
 */
@SuppressWarnings("unused")
public final class WheelNetworkHook {

    public static final WheelNetworkHook INSTANCE = new WheelNetworkHook();

    private WheelNetworkHook() {
    }

    /** Always returns a non-null Interceptor proxy. */
    public static Object createInterceptor() {
        try {
            Class<?> interceptorClass = Class.forName("okhttp3.Interceptor");
            return Proxy.newProxyInstance(
                    interceptorClass.getClassLoader(),
                    new Class<?>[] {interceptorClass},
                    new Handler()
            );
        } catch (Throwable t) {
            // Last resort: still try with context classloader
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> interceptorClass = Class.forName("okhttp3.Interceptor", true, cl);
                return Proxy.newProxyInstance(
                        cl,
                        new Class<?>[] {interceptorClass},
                        new Handler()
                );
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private static final class Handler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                String name = method.getName();
                if ("intercept".equals(name) && args != null && args.length == 1) {
                    return interceptSafe(args[0]);
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return proxy == (args != null ? args[0] : null);
                }
                if ("toString".equals(name)) {
                    return "WheelNetworkHook";
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static Object interceptSafe(Object chain) throws Throwable {
        try {
            return intercept(chain);
        } catch (Throwable t) {
            // Always fall through to real network
            Object request = chain.getClass().getMethod("request").invoke(chain);
            Class<?> requestClass = Class.forName("okhttp3.Request");
            return chain.getClass().getMethod("proceed", requestClass).invoke(chain, request);
        }
    }

    private static Object intercept(Object chain) throws Throwable {
        Class<?> requestClass = Class.forName("okhttp3.Request");
        Method proceed = chain.getClass().getMethod("proceed", requestClass);
        Object request = chain.getClass().getMethod("request").invoke(chain);
        Object httpUrl = request.getClass().getMethod("url").invoke(request);
        String url = String.valueOf(httpUrl);

        if (!isTarget(url)) {
            return proceed.invoke(chain, request);
        }

        Object upstream = proceed.invoke(chain, request);
        String json = bodyFor(url);
        Object responseBody = createResponseBody(json);

        Method newBuilder = upstream.getClass().getMethod("newBuilder");
        Object builder = newBuilder.invoke(upstream);
        builder.getClass().getMethod("code", int.class).invoke(builder, 200);
        builder.getClass().getMethod("message", String.class).invoke(builder, "OK");
        Class<?> bodyClass = Class.forName("okhttp3.ResponseBody");
        builder.getClass().getMethod("body", bodyClass).invoke(builder, responseBody);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static boolean isTarget(String url) {
        // Only the spin (and ads quota). Do not touch conditions/items on startup.
        return url.contains("goldenWheelSpin")
                || url.contains("rewardVideosAdsToPlay");
    }

    private static String bodyFor(String url) {
        if (url.contains("rewardVideosAdsToPlay")) {
            return "{\"adsToPlay\":99,\"count\":99,\"remaining\":99}";
        }
        if (url.contains("goldenWheelConditions")) {
            return "{\"supported\":true,\"canSpin\":true,\"nextAvailableTime\":0}";
        }
        if (url.contains("goldenWheelItems")) {
            return "{\"items\":[{\"type\":\"gold\",\"amount\":30},{\"type\":\"gold\",\"amount\":15}]}";
        }
        return "{"
                + "\"success\":true,"
                + "\"granted\":true,"
                + "\"rewardGranted\":true,"
                + "\"type\":\"gold\","
                + "\"amount\":30,"
                + "\"goldAmount\":30,"
                + "\"reward\":{\"type\":\"gold\",\"amount\":30},"
                + "\"item\":{\"type\":\"gold\",\"amount\":30},"
                + "\"nextAvailableTime\":0"
                + "}";
    }

    private static Object createResponseBody(String json) throws Throwable {
        Class<?> mediaTypeClass = Class.forName("okhttp3.MediaType");
        Object mediaType = mediaTypeClass.getMethod("parse", String.class)
                .invoke(null, "application/json; charset=utf-8");
        Class<?> responseBodyClass = Class.forName("okhttp3.ResponseBody");
        try {
            return responseBodyClass.getMethod("create", String.class, mediaTypeClass)
                    .invoke(null, json, mediaType);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            return responseBodyClass.getMethod("create", mediaTypeClass, String.class)
                    .invoke(null, mediaType, json);
        } catch (NoSuchMethodException ignored) {
        }
        Object companion = responseBodyClass.getField("Companion").get(null);
        return companion.getClass().getMethod("create", String.class, mediaTypeClass)
                .invoke(companion, json, mediaType);
    }
}
