package com.jmw.rd.oddplay.feed;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

class FeedController {
    public static final String ADD_URL = "com.jmw.oddplayer.feed.ADD_URL";
    public static final String URL_ADDRESS = "urlAddress";
    public static final String FEED_ADDED = "com.jmw.oddplayer.feed.FEED_ADDED";
    public static final String ADD_WAS_SUCCESSFUL = "com.jmw.oddplayer.feed.SUCCESSFUL_ADD";

    private final Context context;

    public FeedController(Context context) {
        this.context = context;
    }

    // these are messages coming back from the Feed-adding thread

    public void registerAddFeedListener(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(ADD_URL));
    }

    public void unregisterUrlAddedListener(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public void broadcastAddFeed(String url) {
        final Intent i = new Intent(ADD_URL);
        i.putExtra(URL_ADDRESS, url);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    public void registerFeedAddedListener(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(FEED_ADDED));
    }

    public void unregisterFeedAddedListener(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public void broadcastFeedAdded(String url, boolean success) {
        final Intent i = new Intent(FEED_ADDED);
        i.putExtra(URL_ADDRESS, url);
        i.putExtra(ADD_WAS_SUCCESSFUL, success);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }


}
