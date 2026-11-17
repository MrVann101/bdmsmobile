package com.barangay.bdms.ui.resident;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.EmptyStateView;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComplaintListFragment extends Fragment {

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private EmptyStateView emptyState;
    private LoadingOverlay loadingOverlay;
    private ComplaintAdapter adapter;
    private boolean skipNextResumeRefresh = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r7_complaint_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshComplaints);
        recyclerView = view.findViewById(R.id.rvComplaints);
        emptyState = view.findViewById(R.id.emptyStateComplaints);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabNewComplaint);

        adapter = new ComplaintAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v ->
                NavUtils.safeNavigateFromActivity(this, R.id.residentNavHost, R.id.action_community_to_complaintForm, null)
        );

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> fetchComplaints(false));
        skipNextResumeRefresh = true;
        fetchComplaints(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            fetchComplaints(false);
        }
    }

    private void fetchComplaints(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getComplaints().enqueue(new Callback<List<Complaint>>() {
            @Override
            public void onResponse(@NonNull Call<List<Complaint>> call, @NonNull Response<List<Complaint>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Complaint> list = response.body();
                    adapter.submitList(list);
                    if (list.isEmpty()) {
                        emptyState.setMessage("No complaints filed yet.\nTap File Complaint to report a concern.");
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to load complaints.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    if (adapter.getItemCount() == 0) {
                        emptyState.setMessage(err + "\nPull down to retry.");
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Complaint>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading complaints.", Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    emptyState.setMessage("Unable to load complaints.\nPull down to retry.");
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }
}
