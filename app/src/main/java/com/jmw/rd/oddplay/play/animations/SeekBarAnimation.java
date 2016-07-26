package com.jmw.rd.oddplay.play.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.widget.SeekBar;

public class SeekBarAnimation {

    private final SeekBar seekBar;
    private boolean seekBarAnimationRunning;

    public SeekBarAnimation(SeekBar seekBar) {
        this.seekBar = seekBar;
    }

    public void runAnimation(int oldPosition, int currentPosition) {
        seekBarAnimationRunning = true;
        ValueAnimator animation = ValueAnimator.ofInt(oldPosition, currentPosition);
        animation.setDuration(200);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                seekBar.setProgress((Integer) valueAnimator.getAnimatedValue());
            }
        });
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                seekBarAnimationRunning = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                seekBarAnimationRunning = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                seekBarAnimationRunning = true;
            }
        });
        animation.start();
    }

    public boolean isRunning() {
        return seekBarAnimationRunning;
    }
}
