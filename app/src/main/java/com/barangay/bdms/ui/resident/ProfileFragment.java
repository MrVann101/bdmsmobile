package com.barangay.bdms.ui.resident;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.model.Profile;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.network.AuthenticatedImageLoader;
import com.barangay.bdms.network.NotificationSocketManager;
import com.barangay.bdms.network.SessionManager;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.NotificationBellView;
import com.barangay.bdms.ui.common.PhotoPickerHelper;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements NotificationSocketManager.Listener {

    private BdmsRepository repository;
    private PhotoPickerHelper photoPickerHelper;

    private SwipeRefreshLayout swipeRefresh;
    private LoadingOverlay loadingOverlay;
    private NotificationBellView notificationBell;
    private ImageView ivAvatar;
    private TextView tvFullName, tvSubtitle, tvEmail, tvMobile, tvPurok, tvAddress, tvPrivateRecords;

    @Nullable
    private Profile currentProfile;
    private boolean skipNextResumeBellRefresh = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPickerHelper = new PhotoPickerHelper(this, new PhotoPickerHelper.OnPhotoPickedListener() {
            @Override
            public void onPhotoPicked(@NonNull Uri uri) {
                if (isAdded()) {
                    uploadAvatar(uri);
                }
            }

            @Override
            public void onPhotoError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
        photoPickerHelper.restoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoPickerHelper != null) {
            photoPickerHelper.saveInstanceState(outState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r13_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefreshProfile);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        notificationBell = view.findViewById(R.id.notificationBell);
        FrameLayout avatarPicker = view.findViewById(R.id.layoutAvatarPicker);
        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvFullName = view.findViewById(R.id.tvProfileFullName);
        tvSubtitle = view.findViewById(R.id.tvProfileSubtitle);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvMobile = view.findViewById(R.id.tvProfileMobile);
        tvPurok = view.findViewById(R.id.tvProfilePurok);
        tvAddress = view.findViewById(R.id.tvProfileAddress);
        tvPrivateRecords = view.findViewById(R.id.tvPrivateRecords);
        MaterialButton btnEditMobile = view.findViewById(R.id.btnEditMobile);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        notificationBell.setOnBellClickListener(v ->
                NavUtils.safeNavigate(this, R.id.action_profile_to_notifications)
        );
        avatarPicker.setOnClickListener(v -> photoPickerHelper.showPickerDialog(requireContext()));
        btnEditMobile.setOnClickListener(v -> showEditMobileDialog());
        btnLogout.setOnClickListener(v -> confirmLogout());

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> {
            fetchProfile();
            refreshUnreadBell();
        });

        skipNextResumeBellRefresh = true;
        fetchProfile();
        refreshUnreadBell();
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationSocketManager.getInstance().addListener(this);
        if (skipNextResumeBellRefresh) {
            skipNextResumeBellRefresh = false;
        } else {
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

    private void fetchProfile() {
        if (!swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getMyProfile().enqueue(new Callback<Profile>() {
            @Override
            public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    syncSessionFromProfile(currentProfile);
                    bindProfile(currentProfile);
                } else {
                    Toast.makeText(
                            requireContext(),
                            ApiErrorUtils.parseErrorMessage(response, "Unable to load profile."),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error loading profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncSessionFromProfile(@NonNull Profile profile) {
        SessionManager session = SessionManager.getInstance(requireContext());
        ApiClient.saveSession(
                requireContext(),
                session.getToken(),
                profile.getId() != null ? profile.getId().toString() : session.getUserId(),
                profile.getRole() != null ? profile.getRole() : session.getRole(),
                profile.getPurok(),
                profile.getFullName(),
                profile.getEmail()
        );
    }

    private void bindProfile(@NonNull Profile p) {
        tvFullName.setText(p.getFullName() != null ? p.getFullName() : "Resident");
        tvSubtitle.setText("@" + (p.getUsername() != null ? p.getUsername() : "resident")
                + " • " + (p.getPurok() != null ? p.getPurok() : "Unassigned"));
        tvEmail.setText(p.getEmail() != null ? p.getEmail() : "—");
        tvMobile.setText(p.getMobileNumber() != null && !p.getMobileNumber().isEmpty()
                ? p.getMobileNumber() : "Not provided");
        tvPurok.setText(p.getPurok() != null ? p.getPurok() : "Unassigned");

        StringBuilder addr = new StringBuilder();
        addr.append(p.getAddress() != null && !p.getAddress().isEmpty() ? p.getAddress() : "No street address");
        if (p.getResidencyType() != null && !p.getResidencyType().isEmpty()) {
            addr.append("\nType: ").append(p.getResidencyType());
        }
        if (p.getResidencyPeriod() != null && !p.getResidencyPeriod().isEmpty()) {
            addr.append(" (").append(p.getResidencyPeriod()).append(")");
        }
        tvAddress.setText(addr.toString());

        String ctc = p.getCtcNumber() != null && !p.getCtcNumber().isEmpty() ? p.getCtcNumber() : "None";
        String ctcOr = p.getCtcOrNumber() != null && !p.getCtcOrNumber().isEmpty() ? p.getCtcOrNumber() : "None";
        String sectors = p.getSector() != null && !p.getSector().isEmpty()
                ? TextUtils.join(", ", p.getSector()) : "None";
        String disabilities = p.getDisabilities() != null && !p.getDisabilities().isEmpty()
                ? TextUtils.join(", ", p.getDisabilities()) : "None";

        tvPrivateRecords.setText(
                "CTC Number: " + ctc
                        + "\nCTC OR Number: " + ctcOr
                        + "\nSectors: " + sectors
                        + "\nDeclared Disabilities: " + disabilities
        );

        AuthenticatedImageLoader.load(requireContext(), p.getAvatarUrl(), ivAvatar, R.drawable.ic_bdms_logo, 320);
    }

    private void showEditMobileDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("09XXXXXXXXX or +639XXXXXXXXX");
        if (currentProfile != null && currentProfile.getMobileNumber() != null) {
            input.setText(currentProfile.getMobileNumber());
        }
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Mobile Number")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String nextMobile = input.getText() != null ? input.getText().toString().trim() : "";
                    updateMobileNumber(nextMobile);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMobileNumber(@NonNull String mobile) {
        String compact = mobile.replaceAll("[\\s()-]", "");
        if (!compact.matches("^(\\+?63|0)9\\d{9}$")) {
            Toast.makeText(
                    requireContext(),
                    "Enter a valid Philippine mobile number (e.g. 09171234567 or +639171234567).",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("mobileNumber", compact);

        loadingOverlay.show();
        repository.updateMyProfile(body).enqueue(new Callback<Profile>() {
            @Override
            public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    bindProfile(currentProfile);
                    Toast.makeText(requireContext(), "Mobile number updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            requireContext(),
                            ApiErrorUtils.parseErrorMessage(response, "Unable to update profile."),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadAvatar(@NonNull Uri uri) {
        final String oldAvatarUrl = currentProfile != null ? currentProfile.getAvatarUrl() : null;
        loadingOverlay.show();

        PhotoPickerHelper.createMultipartPartAsync(requireContext(), "avatar", uri, new PhotoPickerHelper.PartsCallback() {
            @Override
            public void onPartsReady(@NonNull List<MultipartBody.Part> parts) {
                if (!isAdded() || parts.isEmpty()) return;
                repository.uploadAvatar(parts.get(0)).enqueue(new Callback<Profile>() {
                    @Override
                    public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();
                        if (response.isSuccessful() && response.body() != null) {
                            if (oldAvatarUrl != null) {
                                AuthenticatedImageLoader.invalidate(oldAvatarUrl);
                            }
                            currentProfile = response.body();
                            AuthenticatedImageLoader.invalidate(currentProfile.getAvatarUrl());
                            bindProfile(currentProfile);
                            Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(
                                    requireContext(),
                                    ApiErrorUtils.parseErrorMessage(response, "Unable to upload avatar."),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        loadingOverlay.hide();
                        Toast.makeText(requireContext(), "Upload failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onPartsError(@NonNull String message) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your BDMS account?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    if (getActivity() instanceof ResidentMainActivity) {
                        ((ResidentMainActivity) getActivity()).performLogout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
