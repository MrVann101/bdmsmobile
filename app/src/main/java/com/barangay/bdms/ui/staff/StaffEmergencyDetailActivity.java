package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class StaffEmergencyDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s6_emergency_detail);

        MaterialButton btnVerify = findViewById(R.id.btnVerify);
        MaterialButton btnDispatchRelief = findViewById(R.id.btnDispatchRelief);

        if (btnVerify != null && btnDispatchRelief != null) {
            btnVerify.setOnClickListener(v -> {
                btnVerify.setEnabled(false);
                btnVerify.setText("✓ Report Verified");
                btnDispatchRelief.setEnabled(true);
                btnDispatchRelief.setText("Dispatch Relief & Rescue →");
                btnDispatchRelief.setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                btnDispatchRelief.setTextColor(getResources().getColor(android.R.color.white, null));
                Toast.makeText(this, "Emergency report verified! Relief dispatch unlocked.", Toast.LENGTH_LONG).show();
            });
        }

        if (btnDispatchRelief != null) {
            btnDispatchRelief.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Relief Dispatch console (S7)...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
