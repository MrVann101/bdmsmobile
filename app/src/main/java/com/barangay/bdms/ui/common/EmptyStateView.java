package com.barangay.bdms.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import com.barangay.bdms.R;

public class EmptyStateView extends LinearLayout {

    private TextView messageView;

    public EmptyStateView(Context context) {
        super(context);
        init();
    }

    public EmptyStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmptyStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(32, 64, 32, 64);

        messageView = new AppCompatTextView(getContext());
        messageView.setGravity(Gravity.CENTER);
        messageView.setTextSize(16);
        messageView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextSecondary));
        messageView.setText("No data available");

        addView(messageView);
    }

    public void setMessage(String message) {
        if (messageView != null) {
            messageView.setText(message);
        }
    }
}
