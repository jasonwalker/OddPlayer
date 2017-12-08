package com.jmw.rd.oddplay.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.jmw.rd.oddplay.ImageHolder;
import com.jmw.rd.oddplay.R;

public class PlayImageWrapper implements View.OnTouchListener {
    private static final int TRANSITION_TIME_MS = 200;
    private final Bitmap playBitmap;
    private final Bitmap pauseBitmap;
    private final ImageView pushStateImage;
    private final ImageView pausePlayImage;
    private final TransitionDrawable buttonBackgroundTransition;
    private View.OnTouchListener outsideTouchListener;
    private final int HIGHLIGHT_COLOR;
    private boolean playState;
    private boolean noStateInfoYet;

    public PlayImageWrapper(Context context, ImageView imageView, ImageView pausePlayImage) {
        this.pushStateImage = imageView;
        this.pausePlayImage = pausePlayImage;
        playBitmap = ImageHolder.getImageResource(context, R.drawable.play_only);
        pauseBitmap = ImageHolder.getImageResource(context, R.drawable.pause_only);

        HIGHLIGHT_COLOR = ContextCompat.getColor(context, R.color.button_highlight);
        buttonBackgroundTransition = (TransitionDrawable) pushStateImage.getBackground();
        pausePlayImage.setImageBitmap(playBitmap);
        playState = false;
        noStateInfoYet = true;
        this.pushStateImage.setOnTouchListener(this);
        pausePlayImage.setColorFilter(ContextCompat.getColor(context, R.color.button_disable));
    }

    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.outsideTouchListener = onTouchListener;
    }

    public boolean isPlaying() {
        return this.playState;
    }

    public void setPlaying(boolean playing) {
        if (noStateInfoYet) {
            noStateInfoYet = false;
            pausePlayImage.clearColorFilter();
        }
        if (playing == playState) {
            return;
        }
        playState = playing;
        pausePlayImage.setImageBitmap(playing ? pauseBitmap : playBitmap);
        if (playing) {
            buttonBackgroundTransition.startTransition(TRANSITION_TIME_MS);
        } else {
            buttonBackgroundTransition.reverseTransition(TRANSITION_TIME_MS);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            this.pushStateImage.setColorFilter(HIGHLIGHT_COLOR);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            this.pushStateImage.clearColorFilter();
        }
        outsideTouchListener.onTouch(v, event);
        return true;
    }
}
