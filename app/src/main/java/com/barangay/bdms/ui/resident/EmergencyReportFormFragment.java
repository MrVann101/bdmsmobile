package com.barangay.bdms.ui.resident;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.barangay.bdms.R;
import com.barangay.bdms.model.EmergencyReport;
import com.barangay.bdms.model.Profile;
import com.barangay.bdms.network.SessionManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyReportFormFragment extends Fragment {

    private static final String STATE_DISASTER_INDEX = "emergency_disaster_idx";
    private static final String STATE_AFFECTED_COUNT = "emergency_affected_count";
    private static final String STATE_PHOTO_URI = "emergency_photo_uri";

    private static final String[] DISASTER_LABELS = {
            "Flood (flood)",
            "Fire (fire)",
            "Typhoon (typhoon)",
            "Earthquake (earthquake)",
            "Landslide (landslide)"
    };
    private static final String[] DISASTER_KEYS = {
            "flood", "fire", "typhoon", "earthquake", "landslide"
    };

    private BdmsRepository repository;
    private PhotoPickerHelper photoPickerHelper;

    private TextView tvReadOnlyPurok, txtAffectedCount;
    private AutoCompleteTextView actvDisasterType;
    private ChipGroup chipGroupNeeds;
    private ImageView ivPhotoPreview;
    private MaterialButton btnAttachPhoto, submitEmergencyBtn;
    private LoadingOverlay loadingOverlay;

    private int selectedDisasterIndex = 0;
    private int affectedCount = 1;
    @Nullable
    private Uri selectedPhotoUri = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedDisasterIndex = savedInstanceState.getInt(STATE_DISASTER_INDEX, 0);
            affectedCount = Math.max(1, savedInstanceState.getInt(STATE_AFFECTED_COUNT, 1));
            selectedPhotoUri = savedInstanceState.getParcelable(STATE_PHOTO_URI);
        }
        photoPickerHelper = new PhotoPickerHelper(this, new PhotoPickerHelper.OnPhotoPickedListener() {
            @Override
            public void onPhotoPicked(@NonNull Uri uri) {
                selectedPhotoUri = uri;
                if (isAdded() && getView() != null) {
                    applyPhotoPreviewUi();
                }
            }

            @Override
            public void onPhotoError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
        photoPickerHelper.restoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_DISASTER_INDEX, selectedDisasterIndex);
        outState.putInt(STATE_AFFECTED_COUNT, affectedCount);
        if (selectedPhotoUri != null) {
            outState.putParcelable(STATE_PHOTO_URI, selectedPhotoUri);
        }
        if (photoPickerHelper != null) {
            photoPickerHelper.saveInstanceState(outState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r11_emergency_report_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        MaterialButton btnCallHotline = view.findViewById(R.id.btnCallHotline);
        tvReadOnlyPurok = view.findViewById(R.id.tvReadOnlyPurok);
        actvDisasterType = view.findViewById(R.id.actvDisasterType);
        txtAffectedCount = view.findViewById(R.id.txtAffectedCount);
        MaterialButton btnMinus = view.findViewById(R.id.btnMinus);
        MaterialButton btnPlus = view.findViewById(R.id.btnPlus);
        chipGroupNeeds = view.findViewById(R.id.chipGroupNeeds);
        ivPhotoPreview = view.findViewById(R.id.ivEmergencyPhotoPreview);
        btnAttachPhoto = view.findViewById(R.id.btnAttachEmergencyPhoto);
        submitEmergencyBtn = view.findViewById(R.id.submitEmergencyBtn);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        btnBack.setOnClickListener(v -> NavUtils.safeNavigateUp(this));
        btnCallHotline.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("BDRRMC Emergency Hotlines")
                        .setMessage(R.string.barangay_emergency_hotlines)
                        .setPositiveButton("Close", null)
                        .show()
        );

        bindReadOnlyPurok();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                DISASTER_LABELS
        );
        actvDisasterType.setAdapter(adapter);
        int clamped = Math.max(0, Math.min(selectedDisasterIndex, DISASTER_LABELS.length - 1));
        actvDisasterType.setText(DISASTER_LABELS[clamped], false);
        actvDisasterType.setOnItemClickListener((parent, itemView, position, id) -> selectedDisasterIndex = position);

        txtAffectedCount.setText(String.valueOf(affectedCount));
        btnMinus.setOnClickListener(v -> {
            if (affectedCount > 1) {
                affectedCount--;
                txtAffectedCount.setText(String.valueOf(affectedCount));
            }
        });
        btnPlus.setOnClickListener(v -> {
            affectedCount++;
            txtAffectedCount.setText(String.valueOf(affectedCount));
        });

        applyPhotoPreviewUi();
        btnAttachPhoto.setOnClickListener(v -> photoPickerHelper.showPickerDialog(requireContext()));
        submitEmergencyBtn.setOnClickListener(v -> submitEmergency(false));
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void applyPhotoPreviewUi() {
        if (selectedPhotoUri != null && ivPhotoPreview != null) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            ivPhotoPreview.setImageURI(selectedPhotoUri);
            if (btnAttachPhoto != null) {
                btnAttachPhoto.setText("Change Scene Photo");
            }
        } else if (ivPhotoPreview != null) {
            ivPhotoPreview.setVisibility(View.GONE);
            if (btnAttachPhoto != null) {
                btnAttachPhoto.setText("Attach Scene Photo");
            }
        }
    }

    private void bindReadOnlyPurok() {
        SessionManager session = SessionManager.getInstance(requireContext());
        String purok = session.getPurok();
        if (purok != null && !purok.trim().isEmpty()) {
            tvReadOnlyPurok.setText(purok.trim());
            return;
        }
        tvReadOnlyPurok.setText("Loading...");
        repository.getMyProfile().enqueue(new Callback<Profile>() {
            @Override
            public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getPurok() != null) {
                    tvReadOnlyPurok.setText(response.body().getPurok());
                } else {
                    tvReadOnlyPurok.setText("Unassigned");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                if (isAdded()) {
                    tvReadOnlyPurok.setText("Unavailable");
                }
            }
        });
    }

    private void submitEmergency(boolean confirmDuplicate) {
        submitEmergencyBtn.setEnabled(false);
        loadingOverlay.show();

        if (selectedPhotoUri == null) {
            dispatchEmergencyRequest(confirmDuplicate, null);
            return;
        }

        PhotoPickerHelper.createMultipartPartAsync(requireContext(), "photo", selectedPhotoUri, new PhotoPickerHelper.PartsCallback() {
            @Override
            public void onPartsReady(@NonNull List<MultipartBody.Part> parts) {
                if (!isAdded()) return;
                dispatchEmergencyRequest(confirmDuplicate, parts.isEmpty() ? null : parts.get(0));
            }

            @Override
            public void onPartsError(@NonNull String message) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                submitEmergencyBtn.setEnabled(true);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dispatchEmergencyRequest(boolean confirmDuplicate, @Nullable MultipartBody.Part photoPart) {
        String disasterKey = DISASTER_KEYS[Math.max(0, Math.min(selectedDisasterIndex, DISASTER_KEYS.length - 1))];

        List<String> needs = new ArrayList<>();
        for (int i = 0; i < chipGroupNeeds.getChildCount(); i++) {
            View child = chipGroupNeeds.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Object tag = child.getTag();
                if (tag != null) needs.add(tag.toString());
            }
        }

        MediaType textPlain = MediaType.parse("text/plain");
        RequestBody disasterBody = RequestBody.create(textPlain, disasterKey);
        RequestBody affectedBody = RequestBody.create(textPlain, String.valueOf(affectedCount));
        RequestBody needsBody = RequestBody.create(textPlain, gson.toJson(needs));
        RequestBody confirmBody = RequestBody.create(textPlain, String.valueOf(confirmDuplicate));

        repository.createEmergencyReportMultipart(
                disasterBody,
                affectedBody,
                needsBody,
                confirmBody,
                photoPart
        ).enqueue(new Callback<EmergencyReport>() {
            @Override
            public void onResponse(@NonNull Call<EmergencyReport> call, @NonNull Response<EmergencyReport> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();

                if (response.isSuccessful() && response.body() != null) {
                    submitEmergencyBtn.setEnabled(true);
                    EmergencyReport created = response.body();
                    String refId = created.getReportId() != null ? created.getReportId().toString() : "Submitted";
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Emergency Report Sent")
                            .setMessage("Your emergency report for " + created.getPurok() + " has been logged.\n\nReference ID:\n" + refId)
                            .setPositiveButton("View Reports", (dialog, which) -> navigateToEmergencyList())
                            .setCancelable(false)
                            .show();
                } else if (response.code() == 409) {
                    submitEmergencyBtn.setEnabled(true);
                    String msg = ApiErrorUtils.parseErrorMessage(
                            response,
                            "You recently submitted a similar emergency report. Submit again?"
                    );
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Similar Report Detected")
                            .setMessage(msg)
                            .setPositiveButton("Submit Again", (d, w) -> submitEmergency(true))
                            .setNegativeButton("Cancel", null)
                            .show();
                } else if (response.code() == 429) {
                    String msg = ApiErrorUtils.parseErrorMessage(response, "Submission rate limit reached. Please wait before trying again.");
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    handler.postDelayed(() -> {
                        if (isAdded() && submitEmergencyBtn != null) {
                            submitEmergencyBtn.setEnabled(true);
                        }
                    }, 10000);
                } else {
                    submitEmergencyBtn.setEnabled(true);
                    String err = ApiErrorUtils.parseErrorMessage(response, "Failed to submit emergency report.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EmergencyReport> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                submitEmergencyBtn.setEnabled(true);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToEmergencyList() {
        if (!isAdded()) return;
        if (getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).openCommunityTab(CommunityHostFragment.TAB_EMERGENCIES);
        } else {
            NavUtils.safeNavigateUp(this);
        }
    }
}
