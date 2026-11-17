package com.barangay.bdms.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Purok;
import com.barangay.bdms.repository.BdmsRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class OnboardingStep3Fragment extends Fragment {

    private static final String[] RESIDENCY_TYPES = {
            "Owner", "Renter", "Living with Relatives", "Government Housing", "Others"
    };

    private OnboardingViewModel viewModel;
    private BdmsRepository repository;
    private TextInputLayout tilPurok, tilAddress, tilResidencyType;
    private AutoCompleteTextView actvPurok, actvResidencyType;
    private TextInputEditText etAddress, etResidencyPeriod;
    private ChipGroup chipGroupSector;
    private LinearLayout layoutPurokRetry;
    private TextView tvPurokLoadError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step3_residency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);
        repository = new BdmsRepository(requireContext());

        tilPurok = view.findViewById(R.id.tilPurok);
        tilAddress = view.findViewById(R.id.tilAddress);
        tilResidencyType = view.findViewById(R.id.tilResidencyType);
        actvPurok = view.findViewById(R.id.actvPurok);
        actvResidencyType = view.findViewById(R.id.actvResidencyType);
        etAddress = view.findViewById(R.id.etAddress);
        etResidencyPeriod = view.findViewById(R.id.etResidencyPeriod);
        chipGroupSector = view.findViewById(R.id.chipGroupSector);
        layoutPurokRetry = view.findViewById(R.id.layoutPurokRetry);
        tvPurokLoadError = view.findViewById(R.id.tvPurokLoadError);
        MaterialButton btnRetry = view.findViewById(R.id.btnRetryPuroks);

        ArrayAdapter<String> residencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                RESIDENCY_TYPES
        );
        actvResidencyType.setAdapter(residencyAdapter);
        if (savedInstanceState == null) {
            if (!viewModel.residencyType.isEmpty()) {
                actvResidencyType.setText(viewModel.residencyType, false);
            }
            etAddress.setText(viewModel.address);
            etResidencyPeriod.setText(viewModel.residencyPeriod);
            restoreSectorChips();
        }

        viewModel.getPuroks().observe(getViewLifecycleOwner(), puroks -> {
            if (puroks == null) return;
            List<String> names = new ArrayList<>();
            for (Purok p : puroks) {
                if (p.getName() != null && !p.getName().trim().isEmpty()) {
                    names.add(p.getName().trim());
                }
            }
            ArrayAdapter<String> purokAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
            );
            actvPurok.setAdapter(purokAdapter);
            if (!viewModel.purok.isEmpty()) {
                actvPurok.setText(viewModel.purok, false);
            }
            layoutPurokRetry.setVisibility(View.GONE);
        });

        viewModel.getPurokError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                tvPurokLoadError.setText(err);
                layoutPurokRetry.setVisibility(View.VISIBLE);
            } else {
                layoutPurokRetry.setVisibility(View.GONE);
            }
        });

        btnRetry.setOnClickListener(v -> viewModel.loadPuroks(repository));

        MaterialButton btnBack = view.findViewById(R.id.btnStep3Back);
        MaterialButton btnNext = view.findViewById(R.id.btnStep3Next);

        btnBack.setOnClickListener(v -> {
            saveDraft();
            ((OnboardingActivity) requireActivity()).goBackStep();
        });
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    @Override
    public void onPause() {
        saveDraft();
        super.onPause();
    }

    private void restoreSectorChips() {
        for (int i = 0; i < chipGroupSector.getChildCount(); i++) {
            View child = chipGroupSector.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(viewModel.sector.contains(chip.getText().toString()));
            }
        }
    }

    private void saveDraft() {
        if (viewModel == null || actvPurok == null) return;
        viewModel.purok = actvPurok.getText() != null ? actvPurok.getText().toString().trim() : "";
        viewModel.address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        viewModel.residencyType = actvResidencyType.getText() != null ? actvResidencyType.getText().toString().trim() : "";
        viewModel.residencyPeriod = etResidencyPeriod.getText() != null ? etResidencyPeriod.getText().toString().trim() : "";

        List<String> selectedSectors = new ArrayList<>();
        for (int i = 0; i < chipGroupSector.getChildCount(); i++) {
            View child = chipGroupSector.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                selectedSectors.add(((Chip) child).getText().toString());
            }
        }
        viewModel.sector = selectedSectors;
    }

    private void validateAndProceed() {
        tilPurok.setError(null);
        tilAddress.setError(null);
        tilResidencyType.setError(null);

        saveDraft();

        boolean valid = true;
        if (viewModel.purok.isEmpty()) {
            tilPurok.setError("Please select your Purok / Zone");
            valid = false;
        }
        if (viewModel.address.isEmpty()) {
            tilAddress.setError("Address is required");
            valid = false;
        }
        if (viewModel.residencyType.isEmpty()) {
            tilResidencyType.setError("Select your residency type");
            valid = false;
        }
        if (!valid) return;

        ((OnboardingActivity) requireActivity()).goToNextStep();
    }
}
