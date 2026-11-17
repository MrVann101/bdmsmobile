package com.barangay.bdms.ui.staff;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.barangay.bdms.R;
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.repository.BdmsRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class StaffRequestQueueActivity extends AppCompatActivity {

    private BdmsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_staff_s2_request_queue);
        repository = new BdmsRepository();
        fetchRequests();
    }

    private void fetchRequests() {
        repository.getDocumentRequests(null).enqueue(new Callback<List<DocumentRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<DocumentRequest>> call, @NonNull Response<List<DocumentRequest>> response) {
                if (isFinishing() || isDestroyed()) return;
            }

            @Override
            public void onFailure(@NonNull Call<List<DocumentRequest>> call, @NonNull Throwable t) {}
        });
    }
}
