package com.barangay.bdms.ui.auth;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class OnboardingStep4Fragment extends Fragment {

    private OnboardingViewModel viewModel;
    private PhotoPickerHelper photoPickerHelper;
    private TextInputEditText etCtcNumber, etCtcOrNumber;
    private SwitchMaterial switchDisabilities;
    private ChipGroup chipGroupDisabilities;
    private ImageView ivAvatarPreview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPickerHelper = new PhotoPickerHelper(this, new PhotoPickerHelper.OnPhotoPickedListener() {
            @Override
            public void onPhotoPicked(@NonNull Uri uri) {
                if (!isAdded()) return;
                OnboardingViewModel vm = viewModel != null
                        ? viewModel
                        : new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);
                vm.avatarUri = uri;
                if (ivAvatarPreview != null) {
                    ivAvatarPreview.setImageURI(uri);
                }
            }

            @Override
            public void onPhotoError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        photoPickerHelper.restoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoPickerHelper != null) {
            photoPickerHelper.saveInstanceState(outState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step4_supplementary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        etCtcNumber = view.findViewById(R.id.etCtcNumber);
        etCtcOrNumber = view.findViewById(R.id.etCtcOrNumber);
        switchDisabilities = view.findViewById(R.id.switchHasDisabilities);
        chipGroupDisabilities = view.findViewById(R.id.chipGroupDisabilities);
        ivAvatarPreview = view.findViewById(R.id.ivAvatarPreview);
        MaterialButton btnPickAvatar = view.findViewById(R.id.btnPickAvatar);
        MaterialButton btnBack = view.findViewById(R.id.btnStep4Back);
        MaterialButton btnNext = view.findViewById(R.id.btnStep4Next);

        if (savedInstanceState == null) {
            etCtcNumber.setText(viewModel.ctcNumber);
            etCtcOrNumber.setText(viewModel.ctcOrNumber);

            boolean hasDisabilities = !viewModel.disabilities.isEmpty();
            switchDisabilities.setChecked(hasDisabilities);
            chipGroupDisabilities.setVisibility(hasDisabilities ? View.VISIBLE : View.GONE);
            for (int i = 0; i < chipGroupDisabilities.getChildCount(); i++) {
                View child = chipGroupDisabilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setChecked(viewModel.disabilities.contains(chip.getText().toString()));
                }
            }
        } else {
            chipGroupDisabilities.setVisibility(switchDisabilities.isChecked() ? View.VISIBLE : View.GONE);
        }

        if (viewModel.avatarUri != null) {
            ivAvatarPreview.setImageURI(viewModel.avatarUri);
        }

        switchDisabilities.setOnCheckedChangeListener((buttonView, isChecked) ->
                chipGroupDisabilities.setVisibility(isChecked ? View.VISIBLE : View.GONE)
        );

        btnPickAvatar.setOnClickListener(v -> photoPickerHelper.showPickerDialog(requireContext()));

        btnBack.setOnClickListener(v -> {
            saveDraft();
            ((OnboardingActivity) requireActivity()).goBackStep();
        });

        btnNext.setOnClickListener(v -> {
            saveDraft();
            ((OnboardingActivity) requireActivity()).goToNextStep();
        });
    }

    @Override
    public void onPause() {
        saveDraft();
        super.onPause();
    }

    private void saveDraft() {
        if (viewModel == null || etCtcNumber == null) return;
        viewModel.ctcNumber = etCtcNumber.getText() != null ? etCtcNumber.getText().toString().trim() : "";
        viewModel.ctcOrNumber = etCtcOrNumber.getText() != null ? etCtcOrNumber.getText().toString().trim() : "";

        List<String> selectedDisabilities = new ArrayList<>();
        if (switchDisabilities.isChecked()) {
            for (int i = 0; i < chipGroupDisabilities.getChildCount(); i++) {
                View child = chipGroupDisabilities.getChildAt(i);
                if (child instanceof Chip && ((Chip) child).isChecked()) {
                    selectedDisabilities.add(((Chip) child).getText().toString());
                }
            }
        }
        viewModel.disabilities = selectedDisabilities;
    }
}
