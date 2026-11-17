package com.barangay.bdms.ui.staff;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.card.MaterialCardView;

public class AdminHubActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s13_admin_hub);
        StaffNavHelper.setupBottomNav(this);

        MaterialCardView cardUserManagement = findViewById(R.id.cardUserManagement);
        if (cardUserManagement != null) {
            cardUserManagement.setOnClickListener(v -> {
                startActivity(new Intent(this, UserManagementActivity.class));
            });
        }

        MaterialCardView cardNotifCenter = findViewById(R.id.cardNotifCenter);
        if (cardNotifCenter != null) {
            cardNotifCenter.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationCenterActivity.class));
            });
        }

        MaterialCardView cardAuditLog = findViewById(R.id.cardAuditLog);
        if (cardAuditLog != null) {
            cardAuditLog.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Sensitive Action Audit Log (S8)...", Toast.LENGTH_SHORT).show();
            });
        }

        MaterialCardView cardCertTemplates = findViewById(R.id.cardCertTemplates);
        if (cardCertTemplates != null) {
            cardCertTemplates.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Certificate Templates Settings (S9)...", Toast.LENGTH_SHORT).show();
            });
        }

        MaterialCardView cardManagePuroks = findViewById(R.id.cardManagePuroks);
        if (cardManagePuroks != null) {
            cardManagePuroks.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Purok Management (S14)...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
