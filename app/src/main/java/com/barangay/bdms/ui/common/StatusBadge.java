package com.barangay.bdms.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.barangay.bdms.R;

import java.util.Locale;

public class StatusBadge extends AppCompatTextView {

    public StatusBadge(Context context) {
        super(context);
        init();
    }

    public StatusBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusBadge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setPadding(24, 8, 24, 8);
        setTextSize(12);
        setTypeface(null, Typeface.BOLD);
        setBackgroundResource(R.drawable.bg_status_badge);
    }

    public void setStatus(String status) {
        setStatus(status, null);
    }

    public void setStatus(String status, @androidx.annotation.Nullable String labelOverride) {
        String raw = status != null && !status.trim().isEmpty() ? status.trim() : "pending";
        String normalized = raw.toLowerCase(Locale.US);
        String display = (labelOverride != null && !labelOverride.trim().isEmpty())
                ? labelOverride.trim()
                : raw.replace('_', ' ').toUpperCase(Locale.US);
        setText(display);

        int bgRes;
        int textRes;

        switch (normalized) {
            case "processing":
            case "reviewing":
            case "verified":
            case "relief_dispatched":
                bgRes = R.color.statusProcessingBg;
                textRes = R.color.statusProcessingText;
                break;
            case "ready":
                bgRes = R.color.statusReadyBg;
                textRes = R.color.statusReadyText;
                break;
            case "completed":
            case "resolved":
            case "sent":
                bgRes = R.color.statusCompletedBg;
                textRes = R.color.statusCompletedText;
                break;
            case "rejected":
            case "dismissed":
            case "failed":
            case "high":
                bgRes = R.color.statusRejectedBg;
                textRes = R.color.statusRejectedText;
                break;
            case "cancelled":
            case "canceled":
                bgRes = R.color.statusCancelledBg;
                textRes = R.color.statusCancelledText;
                break;
            case "pending":
            case "reported":
            default:
                bgRes = R.color.statusPendingBg;
                textRes = R.color.statusPendingText;
                break;
        }

        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(getContext(), bgRes));
        setTextColor(ContextCompat.getColor(getContext(), textRes));
    }
}
