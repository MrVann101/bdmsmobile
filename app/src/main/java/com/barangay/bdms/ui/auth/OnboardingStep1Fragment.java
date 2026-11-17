package com.barangay.bdms.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class OnboardingStep1Fragment extends Fragment {

    private OnboardingViewModel viewModel;
    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step1_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        tilUsername = view.findViewById(R.id.tilUsername);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        if (savedInstanceState == null) {
            etUsername.setText(viewModel.username);
            etEmail.setText(viewModel.email);
            etPassword.setText(viewModel.password);
            etConfirmPassword.setText(viewModel.password);
        }

        MaterialButton btnNext = view.findViewById(R.id.btnStep1Next);
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    @Override
    public void onPause() {
        saveDraft();
        super.onPause();
    }

    private void saveDraft() {
        if (viewModel == null || etUsername == null) return;
        viewModel.username = readText(etUsername);
        viewModel.setEmailAndResetOtpIfChanged(readText(etEmail));
        viewModel.password = etPassword.getText() != null ? etPassword.getText().toString() : "";
    }

    private void validateAndProceed() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String username = readText(etUsername);
        String email = readText(etEmail);
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean valid = true;
        if (username.length() < 3) {
            tilUsername.setError("Enter a username (at least 3 characters)");
            valid = false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }
        if (!OnboardingViewModel.isValidPassword(password)) {
            tilPassword.setError("Must be 8+ chars with 1 uppercase, 1 number, and 1 special character");
            valid = false;
        }
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        }
        if (!valid) return;

        viewModel.username = username;
        viewModel.setEmailAndResetOtpIfChanged(email);
        viewModel.password = password;
        ((OnboardingActivity) requireActivity()).goToNextStep();
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
