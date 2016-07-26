package com.jmw.rd.oddplay.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.jmw.rd.oddplay.R;

public class RotatingRefreshIcon {
    private MenuItem item;
    private boolean stopRefreshing;

    public RotatingRefreshIcon(Context context, MenuItem item) {
        if (item.getActionView() == null) {
            this.item = item;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams")
            ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_animate, null);
            Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_refresh);
            rotation.setAnimationListener(new RefreshAnimationListener());
            rotation.setRepeatCount(Animation.INFINITE);
            iv.startAnimation(rotation);
            stopRefreshing = false;
            item.setActionView(iv);
        }
    }

    public void setOnClickListener(View.OnClickListener listener) {
        item.getActionView().setOnClickListener(listener);
    }

    public void stop() {
        stopRefreshing = true;
    }

    private class RefreshAnimationListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (stopRefreshing) {
                if (item != null) {
                    View actionView = item.getActionView();
                    if (actionView != null) {
                        actionView.clearAnimation();
                        item.setActionView(null);
                    }
                }
            }
        }
    }

}
