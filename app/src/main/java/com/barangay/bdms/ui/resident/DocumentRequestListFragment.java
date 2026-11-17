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
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.EmptyStateView;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.NotificationBellView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentRequestListFragment extends Fragment implements NotificationSocketManager.Listener {

    private static final String STATE_FILTER = "selected_status_filter";

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private EmptyStateView emptyState;
    private LoadingOverlay loadingOverlay;
    private NotificationBellView notificationBell;
    private ChipGroup filterGroup;
    private DocumentRequestAdapter adapter;
    private final List<DocumentRequest> allRequests = new ArrayList<>();
    @Nullable
    private String selectedStatusFilter = null;
    private boolean skipNextResumeRefresh = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedStatusFilter = savedInstanceState.getString(STATE_FILTER, null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedStatusFilter != null) {
            outState.putString(STATE_FILTER, selectedStatusFilter);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r4_document_request_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshRequests);
        recyclerView = view.findViewById(R.id.rvDocumentRequests);
        emptyState = view.findViewById(R.id.emptyStateRequests);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        notificationBell = view.findViewById(R.id.notificationBell);
        filterGroup = view.findViewById(R.id.chipGroupStatusFilter);
        ExtendedFloatingActionButton fabNewRequest = view.findViewById(R.id.fabNewRequest);

        adapter = new DocumentRequestAdapter(request -> {
            if (request.getRequestId() == null) return;
            Bundle args = new Bundle();
            args.putString("requestId", request.getRequestId().toString());
            NavUtils.safeNavigate(this, R.id.action_requestList_to_requestDetail, args);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabNewRequest.setOnClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_requestList_to_requestForm)
        );
        notificationBell.setOnBellClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_requestList_to_notifications)
        );

        syncChipSelectionToFilter();
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedStatusFilter = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipFilterPending) selectedStatusFilter = "pending";
                else if (id == R.id.chipFilterProcessing) selectedStatusFilter = "processing";
                else if (id == R.id.chipFilterReady) selectedStatusFilter = "ready";
                else if (id == R.id.chipFilterCompleted) selectedStatusFilter = "completed";
                else if (id == R.id.chipFilterRejected) selectedStatusFilter = "rejected";
                else selectedStatusFilter = null;
            }
            applyFilter(null);
        });

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> fetchRequests(false));
        skipNextResumeRefresh = true;
        fetchRequests(true);
        refreshUnreadBell();
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationSocketManager.getInstance().addListener(this);
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            fetchRequests(false);
            refreshUnreadBell();
        }
    }

    @Override
    public void onPause() {
        NotificationSocketManager.getInstance().removeListener(this);
        super.onPause();
    }

    @Override
    public void onNotificationReceived() {
        if (isAdded()) {
            refreshUnreadBell();
        }
    }

    private void syncChipSelectionToFilter() {
        if (filterGroup == null) return;
        int chipId = R.id.chipFilterAll;
        if ("pending".equalsIgnoreCase(selectedStatusFilter)) chipId = R.id.chipFilterPending;
        else if ("processing".equalsIgnoreCase(selectedStatusFilter)) chipId = R.id.chipFilterProcessing;
        else if ("ready".equalsIgnoreCase(selectedStatusFilter)) chipId = R.id.chipFilterReady;
        else if ("completed".equalsIgnoreCase(selectedStatusFilter)) chipId = R.id.chipFilterCompleted;
        else if ("rejected".equalsIgnoreCase(selectedStatusFilter)) chipId = R.id.chipFilterRejected;
        if (filterGroup.getCheckedChipId() != chipId) {
            filterGroup.check(chipId);
        }
    }

    private void fetchRequests(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getDocumentRequests(null).enqueue(new Callback<List<DocumentRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<DocumentRequest>> call, @NonNull Response<List<DocumentRequest>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allRequests.clear();
                    allRequests.addAll(response.body());
                    applyFilter(null);
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to load requests.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    applyFilter(err);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DocumentRequest>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading requests.", Toast.LENGTH_SHORT).show();
                applyFilter("Unable to load document requests.\nPull down to retry.");
            }
        });
    }

    private void applyFilter(@Nullable String errorMessage) {
        List<DocumentRequest> filtered = new ArrayList<>();
        for (DocumentRequest req : allRequests) {
            if (selectedStatusFilter == null || selectedStatusFilter.equalsIgnoreCase(req.getStatus())) {
                filtered.add(req);
            }
        }
        adapter.submitList(filtered);
        if (filtered.isEmpty()) {
            if (errorMessage != null && allRequests.isEmpty()) {
                emptyState.setMessage(errorMessage);
            } else {
                emptyState.setMessage(selectedStatusFilter == null
                        ? "No document requests yet.\nTap + New Request to apply."
                        : "No " + selectedStatusFilter + " document requests.");
            }
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshUnreadBell() {
        repository.getMyNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                int unread = 0;
                for (Notification n : response.body()) {
                    if (!n.isRead()) unread++;
                }
                notificationBell.setUnreadCount(unread);
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
            }
        });
    }
}
