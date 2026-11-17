package com.barangay.bdms.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrorUtils {

    private static final Gson GSON = new Gson();

    private ApiErrorUtils() {
    }

    @NonNull
    public static String parseErrorMessage(@Nullable Response<?> response, @NonNull String fallback) {
        JsonObject obj = parseErrorJson(response);
        if (obj != null && obj.has("error") && !obj.get("error").isJsonNull()) {
            String msg = obj.get("error").getAsString();
            if (msg != null && !msg.trim().isEmpty()) {
                return msg.trim();
            }
        }
        return fallback;
    }

    @Nullable
    public static String parseErrorCode(@Nullable Response<?> response) {
        JsonObject obj = parseErrorJson(response);
        if (obj != null && obj.has("code") && !obj.get("code").isJsonNull()) {
            return obj.get("code").getAsString();
        }
        return null;
    }

    @Nullable
    private static JsonObject parseErrorJson(@Nullable Response<?> response) {
        if (response == null) return null;
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) return null;
        try {
            String raw = errorBody.source().peek().readUtf8();
            if (raw == null || raw.trim().isEmpty()) return null;
            return GSON.fromJson(raw, JsonObject.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
