package app.template.extension.werewolf;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Rewrites goldenWheelSpin responses only.
 * Tries a response shape aligned with SET_GOLDEN_WHEEL_REWARD_* keys.
 */
@SuppressWarnings("unused")
public final class WheelNetworkHook implements Interceptor {

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    // Shape based on client keys: REWARD_ITEM / REWARD_GRANTED / gold amounts on the wheel UI
    private static final String SPIN_OK =
            "{"
            + "\"rewardGranted\":true,"
            + "\"granted\":true,"
            + "\"success\":true,"
            + "\"item\":{"
            +   "\"type\":\"gold\","
            +   "\"amount\":30,"
            +   "\"goldAmount\":30,"
            +   "\"name\":\"gold\""
            + "},"
            + "\"reward\":{"
            +   "\"type\":\"gold\","
            +   "\"amount\":30,"
            +   "\"goldAmount\":30"
            + "},"
            + "\"type\":\"gold\","
            + "\"amount\":30,"
            + "\"goldAmount\":30,"
            + "\"nextAvailableTime\":0,"
            + "\"rewardNextAvailableTime\":0"
            + "}";

    private WheelNetworkHook() {
    }

    public static OkHttpClient.Builder install(OkHttpClient.Builder builder) {
        if (builder == null) {
            return null;
        }
        return builder.addInterceptor(new WheelNetworkHook());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        try {
            String url = request.url().toString();
            if (!url.contains("goldenWheelSpin")) {
                return response;
            }

            MediaType mediaType = JSON;
            ResponseBody oldBody = response.body();
            if (oldBody != null) {
                if (oldBody.contentType() != null) {
                    mediaType = oldBody.contentType();
                }
                oldBody.close();
            }

            ResponseBody newBody = ResponseBody.create(SPIN_OK, mediaType);
            return response.newBuilder()
                    .code(200)
                    .message("OK")
                    .body(newBody)
                    .build();
        } catch (Throwable t) {
            return response;
        }
    }
}
