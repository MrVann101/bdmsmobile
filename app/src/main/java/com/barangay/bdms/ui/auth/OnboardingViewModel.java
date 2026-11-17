package com.barangay.bdms.ui.auth;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.barangay.bdms.model.Purok;
import com.barangay.bdms.repository.BdmsRepository;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shared state and validation holder across the 6-step ViewPager2 onboarding flow.
 */
public class OnboardingViewModel extends ViewModel {

    // Step 1
    public String username = "";
    public String email = "";
    public String password = "";

    // Step 2
    public String fullName = "";
    public String mobileNumber = "";
    public String sex = "";
    public String birthdate = "";
    public String civilStatus = "";
    public String citizenship = "";
    public String birthplace = "";

    // Step 3
    public String purok = "";
    public String address = "";
    public String residencyType = "";
    public String residencyPeriod = "";
    public List<String> sector = new ArrayList<>();

    // Step 4
    public String ctcNumber = "";
    public String ctcOrNumber = "";
    public List<String> disabilities = new ArrayList<>();
    @Nullable
    public Uri avatarUri = null;

    // Step 5
    public boolean consentAccepted = false;
    @Nullable
    public String consentAcceptedAt = null;

    // Step 6
    public boolean otpSent = false;
    @Nullable
    public String otpVerificationToken = null;
    @Nullable
    public String verifiedOtpEmail = null;

    public void setEmailAndResetOtpIfChanged(@NonNull String nextEmail) {
        if (!nextEmail.equalsIgnoreCase(this.email)) {
            this.email = nextEmail;
            this.otpSent = false;
            this.otpVerificationToken = null;
            this.verifiedOtpEmail = null;
        }
    }

    private final MutableLiveData<List<Purok>> puroksLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> purokErrorLiveData = new MutableLiveData<>();

    public LiveData<List<Purok>> getPuroks() {
        return puroksLiveData;
    }

    public LiveData<String> getPurokError() {
        return purokErrorLiveData;
    }

    public void loadPuroks(@NonNull BdmsRepository repository) {
        purokErrorLiveData.setValue(null);
        repository.getPuroks().enqueue(new Callback<List<Purok>>() {
            @Override
            public void onResponse(@NonNull Call<List<Purok>> call, @NonNull Response<List<Purok>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    puroksLiveData.setValue(response.body());
                } else {
                    purokErrorLiveData.setValue("Failed to load Puroks from server.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Purok>> call, @NonNull Throwable t) {
                purokErrorLiveData.setValue("Unable to reach server to load Puroks.");
            }
        });
    }

    public void markConsentAccepted() {
        consentAccepted = true;
        consentAcceptedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    public Map<String, Object> buildRegistrationPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("fullName", fullName);
        if (!mobileNumber.isEmpty()) payload.put("mobileNumber", mobileNumber);
        if (!sex.isEmpty()) payload.put("sex", sex);
        if (!birthdate.isEmpty()) payload.put("birthdate", birthdate);
        if (!civilStatus.isEmpty()) payload.put("civilStatus", civilStatus);
        if (!citizenship.isEmpty()) payload.put("citizenship", citizenship);
        if (!birthplace.isEmpty()) payload.put("birthplace", birthplace);
        payload.put("purok", purok);
        if (!address.isEmpty()) payload.put("address", address);
        if (!residencyType.isEmpty()) payload.put("residencyType", residencyType);
        if (!residencyPeriod.isEmpty()) payload.put("residencyPeriod", residencyPeriod);
        if (!sector.isEmpty()) payload.put("sector", sector);
        if (!ctcNumber.isEmpty()) payload.put("ctcNumber", ctcNumber);
        if (!ctcOrNumber.isEmpty()) payload.put("ctcOrNumber", ctcOrNumber);
        if (!disabilities.isEmpty()) payload.put("disabilities", disabilities);
        payload.put("consentAccepted", consentAccepted);
        if (consentAcceptedAt != null) payload.put("consentAcceptedAt", consentAcceptedAt);
        if (otpVerificationToken != null && !otpVerificationToken.isEmpty()) {
            payload.put("otpVerificationToken", otpVerificationToken);
        }
        return payload;
    }

    /**
     * Enforces password policy: >= 8 chars, at least 1 uppercase, 1 digit, and 1 special char.
     */
    public static boolean isValidPassword(@Nullable String candidate) {
        if (candidate == null || candidate.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasDigit && hasSpecial;
    }
}
