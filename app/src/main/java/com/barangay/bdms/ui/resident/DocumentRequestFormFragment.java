package com.barangay.bdms.ui.resident;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.barangay.bdms.R;
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.model.DocumentTypeInfo;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.barangay.bdms.ui.common.StatusBadge;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentRequestFormFragment extends Fragment {

    private static final String STATE_SELECTED_TYPE = "selected_document_type";
    private static final String STATE_CLEARANCE_URI = "selected_clearance_uri";

    private BdmsRepository repository;
    private PhotoPickerHelper photoPickerHelper;

    private TextInputLayout tilDocumentType, tilPurpose, tilBusinessName, tilBusinessAddress, tilBusinessType;
    private AutoCompleteTextView actvDocumentType;
    private TextInputEditText etPurpose, etBusinessName, etBusinessAddress, etBusinessType;
    private LinearLayout layoutBusinessFields, layoutTypesError;
    private RadioGroup rgDeliveryMethod;
    private TextView tvFeePreview, tvSubmitHelper, tvTypesError;
    private StatusBadge badgeClearanceStatus;
    private ImageView ivClearancePreview;
    private MaterialButton btnUploadClearance, submitRequestBtn;
    private LoadingOverlay loadingOverlay;

    private final List<DocumentTypeInfo> documentTypes = new ArrayList<>();
    @Nullable
    private DocumentTypeInfo selectedType = null;
    @Nullable
    private String pendingRestoreTypeName = null;
    @Nullable
    private Uri selectedClearanceUri = null;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pendingRestoreTypeName = savedInstanceState.getString(STATE_SELECTED_TYPE);
            selectedClearanceUri = savedInstanceState.getParcelable(STATE_CLEARANCE_URI);
        }
        photoPickerHelper = new PhotoPickerHelper(this, new PhotoPickerHelper.OnPhotoPickedListener() {
            @Override
            public void onPhotoPicked(@NonNull Uri uri) {
                selectedClearanceUri = uri;
                if (isAdded() && getView() != null) {
                    updateClearanceAttachedUi();
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
        if (selectedType != null) {
            outState.putString(STATE_SELECTED_TYPE, selectedType.getDocumentType());
        } else if (pendingRestoreTypeName != null) {
            outState.putString(STATE_SELECTED_TYPE, pendingRestoreTypeName);
        }
        if (selectedClearanceUri != null) {
            outState.putParcelable(STATE_CLEARANCE_URI, selectedClearanceUri);
        }
        if (photoPickerHelper != null) {
            photoPickerHelper.saveInstanceState(outState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r5_document_request_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        tilDocumentType = view.findViewById(R.id.tilDocumentType);
        tilPurpose = view.findViewById(R.id.tilPurpose);
        tilBusinessName = view.findViewById(R.id.tilBusinessName);
        tilBusinessAddress = view.findViewById(R.id.tilBusinessAddress);
        tilBusinessType = view.findViewById(R.id.tilBusinessType);

        actvDocumentType = view.findViewById(R.id.actvDocumentType);
        etPurpose = view.findViewById(R.id.etPurpose);
        etBusinessName = view.findViewById(R.id.etBusinessName);
        etBusinessAddress = view.findViewById(R.id.etBusinessAddress);
        etBusinessType = view.findViewById(R.id.etBusinessType);
        layoutBusinessFields = view.findViewById(R.id.layoutBusinessFields);
        layoutTypesError = view.findViewById(R.id.layoutTypesError);
        tvTypesError = view.findViewById(R.id.tvTypesError);
        MaterialButton btnRetryTypes = view.findViewById(R.id.btnRetryTypes);
        rgDeliveryMethod = view.findViewById(R.id.rgDeliveryMethod);
        tvFeePreview = view.findViewById(R.id.tvFeePreview);
        badgeClearanceStatus = view.findViewById(R.id.badgeClearanceStatus);
        tvSubmitHelper = view.findViewById(R.id.tvSubmitHelper);
        ivClearancePreview = view.findViewById(R.id.ivClearancePreview);
        btnUploadClearance = view.findViewById(R.id.btnUploadClearance);
        submitRequestBtn = view.findViewById(R.id.submitRequestBtn);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        // Hard business rule: Submit stays disabled until Purok Clearance photo is attached
        updateClearanceAttachedUi();

        btnBack.setOnClickListener(v -> NavUtils.safeNavigateUp(this));
        btnUploadClearance.setOnClickListener(v -> photoPickerHelper.showPickerDialog(requireContext()));
        btnRetryTypes.setOnClickListener(v -> loadDocumentTypes());
        submitRequestBtn.setOnClickListener(v -> submitRequest());

        actvDocumentType.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < documentTypes.size()) {
                selectedType = documentTypes.get(position);
                pendingRestoreTypeName = selectedType.getDocumentType();
                onDocumentTypeChanged();
            }
        });

        loadDocumentTypes();
    }

    private void loadDocumentTypes() {
        layoutTypesError.setVisibility(View.GONE);
        loadingOverlay.show();
        repository.getDocumentTypes().enqueue(new Callback<List<DocumentTypeInfo>>() {
            @Override
            public void onResponse(@NonNull Call<List<DocumentTypeInfo>> call, @NonNull Response<List<DocumentTypeInfo>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    documentTypes.clear();
                    documentTypes.addAll(response.body());
                    ArrayAdapter<DocumentTypeInfo> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            documentTypes
                    );
                    actvDocumentType.setAdapter(adapter);

                    DocumentTypeInfo matched = null;
                    String targetType = selectedType != null ? selectedType.getDocumentType() : pendingRestoreTypeName;
                    if (targetType != null) {
                        for (DocumentTypeInfo info : documentTypes) {
                            if (targetType.equalsIgnoreCase(info.getDocumentType())) {
                                matched = info;
                                break;
                            }
                        }
                    }
                    selectedType = matched != null ? matched : documentTypes.get(0);
                    actvDocumentType.setText(selectedType.getDocumentType(), false);
                    onDocumentTypeChanged();
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Failed to load certificate types.");
                    tvTypesError.setText(err);
                    layoutTypesError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DocumentTypeInfo>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                tvTypesError.setText("Unable to load certificate types from server.");
                layoutTypesError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onDocumentTypeChanged() {
        if (selectedType == null) return;
        BigDecimal fee = selectedType.getFee() != null ? selectedType.getFee() : BigDecimal.ZERO;
        tvFeePreview.setText(String.format(Locale.US, "PHP %.2f", fee.doubleValue()));
        boolean isBusiness = "Business Clearance".equalsIgnoreCase(selectedType.getDocumentType());
        layoutBusinessFields.setVisibility(isBusiness ? View.VISIBLE : View.GONE);
    }

    private void updateClearanceAttachedUi() {
        if (!isAdded() || submitRequestBtn == null) return;
        if (selectedClearanceUri != null) {
            ivClearancePreview.setVisibility(View.VISIBLE);
            ivClearancePreview.setImageURI(selectedClearanceUri);
            btnUploadClearance.setText("Change Purok Clearance Photo");
            badgeClearanceStatus.setStatus("completed", "Attached");
            submitRequestBtn.setEnabled(true);
            tvSubmitHelper.setVisibility(View.GONE);
        } else {
            ivClearancePreview.setVisibility(View.GONE);
            btnUploadClearance.setText("Attach Purok Clearance Photo");
            badgeClearanceStatus.setStatus("rejected", "Required");
            submitRequestBtn.setEnabled(false);
            tvSubmitHelper.setVisibility(View.VISIBLE);
        }
    }

    private void submitRequest() {
        tilDocumentType.setError(null);
        tilPurpose.setError(null);
        tilBusinessName.setError(null);
        tilBusinessAddress.setError(null);
        tilBusinessType.setError(null);

        if (selectedClearanceUri == null) {
            Toast.makeText(requireContext(), "Please attach your Purok Clearance photo first.", Toast.LENGTH_SHORT).show();
            submitRequestBtn.setEnabled(false);
            return;
        }
        if (selectedType == null) {
            tilDocumentType.setError("Select a document type");
            return;
        }

        String purpose = etPurpose.getText() != null ? etPurpose.getText().toString().trim() : "";
        if (purpose.isEmpty()) {
            tilPurpose.setError("Enter the purpose of your request");
            return;
        }

        boolean isBusiness = "Business Clearance".equalsIgnoreCase(selectedType.getDocumentType());
        String bizName = etBusinessName.getText() != null ? etBusinessName.getText().toString().trim() : "";
        String bizAddress = etBusinessAddress.getText() != null ? etBusinessAddress.getText().toString().trim() : "";
        String bizType = etBusinessType.getText() != null ? etBusinessType.getText().toString().trim() : "";

        if (isBusiness) {
            boolean bizValid = true;
            if (bizName.isEmpty()) {
                tilBusinessName.setError("Business name is required");
                bizValid = false;
            }
            if (bizAddress.isEmpty()) {
                tilBusinessAddress.setError("Business address is required");
                bizValid = false;
            }
            if (bizType.isEmpty()) {
                tilBusinessType.setError("Business type is required");
                bizValid = false;
            }
            if (!bizValid) return;
        }

        String delivery = resolveDeliveryMethod();
        MediaType textPlain = MediaType.parse("text/plain");
        RequestBody typeBody = RequestBody.create(textPlain, selectedType.getDocumentType());
        RequestBody purposeBody = RequestBody.create(textPlain, purpose);
        RequestBody deliveryBody = RequestBody.create(textPlain, delivery);
        RequestBody bizNameBody = isBusiness ? RequestBody.create(textPlain, bizName) : null;
        RequestBody bizAddrBody = isBusiness ? RequestBody.create(textPlain, bizAddress) : null;
        RequestBody bizTypeBody = isBusiness ? RequestBody.create(textPlain, bizType) : null;

        submitRequestBtn.setEnabled(false);
        loadingOverlay.show();

        PhotoPickerHelper.createMultipartPartAsync(requireContext(), "purokClearance", selectedClearanceUri, new PhotoPickerHelper.PartsCallback() {
            @Override
            public void onPartsReady(@NonNull List<MultipartBody.Part> parts) {
                if (!isAdded() || parts.isEmpty()) return;
                repository.createDocumentRequest(
                        typeBody,
                        purposeBody,
                        deliveryBody,
                        bizNameBody,
                        bizAddrBody,
                        bizTypeBody,
                        parts.get(0)
                ).enqueue(new Callback<DocumentRequest>() {
                    @Override
                    public void onResponse(@NonNull Call<DocumentRequest> call, @NonNull Response<DocumentRequest> response) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();
                        submitRequestBtn.setEnabled(selectedClearanceUri != null);

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Document request submitted!", Toast.LENGTH_LONG).show();
                            NavUtils.safeNavigateUp(DocumentRequestFormFragment.this);
                        } else {
                            String err = ApiErrorUtils.parseErrorMessage(response, "Unable to submit document request.");
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DocumentRequest> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();
                        submitRequestBtn.setEnabled(selectedClearanceUri != null);
                        Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onPartsError(@NonNull String message) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                submitRequestBtn.setEnabled(selectedClearanceUri != null);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    private String resolveDeliveryMethod() {
        int checkedId = rgDeliveryMethod.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDeliveryEmail) return "email";
        if (checkedId == R.id.rbDeliveryBoth) return "both";
        return "pickup";
    }
}
