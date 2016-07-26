package com.jmw.rd.oddplay.widgets;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.jmw.rd.oddplay.R;

public class HiliteButton extends SmallButton {

    public HiliteButton(Context context) {
        super(context);
    }

    public HiliteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HiliteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();
        this.transition = (TransitionDrawable) ContextCompat.getDrawable(getContext(), R.drawable.hilite_press_button_transition);
        this.setBackground(this.transition);
        this.setTextColor(getResources().getColor(R.color.black));
    }

}
