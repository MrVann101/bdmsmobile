package com.barangay.bdms.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.barangay.bdms.R;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.resident.ResidentMainActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class OnboardingActivity extends AppCompatActivity {

    private static final int TOTAL_STEPS = 6;
    private static final String[] STEP_TITLES = {
            "Account Credentials",
            "Personal Information",
            "Barangay Residency",
            "Additional & Photo",
            "Data Privacy Consent",
            "Email Verification"
    };

    private ViewPager2 pager;
    private TextView tvStepTitle;
    private TextView tvStepIndicator;
    private LinearProgressIndicator progressIndicator;
    private LoadingOverlay loadingOverlay;
    private OnboardingViewModel viewModel;
    private BdmsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ApiClient.initToken(this);
        repository = new BdmsRepository(this);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        viewModel.loadPuroks(repository);

        pager = findViewById(R.id.onboardingPager);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        progressIndicator = findViewById(R.id.onboardingProgress);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ImageButton btnBack = findViewById(R.id.btnOnboardingBack);

        pager.setUserInputEnabled(false);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return new OnboardingStep1Fragment();
                    case 1: return new OnboardingStep2Fragment();
                    case 2: return new OnboardingStep3Fragment();
                    case 3: return new OnboardingStep4Fragment();
                    case 4: return new OnboardingStep5Fragment();
                    case 5:
                    default: return new OnboardingStep6Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return TOTAL_STEPS;
            }
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeader(position);
            }
        });

        btnBack.setOnClickListener(v -> goBackStep());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackStep();
            }
        });

        updateHeader(0);
    }

    public void goToNextStep() {
        int current = pager.getCurrentItem();
        if (current < TOTAL_STEPS - 1) {
            pager.setCurrentItem(current + 1, true);
        }
    }

    public void goBackStep() {
        int current = pager.getCurrentItem();
        if (current > 0) {
            pager.setCurrentItem(current - 1, true);
        } else {
            finish();
        }
    }

    public void setLoading(boolean loading) {
        if (loadingOverlay != null) {
            if (loading) loadingOverlay.show();
            else loadingOverlay.hide();
        }
    }

    public void completeOnboardingToResidentHome() {
        Intent intent = new Intent(this, ResidentMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateHeader(int position) {
        int step = position + 1;
        tvStepTitle.setText(STEP_TITLES[position]);
        tvStepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
        progressIndicator.setProgress(step);
    }
}
