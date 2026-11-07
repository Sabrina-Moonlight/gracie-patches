package app.template.extension.werewolf;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Only rewrites /rewards/goldenWheelSpin responses.
 * All other traffic is left untouched (safe on app startup).
 */
@SuppressWarnings("unused")
public final class WheelNetworkHook implements Interceptor {

    private static final String SPIN_PATH = "goldenWheelSpin";
    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private static final String SPIN_OK = "{"
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
            String path = request.url().encodedPath();
            if (path == null || !path.contains(SPIN_PATH)) {
                return response;
            }

            ResponseBody oldBody = response.body();
            if (oldBody != null) {
                oldBody.close();
            }

            ResponseBody newBody = ResponseBody.create(SPIN_OK, JSON);
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
