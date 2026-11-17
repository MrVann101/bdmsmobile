package com.barangay.bdms.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.barangay.bdms.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Loads and caches downsampled protected images served via /api/uploads/:type/:filename
 * using ApiClient's Bearer token interceptor.
 */
public final class AuthenticatedImageLoader {

    private static final int DEFAULT_MAX_DIMENSION_PX = 1024;
    private static final int CACHE_SIZE_KB = 4 * 1024;
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AuthenticatedImageLoader() {
    }

    public static void load(
            Context context,
            @Nullable String serverPath,
            ImageView target,
            @DrawableRes int fallbackRes
    ) {
        load(context, serverPath, target, fallbackRes, DEFAULT_MAX_DIMENSION_PX);
    }

    public static void load(
            Context context,
            @Nullable String serverPath,
            ImageView target,
            @DrawableRes int fallbackRes,
            int maxDimensionPx
    ) {
        if (target == null) return;
        String url = resolveUploadUrl(serverPath);
        if (url == null) {
            target.setTag(null);
            target.setImageResource(fallbackRes);
            return;
        }

        String cacheKey = url + "#" + maxDimensionPx;
        Bitmap cached = MEMORY_CACHE.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            target.setTag(cacheKey);
            target.setImageBitmap(cached);
            return;
        }

        target.setTag(cacheKey);
        target.setImageResource(fallbackRes);
        ApiClient.initToken(context);

        WeakReference<ImageView> targetRef = new WeakReference<>(target);
        EXECUTOR.execute(() -> {
            Bitmap loaded = fetchDownsampledBitmap(url, maxDimensionPx);
            if (loaded != null) {
                MEMORY_CACHE.put(cacheKey, loaded);
            }
            MAIN_HANDLER.post(() -> {
                ImageView view = targetRef.get();
                if (view == null) return;
                Object currentTag = view.getTag();
                if (cacheKey.equals(currentTag) && loaded != null) {
                    view.setImageBitmap(loaded);
                }
            });
        });
    }

    public static void invalidate(@Nullable String serverPath) {
        String url = resolveUploadUrl(serverPath);
        if (url == null) return;
        for (String key : MEMORY_CACHE.snapshot().keySet()) {
            if (key.equals(url) || key.startsWith(url + "#")) {
                MEMORY_CACHE.remove(key);
            }
        }
    }

    @Nullable
    public static String resolveUploadUrl(@Nullable String serverPath) {
        if (serverPath == null || serverPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = serverPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String clean = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (clean.startsWith("api/")) {
            clean = clean.substring(4);
        }
        return BuildConfig.API_BASE_URL + clean;
    }

    @Nullable
    private static Bitmap fetchDownsampledBitmap(String url, int maxDimPx) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = ApiClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            ResponseBody body = response.body();
            if (body == null) return null;

            byte[] bytes = body.bytes();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sampleSize = 1;
            int targetMax = Math.max(64, maxDimPx);
            while ((bounds.outWidth / sampleSize) > targetMax
                    || (bounds.outHeight / sampleSize) > targetMax) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sampleSize;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOpts);
        } catch (Exception ignored) {
            return null;
        }
    }
}
