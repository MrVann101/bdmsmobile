package com.barangay.bdms.ui.resident;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.NotificationBellView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tabbed Community host combining R10 Announcements, R7 Complaints, and R12 Emergency Reports.
 */
public class CommunityHostFragment extends Fragment implements NotificationSocketManager.Listener {

    public static final String ARG_INITIAL_TAB = "initialTab";
    public static final int TAB_ANNOUNCEMENTS = 0;
    public static final int TAB_COMPLAINTS = 1;
    public static final int TAB_EMERGENCIES = 2;

    private static final String[] TAB_TITLES = {"Announcements", "Complaints", "Emergencies"};

    private NotificationBellView bellView;
    private BdmsRepository repository;
    private ViewPager2 viewPager;
    private ResidentNavViewModel navViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_community_host, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());
        navViewModel = new ViewModelProvider(requireActivity()).get(ResidentNavViewModel.class);

        bellView = view.findViewById(R.id.notificationBell);
        bellView.setOnBellClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_community_to_notifications)
        );

        TabLayout tabLayout = view.findViewById(R.id.communityTabLayout);
        viewPager = view.findViewById(R.id.communityViewPager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case TAB_COMPLAINTS:
                        return new ComplaintListFragment();
                    case TAB_EMERGENCIES:
                        return new EmergencyReportListFragment();
                    case TAB_ANNOUNCEMENTS:
                    default:
                        return new AnnouncementFeedFragment();
                }
            }

            @Override
            public int getItemCount() {
                return TAB_TITLES.length;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();

        Integer pendingTab = navViewModel.consumePendingCommunityTab();
        int initialTab = pendingTab != null
                ? pendingTab
                : (getArguments() != null ? getArguments().getInt(ARG_INITIAL_TAB, TAB_ANNOUNCEMENTS) : TAB_ANNOUNCEMENTS);
        if (initialTab >= 0 && initialTab < TAB_TITLES.length) {
            viewPager.setCurrentItem(initialTab, false);
        }

        navViewModel.getPendingCommunityTab().observe(getViewLifecycleOwner(), tabIdx -> {
            if (tabIdx != null && tabIdx >= 0 && tabIdx < TAB_TITLES.length && viewPager != null) {
                navViewModel.consumePendingCommunityTab();
                viewPager.setCurrentItem(tabIdx, false);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationSocketManager.getInstance().addListener(this);
        refreshUnreadBell();
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

    private void refreshUnreadBell() {
        repository.getMyNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                int unread = 0;
                for (Notification n : response.body()) {
                    if (!n.isRead()) unread++;
                }
                bellView.setUnreadCount(unread);
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
            }
        });
    }
}
