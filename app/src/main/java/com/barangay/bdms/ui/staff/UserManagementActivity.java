package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class UserManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s12_user_management);

        MaterialButton btnCreateUser = findViewById(R.id.btnCreateUser);
        if (btnCreateUser != null) {
            btnCreateUser.setOnClickListener(v -> {
                Toast.makeText(this, "Staff/Admin account created! Credential slip generated.", Toast.LENGTH_LONG).show();
            });
        }
    }
}
