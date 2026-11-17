package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;

public class StaffEmergencyDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s5_emergency_dashboard);
        StaffNavHelper.setupBottomNav(this);
    }
}
