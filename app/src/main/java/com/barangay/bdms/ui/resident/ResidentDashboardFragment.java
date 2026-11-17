package com.barangay.bdms.ui.resident;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Announcement;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.network.SessionManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.NotificationBellView;
import com.barangay.bdms.ui.common.StatusBadge;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResidentDashboardFragment extends Fragment implements NotificationSocketManager.Listener {

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private LoadingOverlay loadingOverlay;
    private NotificationBellView notificationBell;

    private TextView tvResidentName, tvResidentPurokBadge;
    private TextView tvStatRequests, tvStatComplaints, tvStatUnread;
    private TextView tvLatestRequestType, tvLatestRequestMeta;
    private StatusBadge badgeLatestRequestStatus;
    private TextView tvLatestAnnouncementCategory, tvLatestAnnouncementTitle, tvLatestAnnouncementBody;
    private MaterialCardView cardLatestRequest;

    @Nullable
    private String latestRequestId = null;
    private int pendingFetches = 0;
    private int failedFetches = 0;
    private boolean skipNextResumeRefresh = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r3_resident_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshDashboard);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        notificationBell = view.findViewById(R.id.notificationBell);

        tvResidentName = view.findViewById(R.id.tvResidentName);
        tvResidentPurokBadge = view.findViewById(R.id.tvResidentPurokBadge);
        tvStatRequests = view.findViewById(R.id.tvStatRequests);
        tvStatComplaints = view.findViewById(R.id.tvStatComplaints);
        tvStatUnread = view.findViewById(R.id.tvStatUnread);
        tvLatestRequestType = view.findViewById(R.id.tvLatestRequestType);
        tvLatestRequestMeta = view.findViewById(R.id.tvLatestRequestMeta);
        badgeLatestRequestStatus = view.findViewById(R.id.badgeLatestRequestStatus);
        tvLatestAnnouncementCategory = view.findViewById(R.id.tvLatestAnnouncementCategory);
        tvLatestAnnouncementTitle = view.findViewById(R.id.tvLatestAnnouncementTitle);
        tvLatestAnnouncementBody = view.findViewById(R.id.tvLatestAnnouncementBody);
        cardLatestRequest = view.findViewById(R.id.cardLatestRequest);

        bindSessionHeader();

        notificationBell.setOnBellClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_notifications)
        );
        view.findViewById(R.id.cardStatAlerts).setOnClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_notifications)
        );
        view.findViewById(R.id.cardStatRequests).setOnClickListener(v -> openTab(R.id.documentRequestListFragment, null));
        view.findViewById(R.id.btnViewRequests).setOnClickListener(v -> openTab(R.id.documentRequestListFragment, null));
        view.findViewById(R.id.cardStatComplaints).setOnClickListener(v -> openCommunityTab(CommunityHostFragment.TAB_COMPLAINTS));

        view.findViewById(R.id.cardRequestDoc).setOnClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_requestForm)
        );
        view.findViewById(R.id.cardFileComplaint).setOnClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_complaintForm)
        );
        view.findViewById(R.id.cardReportEmergency).setOnClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_emergencyForm)
        );
        view.findViewById(R.id.cardAnnouncements).setOnClickListener(v -> openCommunityTab(CommunityHostFragment.TAB_ANNOUNCEMENTS));
        view.findViewById(R.id.cardLatestAnnouncement).setOnClickListener(v -> openCommunityTab(CommunityHostFragment.TAB_ANNOUNCEMENTS));

        cardLatestRequest.setOnClickListener(v -> {
            if (latestRequestId != null) {
                Bundle args = new Bundle();
                args.putString("requestId", latestRequestId);
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_requestDetail, args);
            } else {
                NavUtils.safeNavigate(this, R.id.action_dashboard_to_requestForm);
            }
        });

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> loadDashboardData(false));

        skipNextResumeRefresh = true;
        loadDashboardData(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationSocketManager.getInstance().addListener(this);
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            loadDashboardData(false);
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
            loadNotificationsCount();
        }
    }

    private void bindSessionHeader() {
        SessionManager session = SessionManager.getInstance(requireContext());
        String name = session.getFullName();
        String purok = session.getPurok();
        tvResidentName.setText(name != null && !name.isEmpty() ? name : "Resident");
        tvResidentPurokBadge.setText(purok != null && !purok.isEmpty() ? purok : "Unassigned");
    }

    private void openTab(int tabDestinationId, @Nullable Bundle args) {
        if (getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).selectBottomTab(tabDestinationId, args);
        }
    }

    private void openCommunityTab(int subTabIndex) {
        if (getActivity() instanceof ResidentMainActivity) {
            ((ResidentMainActivity) getActivity()).openCommunityTab(subTabIndex);
        }
    }

    private void loadDashboardData(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        pendingFetches = 4;
        failedFetches = 0;
        loadDocumentRequests();
        loadComplaints();
        loadNotificationsCount();
        loadAnnouncements();
    }

    private void markFetchDone(boolean failed) {
        if (failed) failedFetches++;
        pendingFetches = Math.max(0, pendingFetches - 1);
        if (pendingFetches == 0 && isAdded()) {
            loadingOverlay.hide();
            swipeRefresh.setRefreshing(false);
            if (failedFetches > 0) {
                Toast.makeText(
                        requireContext(),
                        "Some dashboard data could not refresh. Pull down to retry.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void loadDocumentRequests() {
        repository.getDocumentRequests(null).enqueue(new Callback<List<DocumentRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<DocumentRequest>> call, @NonNull Response<List<DocumentRequest>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<DocumentRequest> list = response.body();
                    int activeCount = 0;
                    for (DocumentRequest req : list) {
                        String st = req.getStatus();
                        if ("pending".equals(st) || "processing".equals(st) || "ready".equals(st)) {
                            activeCount++;
                        }
                    }
                    tvStatRequests.setText(String.valueOf(activeCount));
                    if (!list.isEmpty()) {
                        DocumentRequest latest = list.get(0);
                        latestRequestId = latest.getRequestId() != null ? latest.getRequestId().toString() : null;
                        tvLatestRequestType.setText(latest.getDocumentType());
                        tvLatestRequestMeta.setText(
                                (latest.getControlNumber() != null ? latest.getControlNumber() : "Pending")
                                        + " • " + DateFormatUtils.formatDateTime(latest.getCreatedAt())
                        );
                        badgeLatestRequestStatus.setVisibility(View.VISIBLE);
                        badgeLatestRequestStatus.setStatus(latest.getStatus());
                    } else {
                        latestRequestId = null;
                        tvLatestRequestType.setText("No document requests yet");
                        tvLatestRequestMeta.setText("Tap Request Document to submit your first application.");
                        badgeLatestRequestStatus.setVisibility(View.GONE);
                    }
                    markFetchDone(false);
                } else {
                    markFetchDone(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DocumentRequest>> call, @NonNull Throwable t) {
                if (isAdded()) markFetchDone(true);
            }
        });
    }

    private void loadComplaints() {
        repository.getComplaints().enqueue(new Callback<List<Complaint>>() {
            @Override
            public void onResponse(@NonNull Call<List<Complaint>> call, @NonNull Response<List<Complaint>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    int openCount = 0;
                    for (Complaint c : response.body()) {
                        if ("pending".equals(c.getStatus()) || "reviewing".equals(c.getStatus())) {
                            openCount++;
                        }
                    }
                    tvStatComplaints.setText(String.valueOf(openCount));
                    markFetchDone(false);
                } else {
                    markFetchDone(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Complaint>> call, @NonNull Throwable t) {
                if (isAdded()) markFetchDone(true);
            }
        });
    }

    private void loadNotificationsCount() {
        repository.getMyNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    int unread = 0;
                    for (Notification n : response.body()) {
                        if (!n.isRead()) unread++;
                    }
                    tvStatUnread.setText(String.valueOf(unread));
                    notificationBell.setUnreadCount(unread);
                    markFetchDone(false);
                } else {
                    markFetchDone(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                if (isAdded()) markFetchDone(true);
            }
        });
    }

    private void loadAnnouncements() {
        repository.getAnnouncements().enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(@NonNull Call<List<Announcement>> call, @NonNull Response<List<Announcement>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().isEmpty()) {
                        Announcement top = null;
                        for (Announcement a : response.body()) {
                            if ("emergency".equalsIgnoreCase(a.getCategory())) {
                                top = a;
                                break;
                            }
                        }
                        if (top == null) {
                            top = response.body().get(0);
                        }
                        String cat = top.getCategory() != null ? top.getCategory().toUpperCase(Locale.US) : "GENERAL";
                        boolean isEmergency = "EMERGENCY".equals(cat);
                        tvLatestAnnouncementCategory.setText(cat);
                        ViewCompat.setBackgroundTintList(
                                tvLatestAnnouncementCategory,
                                ContextCompat.getColorStateList(
                                        requireContext(),
                                        isEmergency ? R.color.statusRejectedBg : R.color.colorPrimaryLight
                                )
                        );
                        tvLatestAnnouncementCategory.setTextColor(ContextCompat.getColor(
                                requireContext(),
                                isEmergency ? R.color.statusRejectedText : R.color.colorPrimaryDark
                        ));
                        tvLatestAnnouncementTitle.setText(top.getTitle());
                        tvLatestAnnouncementBody.setText(top.getBody());
                    } else {
                        tvLatestAnnouncementCategory.setText("NOTICE");
                        ViewCompat.setBackgroundTintList(
                                tvLatestAnnouncementCategory,
                                ContextCompat.getColorStateList(requireContext(), R.color.colorPrimaryLight)
                        );
                        tvLatestAnnouncementCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark));
                        tvLatestAnnouncementTitle.setText("No active announcements");
                        tvLatestAnnouncementBody.setText("You're all caught up on barangay advisories.");
                    }
                    markFetchDone(false);
                } else {
                    markFetchDone(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Announcement>> call, @NonNull Throwable t) {
                if (isAdded()) markFetchDone(true);
            }
        });
    }
}
