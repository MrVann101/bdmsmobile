package com.barangay.bdms.ui.resident;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.EmptyStateView;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListFragment extends Fragment implements NotificationSocketManager.Listener {

    private static final long POLL_INTERVAL_MS = 20_000L;
    private static final String STATE_UNREAD_ONLY = "notifications_unread_only";

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private EmptyStateView emptyState;
    private LoadingOverlay loadingOverlay;
    private ChipGroup filterGroup;
    private NotificationAdapter adapter;

    private final List<Notification> allNotifications = new ArrayList<>();
    private boolean unreadOnly = false;
    private boolean skipNextResumeRefresh = false;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && isResumed()) {
                fetchNotifications(false);
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            unreadOnly = savedInstanceState.getBoolean(STATE_UNREAD_ONLY, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_UNREAD_ONLY, unreadOnly);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r9_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        swipeRefresh = view.findViewById(R.id.swipeRefreshNotifications);
        recyclerView = view.findViewById(R.id.rvNotifications);
        emptyState = view.findViewById(R.id.emptyStateNotifications);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        filterGroup = view.findViewById(R.id.chipGroupNotificationFilter);

        btnBack.setOnClickListener(v -> NavUtils.safeNavigateUp(this));

        adapter = new NotificationAdapter(this::onNotificationTapped);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        int targetChip = unreadOnly ? R.id.chipNotifUnread : R.id.chipNotifAll;
        if (filterGroup.getCheckedChipId() != targetChip) {
            filterGroup.check(targetChip);
        }
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            unreadOnly = !checkedIds.isEmpty() && checkedIds.get(0) == R.id.chipNotifUnread;
            applyFilter(null);
        });

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> fetchNotifications(false));

        skipNextResumeRefresh = true;
        fetchNotifications(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationSocketManager.getInstance().addListener(this);
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            fetchNotifications(false);
        }
    }

    @Override
    public void onPause() {
        NotificationSocketManager.getInstance().removeListener(this);
        pollHandler.removeCallbacks(pollRunnable);
        super.onPause();
    }

    @Override
    public void onNotificationReceived() {
        if (isAdded()) {
            fetchNotifications(false);
        }
    }

    private void fetchNotifications(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getMyNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allNotifications.clear();
                    allNotifications.addAll(response.body());
                    applyFilter(null);
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to load notifications.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    applyFilter(err + "\nPull down to retry.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading notifications.", Toast.LENGTH_SHORT).show();
                applyFilter("Unable to load notifications.\nPull down to retry.");
            }
        });
    }

    private void applyFilter(@Nullable String errorMessage) {
        List<Notification> filtered = new ArrayList<>();
        for (Notification n : allNotifications) {
            if (!unreadOnly || !n.isRead()) {
                filtered.add(n);
            }
        }
        adapter.submitList(filtered);
        if (filtered.isEmpty()) {
            if (errorMessage != null && allNotifications.isEmpty()) {
                emptyState.setMessage(errorMessage);
            } else {
                emptyState.setMessage(unreadOnly ? "No unread notifications." : "No notifications yet.");
            }
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onNotificationTapped(@NonNull Notification notification) {
        if (!notification.isRead() && notification.getNotificationId() != null) {
            notification.markAsRead();
            applyFilter(null);
            repository.markNotificationRead(notification.getNotificationId().toString())
                    .enqueue(new Callback<Notification>() {
                        @Override
                        public void onResponse(@NonNull Call<Notification> call, @NonNull Response<Notification> response) {
                        }

                        @Override
                        public void onFailure(@NonNull Call<Notification> call, @NonNull Throwable t) {
                        }
                    });
        }
        deepLinkNotification(notification);
    }

    private void deepLinkNotification(@NonNull Notification notification) {
        String type = notification.getType() != null ? notification.getType().toLowerCase(Locale.US) : "";
        if ("request_update".equals(type)) {
            if (notification.getRelatedId() != null) {
                Bundle args = new Bundle();
                args.putString("requestId", notification.getRelatedId().toString());
                NavUtils.safeNavigate(this, R.id.action_notifications_to_requestDetail, args);
            } else if (getActivity() instanceof ResidentMainActivity) {
                ((ResidentMainActivity) getActivity()).selectBottomTab(R.id.documentRequestListFragment, null);
            }
        } else if ("complaint".equals(type) && getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).openCommunityTab(CommunityHostFragment.TAB_COMPLAINTS);
        } else if ("emergency".equals(type) && getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).openCommunityTab(CommunityHostFragment.TAB_EMERGENCIES);
        } else if ("announcement".equals(type) && getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).openCommunityTab(CommunityHostFragment.TAB_ANNOUNCEMENTS);
        }
    }
}
