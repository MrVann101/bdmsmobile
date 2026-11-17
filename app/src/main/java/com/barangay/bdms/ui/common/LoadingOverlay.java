package com.barangay.bdms.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;

import com.barangay.bdms.R;

public class LoadingOverlay extends FrameLayout {

    public LoadingOverlay(Context context) {
        super(context);
        init();
    }

    public LoadingOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.overlayScrim));
        setClickable(true);
        setFocusable(true);
        setVisibility(GONE);

        ProgressBar progressBar = new ProgressBar(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(params);

        addView(progressBar);
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }
}
