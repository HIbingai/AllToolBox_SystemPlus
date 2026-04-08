package com.atb.systemplus.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.Button;
import com.atb.systemplus.R;

public final class CyberButton extends Button {

    public CyberButton(Context context) {
        super(context);
        init();
    }

    public CyberButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CyberButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setAllCaps(false);
        setTextColor(Color.parseColor("#D9FBFF"));
        setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.cyber_button_text_size));
        setLetterSpacing(0.08f);
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.cyber_button_min_height));
        int paddingH = getResources().getDimensionPixelSize(R.dimen.cyber_button_padding_h);
        int paddingV = getResources().getDimensionPixelSize(R.dimen.cyber_button_padding_v);
        setPadding(paddingH, paddingV, paddingH, paddingV);
        setBackgroundResource(R.drawable.bg_cyber_button);
        setShadowLayer(getResources().getDimension(R.dimen.cyber_button_shadow_radius), 0f, 0f, Color.parseColor("#6600E5FF"));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        }
        return super.onTouchEvent(event);
    }
}
