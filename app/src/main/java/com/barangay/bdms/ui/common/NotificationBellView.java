package com.barangay.bdms.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.barangay.bdms.R;

/**
 * Toolbar bell icon with an unread count badge for authenticated Resident and Staff screens.
 */
public class NotificationBellView extends FrameLayout {

    private ImageView bellIcon;
    private TextView badgeView;
    private int unreadCount = 0;
    private boolean showCount = true;

    public NotificationBellView(Context context) {
        super(context);
        init(context);
    }

    public NotificationBellView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NotificationBellView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClickable(true);
        setFocusable(true);
        setContentDescription("Notifications");

        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)) {
            setBackgroundResource(outValue.resourceId);
        }

        int minSize = dpToPx(48);
        setMinimumWidth(minSize);
        setMinimumHeight(minSize);

        bellIcon = new ImageView(context);
        bellIcon.setImageResource(R.drawable.ic_notifications_bell);
        ImageViewCompat.setImageTintList(
                bellIcon,
                ContextCompat.getColorStateList(context, R.color.colorTextPrimary)
        );
        int iconSize = dpToPx(24);
        LayoutParams iconParams = new LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        bellIcon.setLayoutParams(iconParams);
        addView(bellIcon);

        badgeView = new TextView(context);
        badgeView.setBackgroundResource(R.drawable.bg_notification_badge);
        badgeView.setTextColor(ContextCompat.getColor(context, R.color.white));
        badgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        badgeView.setTypeface(null, Typeface.BOLD);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setIncludeFontPadding(false);
        int badgeMinSize = dpToPx(16);
        badgeView.setMinWidth(badgeMinSize);
        badgeView.setMinHeight(badgeMinSize);
        int hPad = dpToPx(4);
        badgeView.setPadding(hPad, 0, hPad, 0);

        LayoutParams badgeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, badgeMinSize);
        badgeParams.gravity = Gravity.TOP | Gravity.END;
        badgeParams.topMargin = dpToPx(6);
        badgeParams.rightMargin = dpToPx(6);
        badgeView.setLayoutParams(badgeParams);
        badgeView.setVisibility(GONE);
        addView(badgeView);
    }

    public void setUnreadCount(int count) {
        this.unreadCount = Math.max(0, count);
        updateBadge();
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setShowCount(boolean showCount) {
        this.showCount = showCount;
        updateBadge();
    }

    public void setOnBellClickListener(@Nullable OnClickListener listener) {
        setOnClickListener(listener);
    }

    private void updateBadge() {
        if (unreadCount <= 0) {
            badgeView.setVisibility(GONE);
            setContentDescription("Notifications");
            return;
        }
        setContentDescription("Notifications, " + unreadCount + " unread");
        badgeView.setVisibility(VISIBLE);
        if (!showCount) {
            badgeView.setText("");
            LayoutParams lp = (LayoutParams) badgeView.getLayoutParams();
            int dotSize = dpToPx(10);
            lp.width = dotSize;
            lp.height = dotSize;
            badgeView.setMinWidth(dotSize);
            badgeView.setMinHeight(dotSize);
            badgeView.setPadding(0, 0, 0, 0);
            badgeView.setLayoutParams(lp);
        } else {
            LayoutParams lp = (LayoutParams) badgeView.getLayoutParams();
            int badgeMinSize = dpToPx(16);
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.height = badgeMinSize;
            badgeView.setMinWidth(badgeMinSize);
            badgeView.setMinHeight(badgeMinSize);
            int hPad = dpToPx(4);
            badgeView.setPadding(hPad, 0, hPad, 0);
            badgeView.setLayoutParams(lp);
            badgeView.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }
}
