package com.jmw.rd.oddplay.episode;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.jmw.rd.oddplay.AmountSentCommunicator;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class EpisodeController {

    private static final String EPISODE_LIST_CHANGED = "com.jmw.rd.oddplayer.action.EPISODE_LIST_CHANGED";
    static final String EPISODE_ADDED = "EPISODE_ADDED";
    static final String EPISODE_REMOVE_BEFORE = "EPISODE_REMOVE_BEFORE";
    static final String EPISODE_DELETED = "EPISODE_DELETED";
    static final String EPISODE_MOVED = "EPISODE_MOVED";
    static final String EPISODE_MOVE_SOURCE = "EPISODE_MOVE_SOURCE";
    static final String EPISODE_MOVE_TARGET = "EPISODE_MOVE_TARGET";
    static final String EPISODE_NUMBER = "EPISODE_NUMBER";
    static final String EPISODE_VALUE = "EPISODE_VALUE";

    private final Context context;

    public EpisodeController(Context context) {
        this.context = context;
    }

    public void registerForEpisodeListChange(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(EPISODE_LIST_CHANGED));
    }
    void unregisterForEpisodeListChange(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public void deleteEpisode(Episode episode, int position) throws ResourceAllocationException {
        StorageUtil.getStorage(context).deleteEpisode(episode);
        final Intent i = new Intent(EPISODE_LIST_CHANGED);
        i.putExtra(EPISODE_DELETED, true);
        i.putExtra(EPISODE_NUMBER, position);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    public void deleteEpisodesBefore(int position) throws ResourceAllocationException {
        StorageUtil.getStorage(context).deleteEpisodesBefore(position);
        final Intent i = new Intent(EPISODE_LIST_CHANGED);
        i.putExtra(EPISODE_ADDED, false);
        i.putExtra(EPISODE_REMOVE_BEFORE, true);
        i.putExtra(EPISODE_NUMBER, position);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    public void shiftEpisodes(ArrayList<Integer> srcLocs, int target) {
        StorageUtil.getStorage(context).shiftEpisodeToPosition(srcLocs, target);
        final Intent i = new Intent(EPISODE_LIST_CHANGED);
        i.putExtra(EPISODE_MOVED, true);
        i.putExtra(EPISODE_MOVE_SOURCE, srcLocs);
        i.putExtra(EPISODE_MOVE_TARGET, target);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    public void addEpisode(Episode episode, InputStream is, AmountSentCommunicator communicator)
            throws ResourceAllocationException, IOException, EmergencyDownloadStopException {
        StorageUtil.getStorage(context).putEpisode(episode, is, communicator);
        final Intent i = new Intent(EPISODE_LIST_CHANGED);
        i.putExtra(EPISODE_ADDED, true);
        i.putExtra(EPISODE_VALUE, episode);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }
}
