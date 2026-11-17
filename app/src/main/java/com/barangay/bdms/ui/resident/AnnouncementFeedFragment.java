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
import com.barangay.bdms.model.Announcement;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.EmptyStateView;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementFeedFragment extends Fragment {

    private static final String STATE_CATEGORY = "selected_announcement_category";

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private EmptyStateView emptyState;
    private LoadingOverlay loadingOverlay;
    private ChipGroup categoryGroup;
    private AnnouncementAdapter adapter;

    private final List<Announcement> allAnnouncements = new ArrayList<>();
    @Nullable
    private String selectedCategory = null;
    private boolean skipNextResumeRefresh = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedCategory = savedInstanceState.getString(STATE_CATEGORY, null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedCategory != null) {
            outState.putString(STATE_CATEGORY, selectedCategory);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r10_announcement_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshAnnouncements);
        recyclerView = view.findViewById(R.id.rvAnnouncements);
        emptyState = view.findViewById(R.id.emptyStateAnnouncements);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        categoryGroup = view.findViewById(R.id.chipGroupAnnouncementCategory);

        adapter = new AnnouncementAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        syncChipSelection();
        categoryGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipCatEmergency) selectedCategory = "emergency";
                else if (id == R.id.chipCatEvent) selectedCategory = "event";
                else if (id == R.id.chipCatGeneral) selectedCategory = "general";
                else selectedCategory = null;
            }
            applyFilterAndPin(null);
        });

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> fetchAnnouncements(false));
        skipNextResumeRefresh = true;
        fetchAnnouncements(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else {
            fetchAnnouncements(false);
        }
    }

    private void syncChipSelection() {
        if (categoryGroup == null) return;
        int chipId = R.id.chipCatAll;
        if ("emergency".equalsIgnoreCase(selectedCategory)) chipId = R.id.chipCatEmergency;
        else if ("event".equalsIgnoreCase(selectedCategory)) chipId = R.id.chipCatEvent;
        else if ("general".equalsIgnoreCase(selectedCategory)) chipId = R.id.chipCatGeneral;
        if (categoryGroup.getCheckedChipId() != chipId) {
            categoryGroup.check(chipId);
        }
    }

    private void fetchAnnouncements(boolean showLoader) {
        if (showLoader && !swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getAnnouncements().enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(@NonNull Call<List<Announcement>> call, @NonNull Response<List<Announcement>> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allAnnouncements.clear();
                    allAnnouncements.addAll(response.body());
                    applyFilterAndPin(null);
                } else {
                    String err = ApiErrorUtils.parseErrorMessage(response, "Unable to load announcements.");
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    applyFilterAndPin(err + "\nPull down to retry.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Announcement>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading announcements.", Toast.LENGTH_SHORT).show();
                applyFilterAndPin("Unable to load announcements.\nPull down to retry.");
            }
        });
    }

    private void applyFilterAndPin(@Nullable String errorMessage) {
        List<Announcement> filtered = new ArrayList<>();
        Announcement pinnedEmergency = null;

        for (Announcement a : allAnnouncements) {
            if (a.isExpired()) continue;
            if (selectedCategory != null && !selectedCategory.equalsIgnoreCase(a.getCategory())) continue;

            if (pinnedEmergency == null && "emergency".equalsIgnoreCase(a.getCategory())) {
                pinnedEmergency = a;
            } else {
                filtered.add(a);
            }
        }

        if (pinnedEmergency != null) {
            filtered.add(0, pinnedEmergency);
        }

        adapter.submitList(filtered, pinnedEmergency != null);
        if (filtered.isEmpty()) {
            if (errorMessage != null && allAnnouncements.isEmpty()) {
                emptyState.setMessage(errorMessage);
            } else {
                emptyState.setMessage("No active announcements right now.");
            }
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
