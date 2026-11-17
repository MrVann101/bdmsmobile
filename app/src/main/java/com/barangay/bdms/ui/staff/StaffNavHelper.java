package com.barangay.bdms.ui.staff;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class StaffNavHelper {
    public static void setupBottomNav(Activity activity) {
        // Post to ensure decor view is fully inflated
        activity.getWindow().getDecorView().post(() -> setupNavViews(activity, activity.getWindow().getDecorView().getRootView()));
    }

    private static void setupNavViews(Activity activity, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup && isBottomNavItem((ViewGroup) child)) {
                    String label = getNavItemLabel((ViewGroup) child);
                    if (label != null) {
                        child.setClickable(true);
                        child.setFocusable(true);
                        child.setOnClickListener(v -> {
                            if (label.contains("Dashboard")) {
                                if (!(activity instanceof StaffMainActivity)) {
                                    activity.startActivity(new Intent(activity, StaffMainActivity.class));
                                    activity.finish();
                                }
                            } else if (label.contains("Complaint")) {
                                if (!(activity instanceof StaffComplaintQueueActivity)) {
                                    activity.startActivity(new Intent(activity, StaffComplaintQueueActivity.class));
                                    activity.finish();
                                }
                            } else if (label.contains("Emergency")) {
                                if (!(activity instanceof StaffEmergencyDashboardActivity)) {
                                    activity.startActivity(new Intent(activity, StaffEmergencyDashboardActivity.class));
                                    activity.finish();
                                }
                            } else if (label.contains("Broadcast")) {
                                if (!(activity instanceof StaffAnnouncementActivity)) {
                                    activity.startActivity(new Intent(activity, StaffAnnouncementActivity.class));
                                    activity.finish();
                                }
                            } else if (label.contains("More") || label.contains("Admin")) {
                                if (!(activity instanceof AdminHubActivity)) {
                                    activity.startActivity(new Intent(activity, AdminHubActivity.class));
                                    activity.finish();
                                }
                            }
                        });
                    }
                } else {
                    setupNavViews(activity, child);
                }
            }
        }
    }

    private static boolean isBottomNavItem(ViewGroup group) {
        boolean hasImg = false;
        boolean hasTv = false;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) hasImg = true;
            if (child instanceof TextView) {
                String text = ((TextView) child).getText().toString();
                if (text.contains("Dashboard") || text.contains("Complaint") || text.contains("Emergency") || text.contains("Broadcast") || text.contains("More") || text.contains("Admin")) {
                    hasTv = true;
                }
            }
        }
        return hasImg && hasTv;
    }

    private static String getNavItemLabel(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                String text = ((TextView) child).getText().toString();
                if (text.contains("Dashboard") || text.contains("Complaint") || text.contains("Emergency") || text.contains("Broadcast") || text.contains("More") || text.contains("Admin")) {
                    return text;
                }
            }
        }
        return null;
    }
}
