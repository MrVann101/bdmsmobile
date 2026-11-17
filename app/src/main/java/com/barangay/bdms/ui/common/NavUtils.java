package com.barangay.bdms.ui.common;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.barangay.bdms.R;
import com.barangay.bdms.network.AuthenticatedImageLoader;

/**
 * Navigation safety guards and shared photo preview helpers for Resident Fragments.
 */
public final class NavUtils {

    private NavUtils() {
    }

    public static void safeNavigate(@NonNull Fragment fragment, @IdRes int actionOrDestId) {
        safeNavigate(fragment, actionOrDestId, null);
    }

    public static void safeNavigate(
            @NonNull Fragment fragment,
            @IdRes int actionOrDestId,
            @Nullable Bundle args
    ) {
        if (!fragment.isAdded()) return;
        NavController controller = NavHostFragment.findNavController(fragment);
        safeNavigate(controller, actionOrDestId, args);
    }

    public static void safeNavigateFromActivity(
            @NonNull Fragment fragment,
            @IdRes int navHostId,
            @IdRes int actionOrDestId,
            @Nullable Bundle args
    ) {
        if (!fragment.isAdded()) return;
        NavController controller = Navigation.findNavController(fragment.requireActivity(), navHostId);
        safeNavigate(controller, actionOrDestId, args);
    }

    public static void safeNavigate(
            @NonNull NavController controller,
            @IdRes int actionOrDestId,
            @Nullable Bundle args
    ) {
        NavDestination current = controller.getCurrentDestination();
        if (current == null || current.getId() == actionOrDestId) {
            return;
        }
        if (current.getAction(actionOrDestId) != null || controller.getGraph().findNode(actionOrDestId) != null) {
            try {
                controller.navigate(actionOrDestId, args);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // Prevent crash on rapid double-tap transitions
            }
        }
    }

    public static void safeNavigateUp(@NonNull Fragment fragment) {
        if (!fragment.isAdded()) return;
        try {
            NavHostFragment.findNavController(fragment).navigateUp();
        } catch (IllegalStateException ignored) {
        }
    }

    public static void showAuthenticatedImageDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String serverPath
    ) {
        ImageView preview = new ImageView(context);
        int heightPx = Math.round(320 * context.getResources().getDisplayMetrics().density);
        preview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        AuthenticatedImageLoader.load(context, serverPath, preview, R.drawable.ic_bdms_logo, 1280);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(preview)
                .setPositiveButton("Close", null)
                .show();
    }
}
