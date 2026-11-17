package com.barangay.bdms.ui.resident;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.EmergencyReport;
import com.barangay.bdms.network.AuthenticatedImageLoader;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.EmptyStateView;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyReportListFragment extends Fragment {

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private EmptyStateView emptyState;
    private LoadingOverlay loadingOverlay;
    private EmergencyReportAdapter adapter;
    private boolean skipNextResumeRefresh = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r12_emergency_report_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshEmergencies);
        recyclerView = view.findViewById(R.id.rvEmergencyReports);
        emptyState = view.findViewById(R.id.emptyStateEmergencies);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabNewEmergency);

        adapter = new EmergencyReportAdapter(this::openReportDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v ->
                NavUtils.safeNavigateFromActivity(this, R.id.residentNavHost, R.id.action_community_to_emergencyForm, null)
        );

        swipeRefresh.setColorSchemeResources(R.color.statusRejectedText);
        swipeRefresh.setOnRefreshListener(() -> fetchReports(false));
        skipNextResumeRefresh = true;
        fetchReports(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            fetchReports(false);
        }
    }

    private void fetchReports(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getEmergencyReports().enqueue(new Callback<List<EmergencyReport>>() {
            @Override
            public void onResponse(@NonNull Call<List<EmergencyReport>> call, @NonNull Response<List<EmergencyReport>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<EmergencyReport> list = response.body();
                    adapter.submitList(list);
                    if (list.isEmpty()) {
                        emptyState.setMessage("No emergency reports submitted.\nTap Report Emergency during a disaster.");
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to load emergency reports.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    if (adapter.getItemCount() == 0) {
                        emptyState.setMessage(err + "\nPull down to retry.");
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<EmergencyReport>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading emergency reports.", Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    emptyState.setMessage("Unable to load emergency reports.\nPull down to retry.");
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void openReportDetail(@NonNull EmergencyReport cachedReport) {
        if (cachedReport.getReportId() == null) {
            showDetailDialog(cachedReport);
            return;
        }
        loadingOverlay.show();
        repository.getEmergencyReportDetail(cachedReport.getReportId().toString()).enqueue(new Callback<EmergencyReport>() {
            @Override
            public void onResponse(@NonNull Call<EmergencyReport> call, @NonNull Response<EmergencyReport> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                EmergencyReport report = (response.isSuccessful() && response.body() != null)
                        ? response.body()
                        : cachedReport;
                showDetailDialog(report);
            }

            @Override
            public void onFailure(@NonNull Call<EmergencyReport> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                showDetailDialog(cachedReport);
            }
        });
    }

    private void showDetailDialog(@NonNull EmergencyReport report) {
        if (!isAdded()) return;
        String type = report.getDisasterType() != null
                ? report.getDisasterType().toUpperCase(Locale.US)
                : "EMERGENCY";
        String status = report.getStatus() != null
                ? report.getStatus().replace('_', ' ').toUpperCase(Locale.US)
                : "REPORTED";
        String needs = report.getNeeds() != null && !report.getNeeds().isEmpty()
                ? TextUtils.join(", ", report.getNeeds())
                : "None specified";

        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);

        TextView body = new TextView(requireContext());
        body.setText(
                "Status: " + status
                        + "\nPurok: " + (report.getPurok() != null ? report.getPurok() : "—")
                        + "\nHousehold Members Affected: " + report.getHouseholdMembersAffected()
                        + "\nNeeds: " + needs
                        + "\nSubmitted: " + DateFormatUtils.formatDateTime(report.getCreatedAt())
        );
        body.setLineSpacing(0f, 1.25f);
        container.addView(body);

        if (report.getPhotoUrl() != null && !report.getPhotoUrl().trim().isEmpty()) {
            ImageView iv = new ImageView(requireContext());
            int imgHeight = Math.round(180 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgHeight);
            lp.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setContentDescription("Emergency report photo");
            AuthenticatedImageLoader.load(requireContext(), report.getPhotoUrl().trim(), iv, R.drawable.ic_bdms_logo, 960);
            container.addView(iv);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(type + " Report")
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
    }
}
