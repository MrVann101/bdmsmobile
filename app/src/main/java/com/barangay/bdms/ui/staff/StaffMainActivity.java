package com.barangay.bdms.ui.staff;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class StaffMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s1_staff_dashboard);

        MaterialButton btnWalkIn = findViewById(R.id.btnWalkIn);
        if (btnWalkIn != null) {
            btnWalkIn.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Walk-In Resident Service (S10)...", Toast.LENGTH_SHORT).show();
            });
        }

        // Staff Bottom Navigation Bar Wiring
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                Toast.makeText(this, "Already on Staff Dashboard.", Toast.LENGTH_SHORT).show();
            });
        }

        LinearLayout navComplaints = findViewById(R.id.navComplaints);
        if (navComplaints != null) {
            navComplaints.setOnClickListener(v -> {
                startActivity(new Intent(this, StaffComplaintQueueActivity.class));
            });
        }

        LinearLayout navEmergency = findViewById(R.id.navEmergency);
        if (navEmergency != null) {
            navEmergency.setOnClickListener(v -> {
                startActivity(new Intent(this, StaffEmergencyDashboardActivity.class));
            });
        }

        LinearLayout navBroadcasts = findViewById(R.id.navBroadcasts);
        if (navBroadcasts != null) {
            navBroadcasts.setOnClickListener(v -> {
                startActivity(new Intent(this, StaffAnnouncementActivity.class));
            });
        }

        LinearLayout navMore = findViewById(R.id.navMore);
        if (navMore != null) {
            navMore.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminHubActivity.class));
            });
        }
    }
}
