package com.barangay.bdms.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Manages the authenticated Socket.io connection for live notifications:{userId} updates.
 */
public class NotificationSocketManager {

    private static final String TAG = "NotificationSocket";
    private static volatile NotificationSocketManager instance;

    public interface Listener {
        void onNotificationReceived();
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Socket socket;

    private NotificationSocketManager() {
    }

    public static NotificationSocketManager getInstance() {
        if (instance == null) {
            synchronized (NotificationSocketManager.class) {
                if (instance == null) {
                    instance = new NotificationSocketManager();
                }
            }
        }
        return instance;
    }

    public synchronized void connect(Context context) {
        SessionManager session = SessionManager.getInstance(context);
        String token = session.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }
        if (socket != null && socket.connected()) {
            return;
        }
        disconnect();

        try {
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", token))
                    .setReconnection(true)
                    .setReconnectionAttempts(5)
                    .build();

            socket = IO.socket(ApiClient.getSocketBaseUrl(), options);
            socket.on("notification:new", args ->
                    mainHandler.post(() -> {
                        for (Listener listener : listeners) {
                            listener.onNotificationReceived();
                        }
                    })
            );
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                if (args != null && args.length > 0) {
                    Log.w(TAG, "Socket connect error (polling fallback active): " + args[0]);
                }
            });
            socket.connect();
        } catch (Exception e) {
            Log.w(TAG, "Unable to initialize Socket.io client", e);
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}
