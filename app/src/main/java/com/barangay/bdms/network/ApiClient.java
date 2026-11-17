package com.barangay.bdms.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.barangay.bdms.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ApiClient {

    public interface SessionExpiredListener {
        void onSessionExpired(String reason);
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile Retrofit retrofit = null;
    private static volatile OkHttpClient httpClient = null;
    private static volatile String jwtToken = null;
    private static volatile SessionManager sessionManager = null;
    private static volatile SessionExpiredListener sessionExpiredListener = null;

    public static void setSessionExpiredListener(@Nullable SessionExpiredListener listener) {
        sessionExpiredListener = listener;
    }

    public static void init(Context context) {
        initToken(context);
    }

    public static void initToken(Context context) {
        if (context == null) return;
        sessionManager = SessionManager.getInstance(context);
        jwtToken = sessionManager.isLoggedIn() ? sessionManager.getToken() : null;
    }

    public static void saveToken(Context context, String token) {
        jwtToken = token;
        if (context != null) {
            sessionManager = SessionManager.getInstance(context);
            sessionManager.saveToken(token);
        }
    }

    public static void saveSession(
            Context context,
            @Nullable String token,
            @Nullable String userId,
            @Nullable String role,
            @Nullable String purok,
            @Nullable String fullName,
            @Nullable String email
    ) {
        jwtToken = token;
        if (context != null) {
            sessionManager = SessionManager.getInstance(context);
            sessionManager.saveSession(token, userId, role, purok, fullName, email);
        }
    }

    public static void clearToken(Context context) {
        jwtToken = null;
        if (context != null) {
            sessionManager = SessionManager.getInstance(context);
            sessionManager.clearSession();
        } else if (sessionManager != null) {
            sessionManager.clearSession();
        }
    }

    public static ApiService getApiService(Context context) {
        if (context != null && sessionManager == null) {
            initToken(context);
        }
        return getApiService();
    }

    public static OkHttpClient getHttpClient() {
        getApiService();
        return httpClient;
    }

    public static String getSocketBaseUrl() {
        String apiUrl = BuildConfig.API_BASE_URL;
        if (apiUrl.endsWith("/api/")) {
            return apiUrl.substring(0, apiUrl.length() - 5);
        }
        if (apiUrl.endsWith("/api")) {
            return apiUrl.substring(0, apiUrl.length() - 4);
        }
        return apiUrl;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(
                            BuildConfig.DEBUG
                                    ? HttpLoggingInterceptor.Level.BODY
                                    : HttpLoggingInterceptor.Level.NONE
                    );

                    Interceptor authInterceptor = new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = jwtToken;
                            if ((token == null || token.isEmpty()) && sessionManager != null) {
                                token = sessionManager.getToken();
                                jwtToken = token;
                            }
                            Request outgoing = original;
                            boolean sentAuthHeader = false;
                            if (token != null && !token.isEmpty()) {
                                outgoing = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build();
                                sentAuthHeader = true;
                            }

                            Response response = chain.proceed(outgoing);
                            if (sentAuthHeader && isAuthSessionFailure(outgoing, response)) {
                                jwtToken = null;
                                if (sessionManager != null) {
                                    sessionManager.clearSession();
                                }
                                SessionExpiredListener listener = sessionExpiredListener;
                                if (listener != null) {
                                    MAIN_HANDLER.post(() -> listener.onSessionExpired("Your session expired. Please log in again."));
                                }
                            }
                            return response;
                        }
                    };

                    httpClient = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(loggingInterceptor)
                            .build();

                    Gson gson = createGson();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .client(httpClient)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit.create(ApiService.class);
    }

    private static boolean isAuthSessionFailure(Request request, Response response) {
        String path = request.url().encodedPath();
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            return false;
        }
        int code = response.code();
        if (code == 401) {
            return true;
        }
        if (code == 403) {
            try {
                ResponseBody body = response.peekBody(2048);
                String text = body.string().toLowerCase(Locale.US);
                return text.contains("expired") || text.contains("invalid") || text.contains("deactivated");
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();
    }

    private static final class OffsetDateTimeAdapter
            implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {

        @Override
        public JsonElement serialize(
                OffsetDateTime src,
                Type typeOfSrc,
                JsonSerializationContext context
        ) {
            return new JsonPrimitive(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(src));
        }

        @Override
        public OffsetDateTime deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context
        ) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            String value = json.getAsString();
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String trimmed = value.trim();
            try {
                return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                try {
                    return Instant.parse(trimmed).atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException ignoredInstant) {
                    try {
                        return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atOffset(ZoneOffset.UTC);
                    } catch (DateTimeParseException e) {
                        throw new JsonParseException("Unparseable OffsetDateTime: " + trimmed, e);
                    }
                }
            }
        }
    }
}
