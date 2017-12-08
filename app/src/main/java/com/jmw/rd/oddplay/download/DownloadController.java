package com.jmw.rd.oddplay.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.storage.Episode;
import java.util.ArrayList;

public class DownloadController {

    private static final String DOWNLOAD_INTENT = "com.jmw.rd.oddplayer.DOWNLOADED";

    public static final String DOWNLOAD_STOP = "com.jmw.rd.oddplayer.DOWNLOAD_STOP";
    public static final String DOWNLOAD_STOP_MSG = "com.jmw.rd.oddplayer.DOWNLOAD_STOP_MSG";

    public static final String DOWNLOAD_FULL_SERVICE = "DOWNLOAD_FULL_SERVICE";
    public static final String DOWNLOAD_PROVIDED_EPISODES = "DOWNLOAD_PROVIDED_EPISODES";
    public static final String DOWNLOAD_EPISODES = "DOWNLOAD_EPISODES";

    public static final String INFO_PARSING_FEEDS = "parsingFeeds";
    public static final String INFO_DOWNLOAD_AMOUNT = "downloadAmount";
    public static final String INFO_DOWNLOAD_TOTAL = "downloadTotal";
    public static final String INFO_DOWNLOAD_FINISHED = "downloadFinished";
    public static final String INFO_DOWNLOAD_NAME = "downloadName";
    public static final String INFO_DOWNLOAD_FEED_INFO = "feedName";
    public static final String INFO_NUMBER_IN_QUEUE= "numberInQueue";
    public static final String INFO_TOTAL_IN_QUEUE = "totalInQueue";
    public static final String INFO_DOWNLOAD_FINISHED_MESSAGE = "downloadFinishedMessage";

    private final Context context;

    public DownloadController(Context context) {
        this.context = context;
    }

    void registerForDownloadStop(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(DOWNLOAD_STOP));
    }

    // this section is messages going to download service
    public void downloadSomeEpisodes(ArrayList<Episode> episodesToDownload) throws NoWifiException{
        if (Utils.isDataOn(context)) {
            Intent downloadIntent = new Intent(context, DownloadService.class);
            downloadIntent.setAction(DOWNLOAD_PROVIDED_EPISODES);
            downloadIntent.putParcelableArrayListExtra(DOWNLOAD_EPISODES, episodesToDownload);
            context.startService(downloadIntent);
        } else {
            throw new NoWifiException();
        }
    }

    public void downloadAllEpisodes() throws NoWifiException {
        if (Utils.isDataOn(context)) {
            Intent downloadIntent = new Intent(context, DownloadService.class);
            downloadIntent.setAction(DOWNLOAD_FULL_SERVICE);
            context.startService(downloadIntent);
        } else {
            throw new NoWifiException();
        }
    }

    public void stopDownloading(String message) {
        final Intent i = new Intent(DOWNLOAD_STOP);
        i.putExtra(DOWNLOAD_STOP_MSG, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

    }

    // This section is messages coming back form DownloadService
    public void registerForDownloadEvent(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(DOWNLOAD_INTENT));
    }

    public void unregisterForDownloadEvent(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }


    void broadcastDownloadAmt(String currentAmt, String total, String name,
                                      String feedInfo, String numberInDownloadQueue,
                                      String totalInDownloadQueue) {
        final Intent i = new Intent(DownloadController.DOWNLOAD_INTENT);
        i.putExtra(DownloadController.INFO_PARSING_FEEDS, false);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FINISHED, false);
        i.putExtra(DownloadController.INFO_DOWNLOAD_NAME, name);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FEED_INFO, feedInfo);
        i.putExtra(DownloadController.INFO_DOWNLOAD_AMOUNT, currentAmt);
        i.putExtra(DownloadController.INFO_DOWNLOAD_TOTAL, total);
        i.putExtra(DownloadController.INFO_NUMBER_IN_QUEUE, numberInDownloadQueue);
        i.putExtra(DownloadController.INFO_TOTAL_IN_QUEUE, totalInDownloadQueue);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    void broadcastParsingFeeds(String feedInfo) {
        final Intent i = new Intent(DownloadController.DOWNLOAD_INTENT);
        i.putExtra(DownloadController.INFO_PARSING_FEEDS, true);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FEED_INFO, feedInfo);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    void broadcastException(String exceptionInfo) {
        final Intent i = new Intent(DownloadController.DOWNLOAD_INTENT);
        i.putExtra(DownloadController.INFO_PARSING_FEEDS, true);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FEED_INFO, exceptionInfo);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    void broadcastDownloadEnded(String message) {
        final Intent i = new Intent(DownloadController.DOWNLOAD_INTENT);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FINISHED, true);
        i.putExtra(DownloadController.INFO_DOWNLOAD_FINISHED_MESSAGE, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }



}
