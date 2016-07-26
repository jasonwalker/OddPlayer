package com.jmw.rd.oddplay.play;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class PlayController {

    private static final String PLAY_INFO = "com.jmw.rd.oddplayer.action.PLAY_INFO";
    private static final String PLAY_STATE = "com.jmw.rd.oddplayer.action.PLAY_STATE";

    public static final String INIT = "com.jmw.rd.oddplayer.action.INIT";
    public static final String TOGGLE_PLAY = "com.jmw.rd.oddplayer.action.TOGGLE_PLAY";
    public static final String SEEK = "com.jmw.rd.oddplayer.action.SEEK";
    public static final String ACTIVITY_PAUSING = "com.jmw.rd.oddplayer.action.ACTIVITY_PAUSING";
    public static final String GOTO = "com.jmw.rd.oddplayer.action.GOTO";
    public static final String NEXT = "com.jmw.rd.oddplayer.action.NEXT";
    public static final String PREV = "com.jmw.rd.oddplayer.action.PREV";
    public static final String PAUSE = "com.jmw.rd.oddplayer.action.PAUSE";
    public static final String PLAY = "com.jmw.rd.oddplayer.action.PLAY";
    public static final String FF = "com.jmw.rd.oddplayer.action.FF";
    public static final String REWIND = "com.jmw.rd.oddplayer.action.REWIND";

    public static final String JUMP_LOCATION = "jumpLocation";

    public static final String REFRESH = "com.jmw.rd.oddplayer.action.REFRESH";
    public static final String INFO_DURATION = "duration";
    public static final String INFO_EPISODE_NUMBER = "episodeNumber";
    public static final String INFO_EXCEPTION = "exception";
    public static final String INFO_ERROR_MSG = "errorMessage";
    public static final String INFO_TOTAL = "total";
    public static final String INFO_WAS_PLAYING = "wasPlaying";
    public static final String INFO_EXTERNAL_GOTO = "externalGoto";
    public static final String STATE_PLAYING = "playing";
    public static final String STATE_LOCATION = "location";
    public static final String STATE_EPISODE_CHANGE = "episodeChange";

    private final Context context;

    public PlayController(Context context) {
        this.context = context;
    }

    // This section is messages going to PlayService

    private void sendSimple(String action){
        final Intent intent = new Intent(context, PlayService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    public void seekLocation(int location) {
        final Intent intent = new Intent(context, PlayService.class);
        intent.setAction(SEEK);
        intent.putExtra(JUMP_LOCATION, location);
        context.startService(intent);
    }

    public void goToEpisodeNumber(int number) {
        final Intent intent = new Intent(context, PlayService.class);
        intent.setAction(GOTO);
        intent.putExtra(INFO_EPISODE_NUMBER, number);
        context.startService(intent);
    }

    public void promptForPlayInfo() {
        sendSimple(REFRESH);
    }

    public void togglePlay() {
        sendSimple(TOGGLE_PLAY);
    }

    public void guiIsPausing() {
        sendSimple(ACTIVITY_PAUSING);
    }

    public void init() {sendSimple(INIT); }

    public void play() {
        sendSimple(PLAY);
    }

    public void pause() {
        sendSimple(PAUSE);
    }

    public void previous() {
        sendSimple(PREV);
    }

    public void next() {
        sendSimple(NEXT);
    }

    public void rewind() {
        sendSimple(REWIND);
    }

    public void fastforward() {
        sendSimple(FF);
    }

    // this section is messages coming back from the PlayService

    public void registerForPlayState(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(PLAY_STATE));
    }

    public void registerForPlayInfo(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(PLAY_INFO));
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    void broadcastInfo(long duration, int episodeNumber,
                               int numberEpisodes, boolean triggeredByUI) {
        final Intent i = new Intent(PlayController.PLAY_INFO);
        i.putExtra(PlayController.INFO_DURATION, duration);
        i.putExtra(PlayController.INFO_EPISODE_NUMBER, episodeNumber);
        i.putExtra(PlayController.INFO_TOTAL, numberEpisodes);
        i.putExtra(PlayController.INFO_EXTERNAL_GOTO, triggeredByUI);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    void broadcastException(Exception e) {
        final Intent i = new Intent(PlayController.PLAY_INFO);
        i.putExtra(PlayController.INFO_EXCEPTION, true);
        i.putExtra(PlayController.INFO_ERROR_MSG, e.getMessage());
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    void broadcastState(boolean play, long location, boolean episodeChange) {
        final Intent i = new Intent(PlayController.PLAY_STATE);
        i.putExtra(PlayController.STATE_PLAYING, play);
        i.putExtra(PlayController.STATE_LOCATION, location);
        i.putExtra(PlayController.STATE_EPISODE_CHANGE, episodeChange);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }
}
