package com.barangay.bdms.ui.auth;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public class OnboardingStep2Fragment extends Fragment {

    private static final String[] SEX_OPTIONS = {"Male", "Female"};
    private static final String[] CIVIL_STATUS_OPTIONS = {"Single", "Married", "Widowed", "Separated", "Annulled"};
    private static final String[] CITIZENSHIP_OPTIONS = {"Filipino", "Dual Citizen", "Foreign National"};

    private OnboardingViewModel viewModel;
    private TextInputLayout tilFullName, tilSex, tilBirthdate, tilCivilStatus, tilCitizenship;
    private TextInputEditText etFullName, etMobileNumber, etBirthdate, etBirthplace;
    private AutoCompleteTextView actvSex, actvCivilStatus, actvCitizenship;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_onboarding_step2_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        tilFullName = view.findViewById(R.id.tilFullName);
        tilSex = view.findViewById(R.id.tilSex);
        tilBirthdate = view.findViewById(R.id.tilBirthdate);
        tilCivilStatus = view.findViewById(R.id.tilCivilStatus);
        tilCitizenship = view.findViewById(R.id.tilCitizenship);

        etFullName = view.findViewById(R.id.etFullName);
        etMobileNumber = view.findViewById(R.id.etMobileNumber);
        etBirthdate = view.findViewById(R.id.etBirthdate);
        etBirthplace = view.findViewById(R.id.etBirthplace);
        actvSex = view.findViewById(R.id.actvSex);
        actvCivilStatus = view.findViewById(R.id.actvCivilStatus);
        actvCitizenship = view.findViewById(R.id.actvCitizenship);

        bindDropdown(actvSex, SEX_OPTIONS, viewModel.sex);
        bindDropdown(actvCivilStatus, CIVIL_STATUS_OPTIONS, viewModel.civilStatus);
        bindDropdown(actvCitizenship, CITIZENSHIP_OPTIONS, viewModel.citizenship);

        if (savedInstanceState == null) {
            etFullName.setText(viewModel.fullName);
            etMobileNumber.setText(viewModel.mobileNumber);
            etBirthdate.setText(viewModel.birthdate);
            etBirthplace.setText(viewModel.birthplace);
        }

        etBirthdate.setOnClickListener(v -> openDatePicker());

        MaterialButton btnBack = view.findViewById(R.id.btnStep2Back);
        MaterialButton btnNext = view.findViewById(R.id.btnStep2Next);
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

    private void bindDropdown(AutoCompleteTextView view, String[] items, String currentValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, items);
        view.setAdapter(adapter);
        if (currentValue != null && !currentValue.isEmpty() && (view.getText() == null || view.getText().toString().isEmpty())) {
            view.setText(currentValue, false);
        }
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) - 20;
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String existing = readText(etBirthdate);
        if (existing.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = existing.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, pickedYear, pickedMonth, pickedDay) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", pickedYear, pickedMonth + 1, pickedDay);
                    etBirthdate.setText(formatted);
                },
                year,
                month,
                day
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void saveDraft() {
        if (viewModel == null || etFullName == null) return;
        viewModel.fullName = readText(etFullName);
        viewModel.mobileNumber = readText(etMobileNumber);
        viewModel.sex = actvSex.getText() != null ? actvSex.getText().toString().trim() : "";
        viewModel.birthdate = readText(etBirthdate);
        viewModel.civilStatus = actvCivilStatus.getText() != null ? actvCivilStatus.getText().toString().trim() : "";
        viewModel.citizenship = actvCitizenship.getText() != null ? actvCitizenship.getText().toString().trim() : "";
        viewModel.birthplace = readText(etBirthplace);
    }

    private void validateAndProceed() {
        tilFullName.setError(null);
        tilSex.setError(null);
        tilBirthdate.setError(null);
        tilCivilStatus.setError(null);
        tilCitizenship.setError(null);

        saveDraft();

        boolean valid = true;
        if (viewModel.fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            valid = false;
        }
        if (viewModel.sex.isEmpty()) {
            tilSex.setError("Select sex");
            valid = false;
        }
        if (viewModel.birthdate.isEmpty()) {
            tilBirthdate.setError("Select birthdate");
            valid = false;
        }
        if (viewModel.civilStatus.isEmpty()) {
            tilCivilStatus.setError("Select civil status");
            valid = false;
        }
        if (viewModel.citizenship.isEmpty()) {
            tilCitizenship.setError("Select citizenship");
            valid = false;
        }
        if (!valid) return;

        ((OnboardingActivity) requireActivity()).goToNextStep();
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
