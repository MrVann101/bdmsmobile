package com.barangay.bdms.ui.staff;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class NotificationCenterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s11_notification_center);

        MaterialButton btnCompose = findViewById(R.id.btnComposeAdvisory);
        if (btnCompose != null) {
            btnCompose.setOnClickListener(v -> {
                startActivity(new Intent(this, StaffAnnouncementActivity.class));
            });
        }
    }
}
