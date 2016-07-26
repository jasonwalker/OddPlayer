package com.jmw.rd.oddplay.play;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.jmw.rd.oddplay.episode.EpisodeController;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.storage.Feed;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayService extends Service {
    public enum EpisodeState {SAME, NEW, NOTFOUND, ABORTED, EXCEPTION}
    private static final int MIN_AMOUNT_TIME_LEFT = 10000;
    private static final int MIN_DURATION_FOR_JUMP_BACK = 20000;
    private static final int UPDATE_INTERVAL = 200;
    private static final int AUDIO_FOCUS_WAIT_INTERVAL = 200;
    private static final int AUDIO_FOCUS_WAIT_RETRIES = 5;

    private PowerManager.WakeLock wakeLock;
    private Storage storage;
    //private BroadcastReceiver unplugHeadphoneReceiver;
    private ServiceHandler serviceHandler;
    private PlayController playController;
    private EpisodeController episodeController;
    private HandlerThread handlerThread;

    public PlayService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playController = new PlayController(this);
        episodeController = new EpisodeController(this);
        storage = StorageUtil.getStorage(this);
        PowerManager mgr = (PowerManager) this.getSystemService(POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();
        startForeground(1991, new Notification());
        playController.init();
        handlerThread = new HandlerThread("PlayService:WorkerThread");
        handlerThread.setDaemon(true);
        handlerThread.start();
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        try {
            handlerThread.quitSafely();
            serviceHandler.close();
            stopForeground(true);
            wakeLock.release();
        } finally {
            super.onDestroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (serviceHandler != null) {
            Message message = serviceHandler.obtainMessage();
            message.arg1 = startId;
            message.obj = intent;
            serviceHandler.sendMessage(message);
        }
        return START_STICKY;
    }


    private final class ServiceHandler extends Handler implements MediaPlayer.OnCompletionListener{
        private final MediaPlayer mp;
        private ScheduledExecutorService playScheduledExecutorService;
        private final BroadcastReceiver unplugAndThenNoiseReceiver;
        private final AudioFocusListener audioFocusListener;
        private CallStateListener callStateListener;
        private TelephonyManager telephonyManager;
        private Episode currentEpisode;
        private long episodeDuration;
        private int finishEpisodeTime;
        private boolean playing = false;
        private boolean pausedForCall = false;
        private boolean onCompleteFired = false;
        private int ffRewJumpDistance = 15000;

        public ServiceHandler(Looper looper) {
            super(looper);
            mp = new MediaPlayer();
            synchronized (mp) {
                mp.reset();
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setDisplay(null);
                mp.setOnErrorListener(new PlayErrorListener());
                mp.setOnCompletionListener(this);
            }
            audioFocusListener = new AudioFocusListener();
            if (telephonyManager == null) {
                telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                callStateListener = new CallStateListener();
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
            unplugAndThenNoiseReceiver = new NoisyAudioStreamReceiver();
            registerReceiver(unplugAndThenNoiseReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(PlayService.this,
                    AudioHardwareRemoteControlReceiver.class));
            ffRewJumpDistance = storage.fast.getSkipDistance();
        }

        public void close() {
            this.sendTimingInfo(false);
            ((AudioManager) getSystemService(AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(new ComponentName(PlayService.this,
                    AudioHardwareRemoteControlReceiver.class));
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
            unregisterReceiver(unplugAndThenNoiseReceiver);
            synchronized (mp) {
                mp.reset();
                mp.release();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized (mp) {
                // checking for null because see bug only when loading new apk during testing
                onHandleIntent((Intent) msg.obj);
            }
        }

        private int onHandleIntent(Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                switch (action) {
                    case PlayController.INIT:
                        goToEpisode(storage.fast.getCurrentEpisodeNumber(), false, false, false);
                        refresh();
                        break;
                    case PlayController.TOGGLE_PLAY:
                        play(!this.playing);
                        break;
                    case PlayController.GOTO:
                        goToEpisode(intent.getIntExtra(PlayController.INFO_EPISODE_NUMBER, 0),
                                intent.getBooleanExtra(PlayController.INFO_WAS_PLAYING, false), true, true);
                        break;
                    case PlayController.SEEK:
                        seek(intent.getIntExtra(PlayController.JUMP_LOCATION, 0), true);
                        break;
                    case PlayController.REFRESH:
                        refresh();
                        break;
                    case PlayController.ACTIVITY_PAUSING:
                        if (!this.playing && !this.pausedForCall) {
                            PlayService.this.stopSelf();
                        }
                        break;
                    case PlayController.NEXT:
                        goToEpisode(storage.fast.getCurrentEpisodeNumber() + 1, false, false, true);
                        break;
                    case PlayController.PREV:
                        goToEpisode(storage.fast.getCurrentEpisodeNumber() - 1, false, false, true);
                        break;
                    case PlayController.PAUSE:
                        play(false);
                        break;
                    case PlayController.PLAY:
                        play(true);
                        break;
                    case PlayController.FF:
                        int currPos;
                        synchronized (mp) {
                            currPos = mp.getCurrentPosition();
                        }
                        seek(currPos + ffRewJumpDistance, true);
                        break;
                    case PlayController.REWIND:
                        int currPos2;
                        synchronized (mp) {
                            currPos2 = mp.getCurrentPosition();
                        }
                        seek(currPos2 - ffRewJumpDistance, true);
                        break;

                }
            }
            return START_NOT_STICKY;
        }

        private void refresh() {
            if (this.currentEpisode == null) {
                return;
            }
            int currentPosition;
            synchronized (mp) {
                currentPosition = mp.getCurrentPosition();
            }
            playController.broadcastInfo(this.episodeDuration, storage.getEpisodeNumber(this.currentEpisode),
                    storage.getNumberEpisodes(), false);
            playController.broadcastState(this.playing, currentPosition, false);
        }

        private void savePosition() {
            try {
                if (this.currentEpisode != null) {
                    synchronized (mp) {
                        this.currentEpisode = storage.updateEpisodeAudioLocation(this.currentEpisode, mp.getCurrentPosition());
                    }

                }
            } catch (IllegalStateException e) {
                Log.d("SAVE POSITION", e.getMessage(), e);
            }
        }

        private void pauseAndSavePosition() {
            savePosition();
            try {
                synchronized (mp) {
                    mp.pause();
                }
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(audioFocusListener);
            } catch (IllegalStateException e) {
                Log.d("PAUSE", e.getMessage(), e);
            }
        }

        private void play(boolean doPlay) {
            if (this.playing == doPlay) {
                return;
            }
            if (this.currentEpisode == null) {
                playController.broadcastState(false, 0, false);
                return;
            }
            if (doPlay) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int result = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                int count = 0;
                while (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    try {
                        Thread.sleep(AUDIO_FOCUS_WAIT_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    result = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN);
                    if (count++ == AUDIO_FOCUS_WAIT_RETRIES) {
                        Toast.makeText(PlayService.this, getString(R.string.cannotGainAudioFocus), Toast.LENGTH_LONG).show();
                        this.sendTimingInfo(false);
                        this.playing = false;
                        return;
                    }
                }
                synchronized (mp) {
                    mp.start();
                }
            } else {
                pauseAndSavePosition();
            }
            this.playing = doPlay;
            this.sendTimingInfo(this.playing);
        }

        private EpisodeState goToEpisode(int episodeNumber, boolean definitelyPlay, boolean triggeredByUI, boolean broadcast) {
            savePosition();
            Episode episode = storage.getEpisode(episodeNumber);
            if (episode != null) {
                if (!definitelyPlay && this.currentEpisode != null && this.currentEpisode.equals(episode)) {
                    int position;
                    synchronized (mp) {
                        this.episodeDuration = mp.getDuration();
                        position = mp.getCurrentPosition();
                    }
                    if (broadcast) {
                        playController.broadcastInfo(episodeDuration, episodeNumber,
                                storage.getNumberEpisodes(), false);
                        playController.broadcastState(this.playing, position, false);
                    }
                    return EpisodeState.SAME;
                } else {
                    try {
                        this.currentEpisode = episode;
                        storage.fast.setCurrentEpisodeNumber(episodeNumber);
                        int audioLocation = this.currentEpisode.getAudioLocation();
                        // feed is null if it has been deleted but episodes still remain
                        Feed feed = storage.getFeed(this.currentEpisode.getFeed());
                        if (feed != null && audioLocation <= 0) {
                            audioLocation = feed.getSkipFirstSeconds() * 1000;
                        }
                        if (definitelyPlay) {
                            this.playing = true;
                        }
                        synchronized (mp) {
                            mp.reset();
                            mp.setDataSource(PlayService.this, storage.getEpisode(this.currentEpisode));
                            mp.prepare();
                            this.episodeDuration = mp.getDuration();
                            // finishEpisodeTime -1 means play to end--android getDuration not necessarily precise
                            if (feed == null) {
                                this.finishEpisodeTime = -1;
                            } else {
                                if (feed.getSkipLastSeconds() == 0) {
                                    this.finishEpisodeTime = -1;
                                } else {
                                    this.finishEpisodeTime = (int) this.episodeDuration - (feed.getSkipLastSeconds() * 1000);
                                }
                            }
                            seek(audioLocation, false);
                            if (this.playing) {
                                mp.start();
                            }
                        }
                        if (broadcast) {
                            playController.broadcastInfo(episodeDuration, episodeNumber, storage.getNumberEpisodes(),
                                    triggeredByUI);
                            playController.broadcastState(this.playing, audioLocation, true);
                        }
                        return EpisodeState.NEW;
                    } catch (IOException | ResourceAllocationException e) {
                        Log.d("GoToEpisode", e.getMessage(), e);
                        playController.broadcastException(e);
                        return EpisodeState.EXCEPTION;
                    }
                }
            } else {
                playController.broadcastInfo(0, episodeNumber, storage.getNumberEpisodes(), triggeredByUI);
                playController.broadcastState(false, 0, false);
                return EpisodeState.NOTFOUND;
            }
        }

        private void seek(int position, boolean broadcastState) {
            if (this.currentEpisode == null) {
                return;
            }
            if (position < 0) {
                position = 0;
            } else if (position >= this.episodeDuration - MIN_AMOUNT_TIME_LEFT && (this.episodeDuration > MIN_DURATION_FOR_JUMP_BACK)) {
                position = (int) this.episodeDuration - MIN_AMOUNT_TIME_LEFT;
            }
            synchronized (mp) {
                mp.seekTo(position);
            }
            this.currentEpisode = storage.updateEpisodeAudioLocation(this.currentEpisode, position);
            if (broadcastState) {
                playController.broadcastState(this.playing, position, false);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            this.onCompleteFired = true;
        }

        private void sendTimingInfo(boolean start) {
            if (start) {
                playScheduledExecutorService = Executors.newScheduledThreadPool(1);
                playScheduledExecutorService.scheduleWithFixedDelay(
                        new Runnable() {
                            @Override
                            public void run() {
                                int currentPosition;
                                synchronized (mp) {
                                    currentPosition = mp.getCurrentPosition();
                                }
                                // finishEpisodeTime -1 means play to end--android getDuration not necessarily precise
                                if (onCompleteFired || ((finishEpisodeTime > -1) && currentPosition >= finishEpisodeTime)) {
                                    mp.setOnCompletionListener(null);
                                    onCompleteFired = false;
                                    final Episode episodeToDelete = currentEpisode;
                                    final int episodeToDeleteNumber = storage.getEpisodeNumber(episodeToDelete);
                                    final int nextEpisodeNumber = episodeToDeleteNumber + 1;
                                    goToEpisode(nextEpisodeNumber, false, false, true);
                                    if (storage.fast.getDeleteAfterListening()) {
                                        // running as thread because on some phones appears to have catastrophic error
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    episodeController.deleteEpisode(episodeToDelete, episodeToDeleteNumber);
                                                } catch (ResourceAllocationException e) {
                                                    playController.broadcastException(e);
                                                }
                                            }
                                        });
                                    }
                                    mp.setOnCompletionListener(ServiceHandler.this);
                                }
                                playController.broadcastState(playing, currentPosition, false);
                            }
                        }, 0, UPDATE_INTERVAL, TimeUnit.MILLISECONDS
                );
            } else {
                if (playScheduledExecutorService != null) {
                    playScheduledExecutorService.shutdownNow();
                    try {
                        playScheduledExecutorService.awaitTermination(UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    playScheduledExecutorService = null;
                }
                int currentPosition;
                synchronized (mp) {
                    currentPosition = mp.getCurrentPosition();
                }
                playController.broadcastState(false, currentPosition, false);
            }
        }



        private class PlayErrorListener implements MediaPlayer.OnErrorListener {

            public boolean onError(MediaPlayer mp, int what, int extra) {
                playController.broadcastException(new Exception(String.format(getString(R.string.mediaPlayerFailed),
                        what, extra)));
                synchronized (ServiceHandler.this.mp) {
                    mp.reset();
                }
                return true;
            }
        }

        private class CallStateListener extends PhoneStateListener {
            private boolean callWasMade = false;
            private boolean wasPlaying = false;

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (callWasMade) {
                            callWasMade = false;
                            if (wasPlaying) {
                                pausedForCall = false;
                                play(true);
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (!callWasMade) {
                            callWasMade = true;
                            wasPlaying = playing;
                            if (wasPlaying) {
                                pausedForCall = true;
                                play(false);
                            }
                        }
                        break;
                }
            }
        }

        private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {

            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        synchronized (mp) {
                            mp.pause();
                            seek(mp.getCurrentPosition() - 1000, false);
                        }
                        sendTimingInfo(false);
                        savePosition();
                        playing = false;
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        synchronized (mp) {
                            mp.start();
                        }
                        playing = true;
                        sendTimingInfo(true);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // below unregisters this as a listener so stops without manual intervention
                       play(false);
                }

            }
        }

        private class NoisyAudioStreamReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    if (playing) {
                        int loc = currentEpisode.getAudioLocation();
                        play(false);
                        PlayService.this.stopSelf();
                        playController.broadcastState(false, loc, false);
                    }
                }
            }
        }
    }

}
