package app.template.extension.werewolf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Runtime OkHttp interceptor via reflection (no okhttp compile dependency).
 * Rewrites golden-wheel related API responses.
 */
@SuppressWarnings("unused")
public final class WheelNetworkHook {

    public static final WheelNetworkHook INSTANCE = new WheelNetworkHook();

    private WheelNetworkHook() {
    }

    /** Injection: returns an okhttp3.Interceptor instance. */
    public static Object createInterceptor() {
        try {
            Class<?> interceptorClass = Class.forName("okhttp3.Interceptor");
            return Proxy.newProxyInstance(
                    interceptorClass.getClassLoader(),
                    new Class<?>[] {interceptorClass},
                    new Handler()
            );
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class Handler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("intercept".equals(name) && args != null && args.length == 1) {
                return intercept(args[0]);
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("toString".equals(name)) {
                return "WheelNetworkHook";
            }
            return null;
        }
    }

    private static Object intercept(Object chain) throws Throwable {
        Method requestMethod = chain.getClass().getMethod("request");
        Object request = requestMethod.invoke(chain);
        Method urlMethod = request.getClass().getMethod("url");
        Object httpUrl = urlMethod.invoke(request);
        String url = String.valueOf(httpUrl);

        Class<?> requestClass = Class.forName("okhttp3.Request");
        Method proceed = chain.getClass().getMethod("proceed", requestClass);

        if (!isTarget(url)) {
            return proceed.invoke(chain, request);
        }

        Object upstream = null;
        try {
            upstream = proceed.invoke(chain, request);
        } catch (Throwable ignored) {
            // still return local body
        }

        String json = bodyFor(url);
        Object responseBody = createResponseBody(json);

        if (upstream != null) {
            Method newBuilder = upstream.getClass().getMethod("newBuilder");
            Object builder = newBuilder.invoke(upstream);
            builder.getClass().getMethod("code", int.class).invoke(builder, 200);
            builder.getClass().getMethod("message", String.class).invoke(builder, "OK");
            Class<?> bodyClass = Class.forName("okhttp3.ResponseBody");
            builder.getClass().getMethod("body", bodyClass).invoke(builder, responseBody);
            return builder.getClass().getMethod("build").invoke(builder);
        }

        Class<?> responseBuilderClass = Class.forName("okhttp3.Response$Builder");
        Object builder = responseBuilderClass.getConstructor().newInstance();
        responseBuilderClass.getMethod("request", requestClass).invoke(builder, request);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object protocol = Enum.valueOf((Class<Enum>) Class.forName("okhttp3.Protocol"), "HTTP_1_1");
        responseBuilderClass.getMethod("protocol", Class.forName("okhttp3.Protocol"))
                .invoke(builder, protocol);
        responseBuilderClass.getMethod("code", int.class).invoke(builder, 200);
        responseBuilderClass.getMethod("message", String.class).invoke(builder, "OK");
        Class<?> bodyClass = Class.forName("okhttp3.ResponseBody");
        responseBuilderClass.getMethod("body", bodyClass).invoke(builder, responseBody);
        return responseBuilderClass.getMethod("build").invoke(builder);
    }

    private static boolean isTarget(String url) {
        return url.contains("goldenWheelSpin")
                || url.contains("rewardVideosAdsToPlay")
                || url.contains("goldenWheelConditions")
                || url.contains("goldenWheelItems");
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
        Method parse = mediaTypeClass.getMethod("parse", String.class);
        Object mediaType = parse.invoke(null, "application/json; charset=utf-8");

        Class<?> responseBodyClass = Class.forName("okhttp3.ResponseBody");
        try {
            Method create = responseBodyClass.getMethod(
                    "create", String.class, mediaTypeClass);
            return create.invoke(null, json, mediaType);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            Method create = responseBodyClass.getMethod(
                    "create", mediaTypeClass, String.class);
            return create.invoke(null, mediaType, json);
        } catch (NoSuchMethodException ignored) {
        }
        Object companion = responseBodyClass.getField("Companion").get(null);
        Method create = companion.getClass().getMethod(
                "create", String.class, mediaTypeClass);
        return create.invoke(companion, json, mediaType);
    }
}
