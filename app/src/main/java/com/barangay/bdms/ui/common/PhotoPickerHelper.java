package com.barangay.bdms.ui.common;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared camera + system photo picker helper producing multipart image payloads under 5 MB.
 * Instantiate before STARTED lifecycle state (e.g. in Fragment.onCreate or Activity.onCreate).
 */
public class PhotoPickerHelper {

    public static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_DIMENSION_PX = 1600;
    private static final String STATE_CAMERA_URI = "bdms_picker_pending_camera_uri";
    private static final String STATE_CAMERA_PATH = "bdms_picker_pending_camera_path";
    private static final String STATE_SELECTED_URI = "bdms_picker_selected_uri";

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    ));

    private static final ExecutorService COMPRESS_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface OnPhotoPickedListener {
        void onPhotoPicked(@NonNull Uri uri);
        void onPhotoError(@NonNull String message);
    }

    public interface PartsCallback {
        void onPartsReady(@NonNull List<MultipartBody.Part> parts);
        void onPartsError(@NonNull String message);
    }

    private final Context context;
    private final OnPhotoPickedListener listener;
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;

    @Nullable
    private Uri pendingCameraUri;
    @Nullable
    private File pendingCameraFile;
    @Nullable
    private Uri selectedUri;

    public PhotoPickerHelper(@NonNull Fragment fragment, @NonNull OnPhotoPickedListener listener) {
        this.context = fragment.requireContext().getApplicationContext();
        this.listener = listener;
        this.galleryLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::handleGalleryResult
        );
        this.cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                this::handleCameraResult
        );
    }

    public PhotoPickerHelper(@NonNull AppCompatActivity activity, @NonNull OnPhotoPickedListener listener) {
        this.context = activity.getApplicationContext();
        this.listener = listener;
        this.galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::handleGalleryResult
        );
        this.cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                this::handleCameraResult
        );
    }

    public void saveInstanceState(@Nullable Bundle outState) {
        if (outState == null) return;
        if (pendingCameraUri != null) {
            outState.putParcelable(STATE_CAMERA_URI, pendingCameraUri);
        }
        if (pendingCameraFile != null) {
            outState.putString(STATE_CAMERA_PATH, pendingCameraFile.getAbsolutePath());
        }
        if (selectedUri != null) {
            outState.putParcelable(STATE_SELECTED_URI, selectedUri);
        }
    }

    @SuppressWarnings("deprecation")
    public void restoreInstanceState(@Nullable Bundle savedState) {
        if (savedState == null) return;
        pendingCameraUri = savedState.getParcelable(STATE_CAMERA_URI);
        String camPath = savedState.getString(STATE_CAMERA_PATH);
        pendingCameraFile = camPath != null ? new File(camPath) : null;
        selectedUri = savedState.getParcelable(STATE_SELECTED_URI);
    }

    public void showPickerDialog(@NonNull Context uiContext) {
        new AlertDialog.Builder(uiContext)
                .setTitle("Select Photo")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void launchGallery() {
        try {
            galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (ActivityNotFoundException e) {
            listener.onPhotoError("No photo picker available on this device.");
        }
    }

    public void launchCamera() {
        try {
            File cacheDir = new File(context.getCacheDir(), "camera");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                listener.onPhotoError("Unable to prepare camera cache directory.");
                return;
            }
            pendingCameraFile = File.createTempFile("capture_", ".jpg", cacheDir);
            pendingCameraUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pendingCameraFile
            );
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | IllegalArgumentException | ActivityNotFoundException e) {
            pendingCameraUri = null;
            pendingCameraFile = null;
            listener.onPhotoError("Camera unavailable: " + e.getMessage());
        }
    }

    private void handleGalleryResult(@Nullable Uri uri) {
        if (uri == null) return;
        String mime = resolveMimeType(context, uri);
        if (mime != null && !ALLOWED_MIME_TYPES.contains(mime)) {
            listener.onPhotoError("Unsupported image format. Use JPEG, PNG, or WebP.");
            return;
        }
        selectedUri = uri;
        listener.onPhotoPicked(uri);
    }

    private void handleCameraResult(boolean success) {
        if (success && pendingCameraUri != null) {
            selectedUri = pendingCameraUri;
            listener.onPhotoPicked(pendingCameraUri);
        } else if (pendingCameraFile != null && pendingCameraFile.exists()) {
            pendingCameraFile.delete();
        }
    }

    @Nullable
    public Uri getSelectedUri() {
        return selectedUri;
    }

    public void clearSelection() {
        selectedUri = null;
    }

    @NonNull
    public MultipartBody.Part createMultipartPart(@NonNull String fieldName, @NonNull Uri uri) throws IOException {
        return buildMultipartPart(context, fieldName, uri);
    }

    public static void createMultipartPartAsync(
            @NonNull Context context,
            @NonNull String fieldName,
            @NonNull Uri uri,
            @NonNull PartsCallback callback
    ) {
        createMultipartPartsAsync(context, fieldName, Collections.singletonList(uri), callback);
    }

    public static void createMultipartPartsAsync(
            @NonNull Context context,
            @NonNull String fieldName,
            @NonNull List<Uri> uris,
            @NonNull PartsCallback callback
    ) {
        Context appContext = context.getApplicationContext();
        List<Uri> copy = new ArrayList<>(uris);
        COMPRESS_EXECUTOR.execute(() -> {
            List<MultipartBody.Part> parts = new ArrayList<>();
            try {
                for (Uri uri : copy) {
                    parts.add(buildMultipartPart(appContext, fieldName, uri));
                }
                MAIN_HANDLER.post(() -> callback.onPartsReady(parts));
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unable to process selected image.";
                MAIN_HANDLER.post(() -> callback.onPartsError(msg));
            }
        });
    }

    @NonNull
    public static MultipartBody.Part buildMultipartPart(
            @NonNull Context context,
            @NonNull String fieldName,
            @NonNull Uri uri
    ) throws IOException {
        Context appContext = context.getApplicationContext();
        byte[] rawBytes = readBytes(appContext, uri);
        String mime = resolveMimeType(appContext, uri);
        if (mime == null || !ALLOWED_MIME_TYPES.contains(mime)) {
            mime = "image/jpeg";
        }

        byte[] uploadBytes = rawBytes;
        String outMime = mime;
        if (uploadBytes.length > MAX_FILE_BYTES) {
            uploadBytes = compressImageToLimit(rawBytes);
            outMime = "image/jpeg";
        }

        if (uploadBytes.length > MAX_FILE_BYTES) {
            throw new IOException("Selected image exceeds the 5 MB upload limit.");
        }

        String ext = extensionForMime(outMime);
        String filename = fieldName + "_" + System.currentTimeMillis() + "." + ext;
        RequestBody body = RequestBody.create(MediaType.parse(outMime), uploadBytes);
        return MultipartBody.Part.createFormData(fieldName, filename, body);
    }

    @Nullable
    private static String resolveMimeType(@NonNull Context context, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        String type = resolver.getType(uri);
        if (type != null) {
            return type.toLowerCase(Locale.US);
        }
        String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (ext != null && !ext.isEmpty()) {
            String mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
            if (mapped != null) {
                return mapped.toLowerCase(Locale.US);
            }
        }
        return null;
    }

    @NonNull
    private static String extensionForMime(@NonNull String mime) {
        if ("image/png".equals(mime)) return "png";
        if ("image/webp".equals(mime)) return "webp";
        return "jpg";
    }

    @NonNull
    private static byte[] readBytes(@NonNull Context context, @NonNull Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (is == null) {
                throw new IOException("Unable to open selected image.");
            }
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    @NonNull
    private static byte[] compressImageToLimit(@NonNull byte[] rawBytes) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, bounds);

        int sampleSize = 1;
        while ((bounds.outWidth / sampleSize) > MAX_DIMENSION_PX
                || (bounds.outHeight / sampleSize) > MAX_DIMENSION_PX) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, decodeOpts);
        if (bitmap == null) {
            throw new IOException("Selected file is not a valid image.");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            while (out.size() > MAX_FILE_BYTES && quality > 40) {
                out.reset();
                quality -= 15;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            }
            return out.toByteArray();
        } finally {
            bitmap.recycle();
        }
    }
}
