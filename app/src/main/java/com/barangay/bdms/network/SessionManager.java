package com.barangay.bdms.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Encrypted session store for the authenticated user's JWT and core identity claims.
 * Shared across Resident, Staff, and Admin screens so role checks read a single source of truth.
 */
public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "bdms_secure_prefs";
    private static final String FALLBACK_PREF_NAME = "bdms_session_fallback_prefs";

    private static final String KEY_JWT = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_PUROK = "user_purok";
    private static final String KEY_FULL_NAME = "user_full_name";
    private static final String KEY_EMAIL = "user_email";

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.prefs = createPrefsWithRecovery(context.getApplicationContext());
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    private SharedPreferences createPrefsWithRecovery(Context appContext) {
        try {
            return openEncryptedPrefs(appContext);
        } catch (GeneralSecurityException | IOException firstErr) {
            Log.w(TAG, "EncryptedSharedPreferences failed, attempting key store recovery", firstErr);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appContext.deleteSharedPreferences(PREF_NAME);
                } else {
                    appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
                }
                return openEncryptedPrefs(appContext);
            } catch (GeneralSecurityException | IOException retryErr) {
                Log.e(TAG, "EncryptedSharedPreferences unrecoverable; using private SharedPreferences fallback", retryErr);
                return appContext.getSharedPreferences(FALLBACK_PREF_NAME, Context.MODE_PRIVATE);
            }
        }
    }

    private SharedPreferences openEncryptedPrefs(Context appContext) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                appContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public void saveSession(
            @Nullable String token,
            @Nullable String userId,
            @Nullable String role,
            @Nullable String purok,
            @Nullable String fullName,
            @Nullable String email
    ) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        putOrRemove(editor, KEY_JWT, token);
        putOrRemove(editor, KEY_USER_ID, userId);
        putOrRemove(editor, KEY_ROLE, role);
        putOrRemove(editor, KEY_PUROK, purok);
        putOrRemove(editor, KEY_FULL_NAME, fullName);
        putOrRemove(editor, KEY_EMAIL, email);
        editor.apply();
    }

    public void saveToken(@Nullable String token) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        putOrRemove(editor, KEY_JWT, token);
        editor.apply();
    }

    private void putOrRemove(SharedPreferences.Editor editor, String key, @Nullable String value) {
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
    }

    @Nullable
    public String getToken() {
        return prefs != null ? prefs.getString(KEY_JWT, null) : null;
    }

    @Nullable
    public String getUserId() {
        return prefs != null ? prefs.getString(KEY_USER_ID, null) : null;
    }

    @Nullable
    public String getRole() {
        return prefs != null ? prefs.getString(KEY_ROLE, null) : null;
    }

    @Nullable
    public String getPurok() {
        return prefs != null ? prefs.getString(KEY_PUROK, null) : null;
    }

    @Nullable
    public String getFullName() {
        return prefs != null ? prefs.getString(KEY_FULL_NAME, null) : null;
    }

    @Nullable
    public String getEmail() {
        return prefs != null ? prefs.getString(KEY_EMAIL, null) : null;
    }

    public boolean isTokenExpired() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length < 2) {
            return true;
        }
        try {
            byte[] decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(payloadJson);
            if (!obj.has("exp")) {
                return false;
            }
            long expSeconds = obj.getLong("exp");
            long nowSeconds = System.currentTimeMillis() / 1000L;
            return nowSeconds >= (expSeconds - 15L);
        } catch (Exception e) {
            Log.w(TAG, "Unable to decode JWT exp claim", e);
            return true;
        }
    }

    public boolean isLoggedIn() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        if (isTokenExpired()) {
            clearSession();
            return false;
        }
        return true;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getRole());
    }

    public boolean isStaffOrAdmin() {
        String role = getRole();
        return "staff".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    public boolean isResident() {
        return "resident".equalsIgnoreCase(getRole());
    }

    public void clearSession() {
        if (prefs == null) return;
        prefs.edit().clear().apply();
    }
}
