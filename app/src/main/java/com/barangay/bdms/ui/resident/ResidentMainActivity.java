package com.barangay.bdms.ui.resident;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.barangay.bdms.R;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.network.SessionManager;
import com.barangay.bdms.ui.auth.LoginActivity;
import com.barangay.bdms.ui.staff.StaffMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ResidentMainActivity extends AppCompatActivity {

    private static final Set<Integer> TOP_LEVEL_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.residentDashboardFragment,
            R.id.documentRequestListFragment,
            R.id.communityHostFragment,
            R.id.profileFragment
    ));

    private NavController navController;
    private BottomNavigationView bottomNav;
    private ResidentNavViewModel navViewModel;
    private boolean redirectingToLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.initToken(this);

        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        if (session.isStaffOrAdmin()) {
            startActivity(new Intent(this, StaffMainActivity.class));
            finish();
            return;
        }

        navViewModel = new ViewModelProvider(this).get(ResidentNavViewModel.class);
        ApiClient.setSessionExpiredListener(reason -> {
            if (!isFinishing() && !isDestroyed() && !redirectingToLogin) {
                redirectingToLogin = true;
                Toast.makeText(ResidentMainActivity.this, reason, Toast.LENGTH_LONG).show();
                NotificationSocketManager.getInstance().disconnect();
                redirectToLogin();
            }
        });

        setContentView(R.layout.activity_resident_main);

        bottomNav = findViewById(R.id.residentBottomNav);
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.residentNavHost);
        if (navHost == null) {
            return;
        }
        navController = navHost.getNavController();

        bottomNav.setOnItemSelectedListener(item -> navigateTopLevel(item.getItemId(), null));

        bottomNav.setOnItemReselectedListener(item -> {
            // Avoid stacking duplicate destinations when reselecting current tab
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isTopLevel = TOP_LEVEL_DESTINATIONS.contains(id);
            bottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
            if (isTopLevel && bottomNav.getSelectedItemId() != id) {
                bottomNav.getMenu().findItem(id).setChecked(true);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SessionManager.getInstance(this).isLoggedIn()) {
            NotificationSocketManager.getInstance().connect(this);
        } else if (!redirectingToLogin) {
            redirectingToLogin = true;
            redirectToLogin();
        }
    }

    @Override
    protected void onDestroy() {
        ApiClient.setSessionExpiredListener(null);
        super.onDestroy();
    }

    public void selectBottomTab(int destinationId, @Nullable Bundle args) {
        if (destinationId == R.id.communityHostFragment && args != null && args.containsKey(CommunityHostFragment.ARG_INITIAL_TAB)) {
            openCommunityTab(args.getInt(CommunityHostFragment.ARG_INITIAL_TAB, CommunityHostFragment.TAB_ANNOUNCEMENTS));
            return;
        }
        navigateTopLevel(destinationId, args);
    }

    public void openCommunityTab(int subTabIndex) {
        if (navViewModel != null) {
            navViewModel.requestCommunityTab(subTabIndex);
        }
        Bundle args = new Bundle();
        args.putInt(CommunityHostFragment.ARG_INITIAL_TAB, subTabIndex);
        navigateTopLevel(R.id.communityHostFragment, args);
    }

    private boolean navigateTopLevel(int destinationId, @Nullable Bundle args) {
        if (navController == null) return false;
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destinationId) {
            return true;
        }
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
        try {
            navController.navigate(destinationId, args, options);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    public void performLogout() {
        NotificationSocketManager.getInstance().disconnect();
        ApiClient.clearToken(this);
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
