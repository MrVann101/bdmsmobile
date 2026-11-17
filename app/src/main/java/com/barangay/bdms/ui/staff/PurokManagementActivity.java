package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class PurokManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s14_purok_management);

        MaterialButton btnAddPurok = findViewById(R.id.btnAddPurok);
        if (btnAddPurok != null) {
            btnAddPurok.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Add New Purok Zone")
                        .setMessage("Feature enabled: New Purok boundary registration dialog opened.")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
    }
}
