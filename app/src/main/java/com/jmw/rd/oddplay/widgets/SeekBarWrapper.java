package com.jmw.rd.oddplay.widgets;

import android.widget.SeekBar;

import com.jmw.rd.oddplay.play.animations.SeekBarAnimation;

public class SeekBarWrapper {
    private static final int SEEK_BAR_INCREMENTS = 10000;

    private final SeekBar seekBar;
    private final SeekBarAnimation seekBarAnimation;
    private boolean userCurrentlyScrolling = false;

    public SeekBarWrapper(SeekBar seekBar, SeekListener seekListener) {
        this.seekBar = seekBar;
        seekBar.setMax(SEEK_BAR_INCREMENTS);
        seekBar.setOnSeekBarChangeListener(new MySeekListener(seekListener));
        seekBarAnimation = new SeekBarAnimation(seekBar);
    }

    public void setPosition(float newPositionRatio, boolean animate) {
        long oldPosition = this.seekBar.getProgress();
        if (seekBarAnimation.isRunning()) {
            return;
        }
        if (!userCurrentlyScrolling) {
            if (animate) {
                seekBarAnimation.runAnimation((int) ((float) oldPosition),
                        (int) (newPositionRatio * SEEK_BAR_INCREMENTS));
            } else {
                seekBar.setProgress((int) (newPositionRatio * SEEK_BAR_INCREMENTS));
            }
        }
    }

    public void userScrolling(boolean update) {
        this.userCurrentlyScrolling = update;
    }

    public boolean isUserScrolling() {
        return this.userCurrentlyScrolling;
    }

    public interface SeekListener {
        void onProgress(float per10Mille, boolean fromUser);

        void onStartTouch();

        void onStopTouch(float per10Mille);

    }

    private class MySeekListener implements SeekBar.OnSeekBarChangeListener {
        private final SeekListener listener;

        MySeekListener(SeekListener listener) {
            this.listener = listener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            listener.onProgress((float) seekBar.getProgress() / SEEK_BAR_INCREMENTS, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            listener.onStartTouch();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            listener.onStopTouch((float) seekBar.getProgress() / SEEK_BAR_INCREMENTS);
        }
    }

}

