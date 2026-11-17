package com.barangay.bdms.ui.auth;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Profile;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingStep6Fragment extends Fragment {

    private OnboardingViewModel viewModel;
    private BdmsRepository repository;
    private TextView tvOtpEmailInfo;
    private TextInputLayout tilOtpCode;
    private TextInputEditText etOtpCode;
    private MaterialButton btnSendOtp;
    private MaterialButton btnVerifyAndComplete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step6_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);
        repository = new BdmsRepository(requireContext());

        tvOtpEmailInfo = view.findViewById(R.id.tvOtpEmailInfo);
        tilOtpCode = view.findViewById(R.id.tilOtpCode);
        etOtpCode = view.findViewById(R.id.etOtpCode);
        btnSendOtp = view.findViewById(R.id.btnSendOtp);
        btnVerifyAndComplete = view.findViewById(R.id.btnVerifyAndComplete);
        MaterialButton btnBack = view.findViewById(R.id.btnStep6Back);

        updateEmailHint();

        btnSendOtp.setOnClickListener(v -> sendOtpCode());
        btnBack.setOnClickListener(v -> ((OnboardingActivity) requireActivity()).goBackStep());
        btnVerifyAndComplete.setOnClickListener(v -> verifyOtpAndRegister());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEmailHint();
    }

    private void updateEmailHint() {
        if (tvOtpEmailInfo != null && viewModel != null) {
            String target = viewModel.email != null && !viewModel.email.isEmpty()
                    ? viewModel.email
                    : "your email address";
            tvOtpEmailInfo.setText("Tap \"Send Verification Code\" to receive a 6-digit OTP at " + target + ".");
            btnSendOtp.setText(viewModel.otpSent ? "Resend Verification Code" : "Send Verification Code");
        }
    }

    private void sendOtpCode() {
        if (viewModel.email == null || viewModel.email.isEmpty()) {
            Toast.makeText(requireContext(), "Go back to Step 1 and enter your email first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", viewModel.email);
        body.put("purpose", "registration");

        setBusy(true);
        repository.sendOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                setBusy(false);
                if (response.isSuccessful()) {
                    viewModel.otpSent = true;
                    viewModel.otpVerificationToken = null;
                    viewModel.verifiedOtpEmail = null;
                    btnSendOtp.setText("Resend Verification Code");
                    Toast.makeText(requireContext(), "Verification code sent to " + viewModel.email, Toast.LENGTH_LONG).show();
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to send verification code.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setBusy(false);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyOtpAndRegister() {
        tilOtpCode.setError(null);

        if (viewModel.otpVerificationToken != null
                && !viewModel.otpVerificationToken.isEmpty()
                && viewModel.email.equalsIgnoreCase(viewModel.verifiedOtpEmail)) {
            setBusy(true);
            performRegistration();
            return;
        }

        String code = etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : "";
        if (!code.matches("\\d{6}")) {
            tilOtpCode.setError("Enter the 6-digit verification code");
            return;
        }

        Map<String, String> otpBody = new HashMap<>();
        otpBody.put("email", viewModel.email);
        otpBody.put("code", code);
        otpBody.put("purpose", "registration");

        setBusy(true);
        repository.verifyOtp(otpBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        viewModel.otpVerificationToken = asString(response.body().get("otpVerificationToken"));
                        viewModel.verifiedOtpEmail = viewModel.email;
                    }
                    performRegistration();
                } else {
                    setBusy(false);
                    String err = ApiErrorUtils.parseErrorMessage(response, "Invalid or expired verification code.");
                    tilOtpCode.setError(err);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setBusy(false);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performRegistration() {
        Map<String, Object> payload = viewModel.buildRegistrationPayload();
        repository.register(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    String token = asString(data.get("token"));
                    Map<?, ?> user = data.get("user") instanceof Map ? (Map<?, ?>) data.get("user") : null;
                    String userId = user != null ? asString(user.get("id")) : null;
                    String role = user != null ? asString(user.get("role")) : "resident";
                    String purok = user != null ? asString(user.get("purok")) : viewModel.purok;
                    String fullName = user != null ? asString(user.get("fullName")) : viewModel.fullName;
                    String email = user != null ? asString(user.get("email")) : viewModel.email;

                    if (token == null || token.isEmpty()) {
                        setBusy(false);
                        Toast.makeText(requireContext(), "Registration failed: missing authentication token.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ApiClient.saveSession(requireContext(), token, userId, role, purok, fullName, email);

                    if (viewModel.avatarUri != null) {
                        uploadOptionalAvatarAndFinish(viewModel.avatarUri);
                    } else {
                        setBusy(false);
                        Toast.makeText(requireContext(), "Account created! Welcome to BDMS Mobile.", Toast.LENGTH_LONG).show();
                        ((OnboardingActivity) requireActivity()).completeOnboardingToResidentHome();
                    }
                } else {
                    setBusy(false);
                    String err = ApiErrorUtils.parseErrorMessage(response, "Registration failed. Please check your entries.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setBusy(false);
                Toast.makeText(requireContext(), "Network error during registration: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadOptionalAvatarAndFinish(@NonNull Uri avatarUri) {
        PhotoPickerHelper.createMultipartPartAsync(requireContext(), "avatar", avatarUri, new PhotoPickerHelper.PartsCallback() {
            @Override
            public void onPartsReady(@NonNull List<MultipartBody.Part> parts) {
                if (!isAdded() || parts.isEmpty()) return;
                repository.uploadAvatar(parts.get(0)).enqueue(new Callback<Profile>() {
                    @Override
                    public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                        if (!isAdded()) return;
                        setBusy(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Account created! Welcome to BDMS Mobile.", Toast.LENGTH_LONG).show();
                        } else {
                            String err = ApiErrorUtils.parseErrorMessage(response, "Photo upload failed");
                            Toast.makeText(requireContext(), "Account created, but profile photo could not be uploaded (" + err + ").", Toast.LENGTH_LONG).show();
                        }
                        ((OnboardingActivity) requireActivity()).completeOnboardingToResidentHome();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        setBusy(false);
                        Toast.makeText(requireContext(), "Account created, but profile photo upload failed. You can update it in Profile.", Toast.LENGTH_LONG).show();
                        ((OnboardingActivity) requireActivity()).completeOnboardingToResidentHome();
                    }
                });
            }

            @Override
            public void onPartsError(@NonNull String message) {
                if (!isAdded()) return;
                setBusy(false);
                Toast.makeText(requireContext(), "Account created (profile photo skipped: " + message + ")", Toast.LENGTH_LONG).show();
                ((OnboardingActivity) requireActivity()).completeOnboardingToResidentHome();
            }
        });
    }

    private void setBusy(boolean busy) {
        btnSendOtp.setEnabled(!busy);
        btnVerifyAndComplete.setEnabled(!busy);
        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).setLoading(busy);
        }
    }

    @Nullable
    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
