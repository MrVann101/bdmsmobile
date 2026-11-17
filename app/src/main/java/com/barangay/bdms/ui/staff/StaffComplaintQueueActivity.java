package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.repository.BdmsRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class StaffComplaintQueueActivity extends AppCompatActivity {
    private BdmsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s3_complaint_queue);
        StaffNavHelper.setupBottomNav(this);
        repository = new BdmsRepository();
        fetchComplaints();
    }

    private void fetchComplaints() {
        repository.getComplaints().enqueue(new Callback<List<Complaint>>() {
            @Override
            public void onResponse(@NonNull Call<List<Complaint>> call, @NonNull Response<List<Complaint>> response) {
                if (isFinishing() || isDestroyed()) return;
            }

            @Override
            public void onFailure(@NonNull Call<List<Complaint>> call, @NonNull Throwable t) {}
        });
    }
}
