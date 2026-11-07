package app.template.extension.werewolf;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Rewrites golden-wheel related API responses so the client can grant the spin.
 */
@SuppressWarnings("unused")
public final class WheelNetworkHook implements Interceptor {

    public static final WheelNetworkHook INSTANCE = new WheelNetworkHook();

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private WheelNetworkHook() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();

        if (url.contains("goldenWheelSpin")
                || url.contains("rewardVideosAdsToPlay")
                || url.contains("goldenWheelConditions")
                || url.contains("goldenWheelItems")) {
            Response upstream = null;
            try {
                upstream = chain.proceed(request);
            } catch (IOException ignored) {
                // still return a local success body
            }
            String body = bodyFor(url);
            ResponseBody responseBody = ResponseBody.create(body, JSON);
            if (upstream != null) {
                return upstream.newBuilder()
                        .code(200)
                        .message("OK")
                        .body(responseBody)
                        .build();
            }
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build();
        }

        return chain.proceed(request);
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
}
