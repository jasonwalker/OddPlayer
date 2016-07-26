package com.jmw.rd.oddplay.play;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.jmw.rd.oddplay.PodPage;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.episode.EpisodeController;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.PlayImageWrapper;
import com.jmw.rd.oddplay.widgets.SeekBarWrapper;


public class PlayViewPage extends PodPage {
    private static final int OFF_SCREEN_FRAGMENTS = 3;
    private TextView timingText;
    private TextView countText;
    private PlayStateReceiver stateReceiver;
    private PlayImageWrapper playImage;
    private long currentEpisodeDuration = -1;
    private String currentEpisodeDurationString;
    private ViewPager playPager;
    private PlayInfoReceiver infoReceiver;
    private PlayPagerAdapter pagerAdapter;
    private EpisodeListChangedReceiver episodeListChangedReceiver;
    private SeekBarWrapper seekBar;
    private boolean userActivelyScrolling = false;
    private EpisodeController episodeController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        episodeController = new EpisodeController(this.activity);
        pagerAdapter = new PlayPagerAdapter(this.getChildFragmentManager(), activity);
        this.episodeListChangedReceiver = new EpisodeListChangedReceiver();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.main_view_play, container, false);
        playPager = (ViewPager) rootView.findViewById(R.id.playDisplayPager);
        playPager.setId(R.id.playDisplayPager);
        // using setAdapter here to avoid bug that looks like #19917
        playPager.setAdapter(pagerAdapter);
        playPager.setOffscreenPageLimit(OFF_SCREEN_FRAGMENTS);
        playPager.addOnPageChangeListener(new PageChangeListener());
        InitPage initPage = new InitPage();
        initPage.execute();
        timingText = (TextView) rootView.findViewById(R.id.timingText);
        countText = (TextView) rootView.findViewById(R.id.countText);
        this.infoReceiver = new PlayInfoReceiver();


        seekBar = new SeekBarWrapper((SeekBar) rootView.findViewById(R.id.progressBar), new PlaySeekListener());
        this.playImage = new PlayImageWrapper(this.activity.getApplicationContext(),
                (ImageView) rootView.findViewById(R.id.playStateImage),
                (ImageView) rootView.findViewById(R.id.pausePlayImage));
        this.stateReceiver = new PlayStateReceiver();

        //playImage.setPlaying(false, false);
        playImage.setOnTouchListener(new PlayToggleListener());
        /*toggleDescriptionButton = (ImageButton) rootView.findViewById(R.id.toggleDescriptionButton);
        toggleDescriptionButton.setOnTouchListener(new ToggleDescriptionListener());
        */
        Button backButton = (Button) rootView.findViewById(R.id.backButton);
        backButton.setOnTouchListener(new BackTouchListener());
        Button forwardButton = (Button) rootView.findViewById(R.id.forwardButton);
        forwardButton.setOnTouchListener(new ForwardTouchListener());
        return rootView;
    }

    private class InitPage extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            pagerAdapter.notifyDataSetChanged();
        }


        @Override
        protected Void doInBackground(Void... unused) {
            // two below just initialize count from db
            if (pagerAdapter.getCount() > 0) {
                pagerAdapter.getItemId(0);
            }

            //toggleDescriptionRotationDown = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.rotate_halfway);
            //toggleDescriptionRotationUp = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.rotate_half_back);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // pager adapter initalized by 'getCount' call above
            playPager.setCurrentItem(StorageUtil.getStorage(activity).fast.getCurrentEpisodeNumber(), false);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        episodeController.registerForEpisodeListChange(this.episodeListChangedReceiver);
        playController.registerForPlayInfo(this.infoReceiver);
        playController.registerForPlayState(this.stateReceiver);
        pagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        playController.unregisterReceiver(this.stateReceiver);
        playController.unregisterReceiver(this.infoReceiver);

        LocalBroadcastManager.getInstance(activity).unregisterReceiver(this.episodeListChangedReceiver);
    }

    private void goBack() {
        playController.rewind();
    }

    private void goForward() {
        playController.fastforward();
    }

    private void setTimingInfo(long location, boolean animate) {
        seekBar.setPosition(((float) location) / currentEpisodeDuration, animate);
        if (!seekBar.isUserScrolling()) {
            this.timingText.setText(String.format("%1$s/%2$s", Utils.formatTime(location), currentEpisodeDurationString));
        }
    }

    private void setCountInfo(int currentNumber, int total) {
        this.countText.setText(String.format("(%1$d/%2$d)", currentNumber, total));
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                playController.goToEpisodeNumber(playPager.getCurrentItem());
            }
        }
    }

    private class PlayToggleListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                playController.togglePlay();
            }
            return true;
        }
    }

    private class EpisodeListChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            pagerAdapter.notifyDataSetChanged();
            playController.promptForPlayInfo();
        }
    }

    private class PlayInfoReceiver extends BroadcastReceiver {

        public PlayInfoReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(PlayController.INFO_EXCEPTION, false)) {
                String msg = intent.getStringExtra(PlayController.INFO_ERROR_MSG);
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                return;
            }
            int currentNumber = intent.getIntExtra(PlayController.INFO_EPISODE_NUMBER, 0);
            int totalEpisodes = intent.getIntExtra(PlayController.INFO_TOTAL, 0);
            currentEpisodeDuration = intent.getLongExtra(PlayController.INFO_DURATION, 0);
            currentEpisodeDurationString = Utils.formatTime(currentEpisodeDuration);
            boolean autoSwitchingEpisode = currentNumber != playPager.getCurrentItem() || currentEpisodeDuration == -1;
            boolean weCausedSwitch = intent.getBooleanExtra(PlayController.INFO_EXTERNAL_GOTO, false);
            if (autoSwitchingEpisode) {
                if (currentNumber >= totalEpisodes) {
                    return;
                }
                if (!weCausedSwitch) {
                    playPager.setCurrentItem(currentNumber, true);
                }
            }
            PlayViewPage.this.setCountInfo(currentNumber + 1, totalEpisodes);
        }
    }

    private class PlayStateReceiver extends BroadcastReceiver {
        private boolean playing;

        public PlayStateReceiver() {
            playing = playImage.isPlaying();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (userActivelyScrolling) {
                return;
            }
            boolean currState = intent.getBooleanExtra(PlayController.STATE_PLAYING, false);
            // doing conditional just to spare a function call
            if (playing != currState) {
                playImage.setPlaying(currState, true);
                playing = currState;
            }
            boolean animate = intent.getBooleanExtra(PlayController.STATE_EPISODE_CHANGE, false);
            PlayViewPage.this.setTimingInfo(intent.getLongExtra(PlayController.STATE_LOCATION, 0), animate);
        }
    }

    private class PlaySeekListener implements SeekBarWrapper.SeekListener {

        @Override
        public void onProgress(float per10Mille, boolean fromUser) {
            if (fromUser) {
                PlayViewPage.this.timingText.setText(String.format("%1$s/%2$s",
                        Utils.formatTime((int) (per10Mille * currentEpisodeDuration)),
                        currentEpisodeDurationString));
            }
        }

        @Override
        public void onStartTouch() {
            userActivelyScrolling = true;
            PlayViewPage.this.timingText.setTextColor(ContextCompat.getColor(activity, R.color.timing_text_selected));
            seekBar.userScrolling(true);
        }

        @Override
        public void onStopTouch(float per10Mille) {
            userActivelyScrolling = false;
            PlayViewPage.this.timingText.setTextColor(ContextCompat.getColor(activity, R.color.timing_text_unselected));
            int location = (int) (per10Mille * PlayViewPage.this.currentEpisodeDuration);
            playController.seekLocation(location);
            seekBar.userScrolling(false);
        }
    }

    private class BackTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                goBack();
            }
            return true;
        }
    }

    private class ForwardTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                goForward();
            }
            return true;
        }
    }

    /*
    private class ToggleDescriptionListener implements View.OnTouchListener {
        private boolean pointingDown = true;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (pointingDown) {
                    toggleDescriptionButton.startAnimation(toggleDescriptionRotationDown);
                } else {
                    toggleDescriptionButton.startAnimation(toggleDescriptionRotationUp);
                }
                pointingDown = !pointingDown;
                toggleDescription();
            }
            return true;
        }
    }
    */
}
