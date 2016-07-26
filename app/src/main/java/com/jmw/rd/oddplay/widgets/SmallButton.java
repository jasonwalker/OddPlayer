package com.jmw.rd.oddplay.widgets;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.jmw.rd.oddplay.R;

public class SmallButton extends Button {
    protected TransitionDrawable transition;
    private OnTouchListener onTouchListener;

    public SmallButton(Context context) {
        super(context);
        init();
    }

    public SmallButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmallButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        super.setOnTouchListener(new SmallButtonOnTouchListener());
        this.transition = (TransitionDrawable) ContextCompat.getDrawable(getContext(), R.drawable.press_button_transition);
        this.setBackground(this.transition);
        this.setPadding(15, 0, 15, 0);
    }

    @Override
    public void setOnTouchListener(OnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            getBackground().setColorFilter(null);
        } else {
            getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
        }
    }

    private class SmallButtonOnTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                transition.startTransition(60);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                transition.reverseTransition(60);
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                return SmallButton.this.onTouchListener != null &&
                        SmallButton.this.onTouchListener.onTouch(v, event);
            } else {
                return false;
            }


        }
    }
}
