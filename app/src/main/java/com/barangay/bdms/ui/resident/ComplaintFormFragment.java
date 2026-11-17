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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComplaintFormFragment extends Fragment {

    private static final String STATE_CATEGORY_INDEX = "complaint_category_idx";
    private static final String STATE_PHOTO_URIS = "complaint_photo_uris";

    private static final String[] CATEGORY_LABELS = {
            "Noise Disturbance (noise)",
            "Sanitation & Waste (sanitation)",
            "Dispute / Mediation (dispute)",
            "Infrastructure & Utilities (infrastructure)",
            "Other Concern (other)"
    };
    private static final String[] CATEGORY_KEYS = {
            "noise", "sanitation", "dispute", "infrastructure", "other"
    };

    private BdmsRepository repository;
    private PhotoPickerHelper photoPickerHelper;

    private TextInputLayout tilCategory, tilDescription;
    private AutoCompleteTextView actvCategory;
    private TextInputEditText etDescription;
    private TextView tvPhotosCount;
    private MaterialButton btnAttachPhoto, btnClearPhotos, submitComplaintBtn;
    private LoadingOverlay loadingOverlay;

    private int selectedCategoryIndex = 0;
    private final ArrayList<Uri> attachedPhotoUris = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedCategoryIndex = savedInstanceState.getInt(STATE_CATEGORY_INDEX, 0);
            ArrayList<Uri> savedUris = savedInstanceState.getParcelableArrayList(STATE_PHOTO_URIS);
            if (savedUris != null) {
                attachedPhotoUris.clear();
                attachedPhotoUris.addAll(savedUris);
            }
        }
        photoPickerHelper = new PhotoPickerHelper(this, new PhotoPickerHelper.OnPhotoPickedListener() {
            @Override
            public void onPhotoPicked(@NonNull Uri uri) {
                if (!isAdded()) return;
                if (attachedPhotoUris.size() >= 3) {
                    Toast.makeText(requireContext(), "Maximum 3 photos allowed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                attachedPhotoUris.add(uri);
                updatePhotoSummary();
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
        outState.putInt(STATE_CATEGORY_INDEX, selectedCategoryIndex);
        outState.putParcelableArrayList(STATE_PHOTO_URIS, attachedPhotoUris);
        if (photoPickerHelper != null) {
            photoPickerHelper.saveInstanceState(outState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r8_complaint_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        tilCategory = view.findViewById(R.id.tilCategory);
        tilDescription = view.findViewById(R.id.tilDescription);
        actvCategory = view.findViewById(R.id.actvCategory);
        etDescription = view.findViewById(R.id.etDescription);
        tvPhotosCount = view.findViewById(R.id.tvComplaintPhotosCount);
        btnAttachPhoto = view.findViewById(R.id.btnAttachComplaintPhoto);
        btnClearPhotos = view.findViewById(R.id.btnClearComplaintPhotos);
        submitComplaintBtn = view.findViewById(R.id.submitComplaintBtn);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                CATEGORY_LABELS
        );
        actvCategory.setAdapter(adapter);
        int clamped = Math.max(0, Math.min(selectedCategoryIndex, CATEGORY_LABELS.length - 1));
        actvCategory.setText(CATEGORY_LABELS[clamped], false);
        actvCategory.setOnItemClickListener((parent, itemView, position, id) -> selectedCategoryIndex = position);

        btnBack.setOnClickListener(v -> NavUtils.safeNavigateUp(this));
        btnAttachPhoto.setOnClickListener(v -> photoPickerHelper.showPickerDialog(requireContext()));
        btnClearPhotos.setOnClickListener(v -> {
            attachedPhotoUris.clear();
            updatePhotoSummary();
        });
        submitComplaintBtn.setOnClickListener(v -> submitComplaint());

        updatePhotoSummary();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void updatePhotoSummary() {
        if (tvPhotosCount == null) return;
        int count = attachedPhotoUris.size();
        if (count == 0) {
            tvPhotosCount.setText("No photos attached");
            btnClearPhotos.setVisibility(View.GONE);
            btnAttachPhoto.setEnabled(true);
        } else {
            tvPhotosCount.setText(count + " photo" + (count > 1 ? "s" : "") + " attached");
            btnClearPhotos.setVisibility(View.VISIBLE);
            btnAttachPhoto.setEnabled(count < 3);
        }
    }

    private void submitComplaint() {
        tilCategory.setError(null);
        tilDescription.setError(null);

        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            tilDescription.setError("Please describe your complaint");
            return;
        }

        String categoryKey = CATEGORY_KEYS[Math.max(0, Math.min(selectedCategoryIndex, CATEGORY_KEYS.length - 1))];
        MediaType textPlain = MediaType.parse("text/plain");
        RequestBody catBody = RequestBody.create(textPlain, categoryKey);
        RequestBody descBody = RequestBody.create(textPlain, description);

        submitComplaintBtn.setEnabled(false);
        loadingOverlay.show();

        PhotoPickerHelper.createMultipartPartsAsync(requireContext(), "photos", attachedPhotoUris, new PhotoPickerHelper.PartsCallback() {
            @Override
            public void onPartsReady(@NonNull List<MultipartBody.Part> photoParts) {
                if (!isAdded()) return;
                repository.createComplaint(catBody, descBody, photoParts).enqueue(new Callback<Complaint>() {
                    @Override
                    public void onResponse(@NonNull Call<Complaint> call, @NonNull Response<Complaint> response) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();

                        if (response.isSuccessful()) {
                            submitComplaintBtn.setEnabled(true);
                            Toast.makeText(requireContext(), "Complaint submitted!", Toast.LENGTH_LONG).show();
                            NavUtils.safeNavigateUp(ComplaintFormFragment.this);
                        } else if (response.code() == 429) {
                            String msg = ApiErrorUtils.parseErrorMessage(response, "Too many submissions. Please wait before submitting again.");
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                            handler.postDelayed(() -> {
                                if (isAdded() && submitComplaintBtn != null) {
                                    submitComplaintBtn.setEnabled(true);
                                }
                            }, 10000);
                        } else {
                            submitComplaintBtn.setEnabled(true);
                            String err = ApiErrorUtils.parseErrorMessage(response, "Unable to file complaint.");
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Complaint> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();
                        submitComplaintBtn.setEnabled(true);
                        Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onPartsError(@NonNull String message) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                submitComplaintBtn.setEnabled(true);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
