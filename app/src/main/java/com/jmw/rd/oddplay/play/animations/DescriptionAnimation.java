package com.jmw.rd.oddplay.play.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.widget.LinearLayout;

public class DescriptionAnimation {

    private final LinearLayout.LayoutParams params;
    private final LinearLayout layout;
    private boolean animationRunning = false;

    public DescriptionAnimation(LinearLayout layout, LinearLayout.LayoutParams params) {
        this.params = params;
        this.layout = layout;
    }

    public void runAnimation() {
        if (!animationRunning) {
            animationRunning = true;
            ValueAnimator animation;
            if (params.weight == 0.0) {
                animation = ValueAnimator.ofFloat(0f, 1f);
            } else {
                animation = ValueAnimator.ofFloat(1f, 0f);
            }
            animation.setDuration(200);

            animation.addUpdateListener(new UpdateListener());
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animationRunning = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    animationRunning = false;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    animationRunning = true;
                }
            });
            animation.start();
        }
    }

    private class UpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            params.weight = (Float) valueAnimator.getAnimatedValue();
            layout.setLayoutParams(params);
        }

    }
}


