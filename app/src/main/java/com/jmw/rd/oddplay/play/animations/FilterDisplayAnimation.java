package com.jmw.rd.oddplay.play.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.jmw.rd.oddplay.Utils;


public class FilterDisplayAnimation {
    final private ViewGroup.LayoutParams params;
    private boolean animationRunning = false;
    final private View view;
    final private int height;
    final private View focusGrabber;
    final private boolean opening;
    final private Context context;

    public FilterDisplayAnimation(Context context, View view, View focusGrabber, int height, ViewGroup.LayoutParams params) {
        this.context = context;
        this.params = params;
        this.height = height;
        this.view = view;
        this.focusGrabber = focusGrabber;
        opening = (params.height == 0);
    }

    public void runAnimation() {
        if (!animationRunning) {
            animationRunning = true;
            ValueAnimator animationHeight;
            if (opening) {
                animationHeight = ValueAnimator.ofInt(0, height);
            } else {
                animationHeight = ValueAnimator.ofInt(height, 0);
            }
            animationHeight.setDuration(200);
            animationHeight.addUpdateListener(new UpdateListener());
            animationHeight.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animationRunning = false;
                    if (opening) {
                        focusGrabber.requestFocus();
                        Utils.showKeyboard(context, focusGrabber);
                    } else {
                        focusGrabber.clearFocus();
                        Utils.hideKeyboard(context, focusGrabber);
                    }
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
            animationHeight.start();
        }
    }

    private class UpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            params.height = (int)valueAnimator.getAnimatedValue();
            view.setLayoutParams(params);
        }

    }
}
