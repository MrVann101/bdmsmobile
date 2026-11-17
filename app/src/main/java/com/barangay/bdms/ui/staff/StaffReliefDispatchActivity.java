package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.google.android.material.button.MaterialButton;

public class StaffReliefDispatchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s7_relief_dispatch);

        MaterialButton btnAuthorize = findViewById(R.id.btnAuthorizeDispatch);
        if (btnAuthorize != null) {
            btnAuthorize.setOnClickListener(v -> {
                Toast.makeText(this, "⚡ Relief & Tanod unit successfully authorized and dispatched!", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }
}
