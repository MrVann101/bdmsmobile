package com.barangay.bdms.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.barangay.bdms.R;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.network.SessionManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.resident.ResidentMainActivity;
import com.barangay.bdms.ui.staff.StaffMainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private LoadingOverlay loadingOverlay;
    private BdmsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.initToken(this);

        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn() && session.getRole() != null) {
            routeByRole(session.getRole());
            return;
        }

        setContentView(R.layout.screen_resident_r1_login);

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        TextView registerLink = findViewById(R.id.registerLink);
        TextView forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        TextView emergencyHotlinesLink = findViewById(R.id.emergencyHotlinesLink);

        repository = new BdmsRepository(this);

        loginButton.setOnClickListener(v -> performLogin());

        if (registerLink != null) {
            registerLink.setOnClickListener(v ->
                    startActivity(new Intent(this, OnboardingActivity.class))
            );
        }

        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnClickListener(v -> showForgotPasswordInfo());
        }

        if (emergencyHotlinesLink != null) {
            emergencyHotlinesLink.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Barangay Emergency Hotlines")
                            .setMessage(R.string.barangay_emergency_hotlines)
                            .setPositiveButton("Close", null)
                            .show()
            );
        }
    }

    private void showForgotPasswordInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Password Assistance")
                .setMessage("Self-service password reset is not yet available in v1. Please visit the Barangay Hall help desk with a valid ID so staff or an administrator can assist your account.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void performLogin() {
        clearErrors();

        String email = emailInput != null && emailInput.getText() != null
                ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null && passwordInput.getText() != null
                ? passwordInput.getText().toString() : "";

        boolean valid = true;
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (emailInputLayout != null) {
                emailInputLayout.setError("Enter a valid registered email address");
            }
            valid = false;
        }
        if (password.isEmpty()) {
            if (passwordInputLayout != null) {
                passwordInputLayout.setError("Enter your password");
            }
            valid = false;
        }
        if (!valid) return;

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        setLoading(true);
        repository.login(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, Object>> call,
                    @NonNull Response<Map<String, Object>> response
            ) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    String token = asString(data.get("token"));
                    Object rawUser = data.get("user");
                    Map<?, ?> user = rawUser instanceof Map ? (Map<?, ?>) rawUser : null;

                    String userId = user != null ? asString(user.get("id")) : null;
                    String role = user != null ? asString(user.get("role")) : null;
                    String purok = user != null ? asString(user.get("purok")) : null;
                    String fullName = user != null ? asString(user.get("fullName")) : null;
                    String userEmail = user != null ? asString(user.get("email")) : email;

                    if (token == null || token.isEmpty() || role == null || role.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Invalid login response from server.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ApiClient.saveSession(LoginActivity.this, token, userId, role, purok, fullName, userEmail);
                    routeByRole(role);
                } else {
                    String errMsg = ApiErrorUtils.parseErrorMessage(
                            response,
                            response.code() == 403
                                    ? "Your account has been deactivated."
                                    : "Invalid email or password."
                    );
                    if (passwordInputLayout != null) {
                        passwordInputLayout.setError(errMsg);
                    } else {
                        Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                Toast.makeText(
                        LoginActivity.this,
                        "Unable to reach server. Please check your connection and try again.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void routeByRole(@NonNull String role) {
        Intent intent;
        if ("staff".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
            intent = new Intent(this, StaffMainActivity.class);
        } else {
            intent = new Intent(this, ResidentMainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        if (emailInputLayout != null) emailInputLayout.setError(null);
        if (passwordInputLayout != null) passwordInputLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        if (loadingOverlay != null) {
            if (loading) loadingOverlay.show();
            else loadingOverlay.hide();
        }
    }

    private static String asString(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }
}
