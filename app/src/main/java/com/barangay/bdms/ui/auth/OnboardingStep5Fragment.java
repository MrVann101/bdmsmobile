package com.barangay.bdms.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class OnboardingStep5Fragment extends Fragment {

    private OnboardingViewModel viewModel;
    private MaterialCheckBox cbConsent;
    private TextView tvConsentError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step5_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        cbConsent = view.findViewById(R.id.cbConsent);
        tvConsentError = view.findViewById(R.id.tvConsentError);
        MaterialButton btnBack = view.findViewById(R.id.btnStep5Back);
        MaterialButton btnNext = view.findViewById(R.id.btnStep5Next);

        cbConsent.setChecked(viewModel.consentAccepted);
        cbConsent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvConsentError.setVisibility(View.GONE);
            }
        });

        btnBack.setOnClickListener(v -> {
            viewModel.consentAccepted = cbConsent.isChecked();
            ((OnboardingActivity) requireActivity()).goBackStep();
        });

        btnNext.setOnClickListener(v -> {
            if (!cbConsent.isChecked()) {
                tvConsentError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.markConsentAccepted();
            ((OnboardingActivity) requireActivity()).goToNextStep();
        });
    }
}
