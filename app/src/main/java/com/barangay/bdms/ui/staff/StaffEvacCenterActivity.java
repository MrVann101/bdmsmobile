package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class StaffEvacCenterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s8_audit_log);

        MaterialButton btnIntake = findViewById(R.id.btnIntakeEvacuee);
        if (btnIntake != null) {
            btnIntake.setOnClickListener(v -> {
                Toast.makeText(this, "Opening New Evacuee Intake Form (S9)...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
