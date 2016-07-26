package com.jmw.rd.oddplay.download;

import android.app.IntentService;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jmw.rd.oddplay.AmountSentCommunicator;
import com.jmw.rd.oddplay.HttpConnection;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.episode.EpisodeController;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.storage.Feed;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.storage.XMLGrabber;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DownloadService extends IntentService {
    private static final DecimalFormat numFormatter = new DecimalFormat("###,##0.00");
    private EmergencyStopReceiver emergencyStopReceiver;
    private DownloadController downloadController;
    private EpisodeController episodeController;
    private Storage storage;
    private EmergencyDownloadStopException emergencyStopException = null;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadController = new DownloadController(this);
        episodeController = new EpisodeController(this);
        emergencyStopReceiver = new EmergencyStopReceiver();
        storage = StorageUtil.getStorage(this);
        downloadController.registerForDownloadStop(this.emergencyStopReceiver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        downloadController.unregisterForDownloadEvent(this.emergencyStopReceiver);
    }

    private class EmergencyStopReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(DownloadController.DOWNLOAD_STOP_MSG);
            emergencyStopException = new EmergencyDownloadStopException(msg);
        }
    }

    private void checkForEmergencyStop() throws EmergencyDownloadStopException {
        if (emergencyStopException != null) {
            throw emergencyStopException;
        }
    }

    private class EpisodeAccumulator implements XMLGrabber.CallEpisodeFunction {

        private final List<Episode> episodes;

        public EpisodeAccumulator(){
            episodes = new ArrayList<>();
        }

        public void call(Episode episode) {
            downloadController.broadcastParsingFeeds(String.format(getString(R.string.parsedEpisode), episode.getTitle()));
            episodes.add(episode);
        }

        public EmergencyDownloadStopException doIStop() {
            return emergencyStopException;
        }

        public List<Episode> getList() {
            return this.episodes;
        }
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case DownloadController.DOWNLOAD_FULL_SERVICE:
                    emergencyStopException = null;
                    downloadAll();
                    break;
                case DownloadController.DOWNLOAD_PROVIDED_EPISODES:
                    emergencyStopException = null;
                    List<Episode> episodes = intent.getParcelableArrayListExtra(DownloadController.DOWNLOAD_EPISODES);
                    downloadSome(episodes);
                    break;
            }
        }
    }

    private void downloadAll() {
        ConnectivityChangeReceiver connectivityChange = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChange, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        int downloadsAttempted = 0;
        int successfulDownloads = 0;
        startForeground(1992, new Notification());
        PowerManager mgr = (PowerManager) DownloadService.this.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();
        try {
            List<Feed> feeds = DownloadService.this.storage.getFeeds();
            List<EpisodeWithFeed> nonPriorityEpisodes = new ArrayList<>();
            List<EpisodeWithFeed> episodesToPrioritize = new ArrayList<>();
            XMLGrabber grabber = new XMLGrabber();
            int totalCount = 0;
            for (Feed feed : feeds) {
                if (feed.isDisabled()) {
                    continue;
                }
                downloadController.broadcastParsingFeeds(String.format(getString(R.string.parsingFeed), feed.getTitle()));
                try {
                    EpisodeAccumulator accumulator = new EpisodeAccumulator();
                    grabber.passEpisodesIntoFunction(feed.getUrl(), feed.getTitle(),
                            feed.getLastEpisodeDate(), DownloadService.this.storage.fast.getMaxDownloadsPerFeed(), accumulator);
                    checkForEmergencyStop();
                    for (Episode episode : accumulator.getList()) {
                        downloadController.broadcastParsingFeeds(String.format(getString(R.string.parsingFeed), feed.getTitle()));
                        if (feed.isPrioritized()) {
                            episodesToPrioritize.add(new EpisodeWithFeed(episode, feed));
                        } else {
                            nonPriorityEpisodes.add(new EpisodeWithFeed(episode, feed));
                        }
                        totalCount++;
                    }
                }catch (EmergencyDownloadStopException e) {
                    throw e;
                } catch (IOException e) {
                    downloadController.broadcastParsingFeeds(String.format(getString(R.string.couldNotConnectToFeed), feed.getTitle()));
                } catch (Exception e) {
                    downloadController.broadcastParsingFeeds(String.format(getString(R.string.feedMalformedXML), feed.getTitle()));
                }
            }
            downloadsAttempted = nonPriorityEpisodes.size() + episodesToPrioritize.size();
            checkForEmergencyStop();
            // first download priority episodes, sort them from most recent to oldest. when bumped to
            // first position, most recent will get bumped down the most so still on chronological order
            Collections.sort(episodesToPrioritize, Collections.reverseOrder());
            int counter = 0;
            for (EpisodeWithFeed episode : episodesToPrioritize) {
                if (downloadEpisode(episode, true, counter++, totalCount)) {
                    successfulDownloads++;
                }
            }
            // download all episodes in order of publish date then play them by download
            // date.  This will normalize order so that no episode shows up before a
            // currently-playing episode
            Collections.sort(nonPriorityEpisodes);
            for (EpisodeWithFeed episode : nonPriorityEpisodes) {
                if (downloadEpisode(episode, false, counter++, totalCount)) {
                    successfulDownloads++;
                }
            }
            downloadController.broadcastDownloadEnded(String.format(DownloadService.this.getString(R.string.finishedDownloadind),
                    successfulDownloads, downloadsAttempted));
        } catch (EmergencyDownloadStopException e) {
            downloadController.broadcastDownloadEnded(String.format(DownloadService.this.getString(R.string.downloadInterrupted),
                    e.getMessage(), successfulDownloads, downloadsAttempted));
        } catch(Exception e) {
            downloadController.broadcastException("Exception during download: " + e.getMessage());
        } finally {
            wakeLock.release();
            stopForeground(true);
            DownloadService.this.stopSelf();
            unregisterReceiver(connectivityChange);
        }
    }

    public class ConnectivityChangeReceiver
            extends BroadcastReceiver {

        ConnectivityManager connectivityManager;
        public ConnectivityChangeReceiver() {
            super();
        }

        /* shut down if switch from WIFI to other network if mobile data not allowed */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Utils.isDataOn(context)) {
                emergencyStopException = new EmergencyDownloadStopException(getString(R.string.switchedOffWifiStoppingDownload));
            }
        }
    }

    private void downloadSome(List<Episode> episodes) {
        ConnectivityChangeReceiver connectivityChange = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChange, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        int downloadsAttempted = 0;
        int downloadsSucceeded = 0;
        startForeground(1993, new Notification());
        PowerManager mgr = (PowerManager) DownloadService.this.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();
        try {
            downloadsAttempted = episodes.size();
            int count = 0;
            for (Episode episode : episodes) {
                try {
                    grabEpisode(episode, count++, downloadsAttempted);
                    downloadsSucceeded++;
                } catch (IOException e) {
                    downloadController.broadcastParsingFeeds(String.format(getString(R.string.failedToDownloadEpisode),
                            episode.getFeedTitle(), episode.getTitle()));
                }
            }
        } catch (EmergencyDownloadStopException e) {
            downloadController.broadcastDownloadEnded(String.format(DownloadService.this.getString(R.string.downloadInterrupted),
                    e.getMessage(), downloadsSucceeded, downloadsAttempted));
        } catch(Exception e) {
            downloadController.broadcastException("Exception during download: " + e.getMessage());
        } finally {
            downloadController.broadcastDownloadEnded(String.format(DownloadService.this.getString(R.string.finishedDownloadind),
                    downloadsSucceeded, downloadsAttempted));
            wakeLock.release();
            stopForeground(true);
            DownloadService.this.stopSelf();
            unregisterReceiver(connectivityChange);
        }
    }

    //returns number of succeeded downloads
    private boolean downloadEpisode(EpisodeWithFeed episodeFeed, boolean prioritize, int counter, final int total) throws
            EmergencyDownloadStopException{
        try {
            grabEpisode(episodeFeed.getEpisode(), counter, total);
            if (episodeFeed.getEpisode().getPublishDate() > episodeFeed.getFeed().getLastEpisodeDate()) {
                this.storage.updateFeedLastEpisodeDate(episodeFeed.getFeed(), episodeFeed.getEpisode().getPublishDate());
            }
            if (prioritize) {
                ArrayList<Integer> toShift = new ArrayList<>();
                toShift.add(storage.getNumberEpisodes() - 1);
                int targetPosition = storage.fast.getCurrentEpisodeNumber() + 1;
                episodeController.shiftEpisodes(toShift, targetPosition);
            }
            return true;
        } catch (IOException e) {
            downloadController.broadcastParsingFeeds(String.format(getString(R.string.failedToDownloadEpisode),
                    episodeFeed.getFeed().getTitle(), episodeFeed.episode.getTitle()));
            Log.e("Failed Download", Log.getStackTraceString(e));
            return false;
        }
    }

    private void grabEpisode(Episode episode, final int currentNumber, final int total) throws IOException,
        EmergencyDownloadStopException{
        try {
            downloadController.broadcastParsingFeeds(String.format(getString(R.string.attemptingEpisodeDownload), episode.getFeed(),
                    episode.getTitle(), currentNumber, total));
            HttpURLConnection connection = (HttpURLConnection) new URL(episode.getAudioUrl()).openConnection();
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    connection.disconnect();
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                }
            }
            try (InputStream is = connection.getInputStream()) {
                int reportedContentLength = connection.getContentLength() > 0 ? connection.getContentLength() : -1;
                episodeController.addEpisode(episode, is,
                        new DownloadCommunicator(episode, reportedContentLength, currentNumber+1, total));
            } finally {
                connection.disconnect();
            }
        } catch(ResourceAllocationException e) {
            downloadController.broadcastException(e.getMessage());
        }
    }

    // TODO: if want different sorts, implement a comparator instead of implementing Comparable
    private class EpisodeWithFeed implements Comparable<EpisodeWithFeed> {
        private final Episode episode;
        private final Feed feed;

        public EpisodeWithFeed(Episode episode, Feed feed) {
            this.episode = episode;
            this.feed = feed;
        }

        public Feed getFeed() {
            return feed;
        }

        public Episode getEpisode() {
            return episode;
        }

        public int compareTo(@NonNull EpisodeWithFeed other) {
            final long lhs = this.episode.getPublishDate();
            final long rhs = other.episode.getPublishDate();
            return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
        }
    }

    private class DownloadCommunicator implements AmountSentCommunicator {
        public static final float contentDivisor = 1000000;
        private final Episode episode;
        private final String contentLengthString;
        private final String numberInDownloadQueue;
        private final String totalInDownloadQueue;

        public DownloadCommunicator(Episode episode, int contentLength, int numberInDownloadQueue, int totalInDownloadQueue) {
            this.episode = episode;
            this.contentLengthString = numFormatter.format((contentLength > 0 ? contentLength : episode.getAudioSize()) / contentDivisor);
            this.numberInDownloadQueue = Integer.toString(numberInDownloadQueue);
            this.totalInDownloadQueue = Integer.toString(totalInDownloadQueue);
        }

        @Override
        public void newDownload() {
        }
        @Override
        public EmergencyDownloadStopException stopNow() {
            return emergencyStopException;
        }

        @Override
        public void totalToOutputStream(long amt) {
            downloadController.broadcastDownloadAmt(numFormatter.format((int) amt / contentDivisor), this.contentLengthString,
                    episode.getTitle(), episode.getFeedTitle(), numberInDownloadQueue, totalInDownloadQueue);
        }
    }
}
