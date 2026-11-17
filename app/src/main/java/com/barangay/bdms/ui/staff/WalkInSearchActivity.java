package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class WalkInSearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s10_walkin);

        MaterialButton btnSelectRequest = findViewById(R.id.btnSelectRequest);
        if (btnSelectRequest != null) {
            btnSelectRequest.setOnClickListener(v -> {
                Toast.makeText(this, "Resident selected for walk-in request (Step 3)...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
