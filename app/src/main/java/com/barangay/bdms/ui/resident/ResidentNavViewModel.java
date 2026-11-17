package com.barangay.bdms.ui.resident;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Coordinates Community sub-tab switches requested from Notifications (R9), Dashboard (R3),
 * or post-submit Emergency flow (R11 -> R12) without stacking duplicate Community destinations.
 */
public class ResidentNavViewModel extends ViewModel {

    private final MutableLiveData<Integer> pendingCommunityTab = new MutableLiveData<>(null);

    public void requestCommunityTab(int tabIndex) {
        pendingCommunityTab.setValue(tabIndex);
    }

    public LiveData<Integer> getPendingCommunityTab() {
        return pendingCommunityTab;
    }

    @Nullable
    public Integer consumePendingCommunityTab() {
        Integer value = pendingCommunityTab.getValue();
        if (value != null) {
            pendingCommunityTab.setValue(null);
        }
        return value;
    }
}
