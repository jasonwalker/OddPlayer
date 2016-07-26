package com.jmw.rd.oddplay.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.jmw.rd.oddplay.AmountSentCommunicator;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public abstract class Storage {
    protected final Context context;
    protected final SharedPreferences prefs;
    public final Fast fast;

    protected static final String PREFS_TRACK_NUMBER = "TRACK_NUMBER";
    protected static final String PREFS_USE_ONLY_WIFI = "USE_ONLY_WIFI";
    protected static final String PREFS_DELETE_AFTER_LISTENING = "PREFS_DELETE_AFTER_LISTENING";
    protected static final String PREFS_SKIP_DISTANCE = "SKIP_DISTANCE_MS";
    protected static final String PREFS_MAX_DOWNLOADS_PER_FEED = "MAX_FEED_DOWNLOADS";
    protected static final String SHARED_PREFS_FILE_KEY = "com.jmw.rd.oddplayer.SHARED_PREFS_FILE_KEY";
    protected static final String PREFS_DOWNLOAD_TIME = "DOWNLOAD_TIME";
    protected static final String PREFS_USING_DOWNLOAD_TIME = "USING_DOWNLOAD_TIME";
    protected static final String PREFS_STARTUP_MESSAGE = "STARTUP_MESSAGE";

    protected Storage(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(SHARED_PREFS_FILE_KEY, Context.MODE_PRIVATE);
        this.fast = new Fast();
    }

    /**
     * These methods are fast enough to run on the UI thread
     */
    public final class Fast {

        private Fast() {}
        /**
         * returns number of current episode, starting at 0
         * @return int
         */
        public int getCurrentEpisodeNumber() {
            return prefs.getInt(PREFS_TRACK_NUMBER, 0);
        }

        /**
         * Changes the current episode number
         * @param currentEpisodeNumber number to change to
         */
        public void setCurrentEpisodeNumber(int currentEpisodeNumber) {
            if (currentEpisodeNumber < 0) {
                currentEpisodeNumber = 0;
            }
            prefs.edit().putInt(PREFS_TRACK_NUMBER, currentEpisodeNumber).apply();
        }

        /**
         * Get the number of downloads that this feed will pull down when 'downloading all'
         * @return
         */
        public long getMaxDownloadsPerFeed() {
            return prefs.getLong(PREFS_MAX_DOWNLOADS_PER_FEED, 10L);
        }

        /**
         * Set the number of downloads that this feed will pull down when 'downloading all'
         * @return
         */
        public void setMaxDownloadsPerFeed(long number) {
            prefs.edit().putLong(PREFS_MAX_DOWNLOADS_PER_FEED, number).apply();
        }

        /**
         * Shows if only WIFI can be used to download, not 3G/4G etc
         * @return
         */
        public boolean getUseOnlyWIFI() {
            return prefs.getBoolean(PREFS_USE_ONLY_WIFI, true);
        }

        /**
         * Configure if only WIFI can be used to download, not 3G/4G etc
         * @return
         */
        public void setUseOnlyWIFI(boolean useWifi) {
            prefs.edit().putBoolean(PREFS_USE_ONLY_WIFI, useWifi).apply();
        }

        /**
         * Shows if episodes will be deleted when moving to subsequent episode
         * @return
         */
        public boolean getDeleteAfterListening() {
            return prefs.getBoolean(PREFS_DELETE_AFTER_LISTENING, false);
        }

        /**
         * Configure if episodes will be deleted when moving to subsequent episode
         * @return
         */
        public void setDeleteAfterListening(boolean delete) {
            prefs.edit().putBoolean(PREFS_DELETE_AFTER_LISTENING, delete).apply();
        }

        /**
         * Gets the number in milliseconds that forward and backward skip will jump while playing audio
         * @return
         */
        public int getSkipDistance() {
            return prefs.getInt(PREFS_SKIP_DISTANCE, 15000);
        }

        /**
         * Sets the number in milliseconds that forward and backward skip will jump while playing audio
         * @return
         */
        public void setSkipDistance(int milliseconds) {
            prefs.edit().putInt(PREFS_SKIP_DISTANCE, milliseconds).apply();
        }

        public void setDownloadScheduleTime(int minutesFromMidnight) {
            prefs.edit().putInt(PREFS_DOWNLOAD_TIME, minutesFromMidnight).apply();
        }

        public int getDownloadScheduleTime() {
            return prefs.getInt(PREFS_DOWNLOAD_TIME, -1);
        }

        public void setUsingDownloadScheduleTime(boolean isUsing) {
            prefs.edit().putBoolean(PREFS_USING_DOWNLOAD_TIME, isUsing).apply();
        }

        public boolean getUsingDownloadScheduleTime() {
            return prefs.getBoolean(PREFS_USING_DOWNLOAD_TIME, false);
        }

        public void setStartupMessage(String message) {
            prefs.edit().putString(PREFS_STARTUP_MESSAGE, message).apply();
        }

        public String getStartupMessage() {
            return prefs.getString(PREFS_STARTUP_MESSAGE, "");
        }

    }

    protected String getPrefsAsString() {
        return String.format("currentEpisodeNumber:%d,skipDistance:%d,maxDownloadsPerFeed:%d,useOnlyWifi:%b,deleteAfterListening:%b",
                fast.getCurrentEpisodeNumber(), fast.getSkipDistance(), fast.getMaxDownloadsPerFeed(), fast.getUseOnlyWIFI(), fast.getDeleteAfterListening());
    }

    protected void setPrefsFromString(String vals) {
        String[] prefs = vals.split(",");
        for (String pref : prefs) {
            String[] nameVal = pref.split(":");
            switch (nameVal[0]) {
                case "currentEpisodeNumber":
                    fast.setCurrentEpisodeNumber(Integer.parseInt(nameVal[1])); break;
                case "skipDistance":
                    fast.setSkipDistance(Integer.parseInt(nameVal[1])); break;
                case "maxDownloadsPerFeed":
                    fast.setMaxDownloadsPerFeed(Long.parseLong(nameVal[1])); break;
                case "useOnlyWifi":
                    fast.setUseOnlyWIFI(Boolean.parseBoolean(nameVal[1])); break;
                case "deleteAfterListening":
                    fast.setDeleteAfterListening(Boolean.parseBoolean(nameVal[1])); break;
            }
        }
    }

    /**
     * gets the absolute value of the directory where episodes are stored
     * @return File
     * @throws ResourceAllocationException
     */
    public abstract File getEpisodesDir() throws ResourceAllocationException;


    /**
     * Returns a list of Episode metadata objects
     * @return List of episodes
     */
    public abstract List<Episode> getAllEpisodes();

    /**
     * Put episode and its metadata into database.  It is this method's responsibility to set the
     * Episode's audioFileName, downloadDate and actual contentLength
     * @param episode The episode to download data for
     * @param is stream to read mp3 or ogg data
     * @param listener listener to periodically update amount written
     * @throws ResourceAllocationException
     * @throws IOException
     * @throws EmergencyDownloadStopException
     */
    public abstract void putEpisode(Episode episode, InputStream is, AmountSentCommunicator listener)
            throws ResourceAllocationException, IOException,
            EmergencyDownloadStopException;

    /**
     * Changes where the audio file is currently being read
     * @param episode The episode to change
     * @param location The location in milliseconds within the audio
     * @return Episode with new location
     */
    public abstract Episode updateEpisodeAudioLocation(Episode episode, int location);

    /**
     * Gets the location of the audio file associated with the metadata
     * @param episode the metadata of the audio
     * @return Uri path to audio file
     * @throws ResourceAllocationException
     */
    public abstract Uri getEpisode(Episode episode) throws ResourceAllocationException;

    /**
     * Remove audio and metadata associated with episode
     * @param episode
     * @throws ResourceAllocationException
     */
    public abstract void deleteEpisode(Episode episode) throws ResourceAllocationException;

    /**
     * Remove audio and metadata of every episode before the provided episode number
     * @param episodeNumber
     * @throws ResourceAllocationException
     */
    public abstract void deleteEpisodesBefore(int episodeNumber) throws ResourceAllocationException;

    /**
     * Put a new podcast feed into the database
     * @param feed
     * @throws DuplicateException
     */
    public abstract void putFeed(Feed feed) throws DuplicateException;

    /**
     * Associate an image with the feed
     * @param feed
     * @param is
     * @throws IOException
     * @throws ResourceAllocationException
     * @throws EmergencyDownloadStopException
     */
    public abstract void putFeedImage(Feed feed, InputStream is) throws IOException, ResourceAllocationException,
            EmergencyDownloadStopException;

    /**
     * Get file associated with feed's url
     * @param feedUrl
     * @return
     * @throws ResourceAllocationException
     */
    public abstract File getFeedImageFile(String feedUrl) throws ResourceAllocationException;

    /**
     * Update the most recent date that this feed has had an episode downloaded
     * @param feed
     * @param lastEpisodeDate
     * @return
     */
    public abstract Feed updateFeedLastEpisodeDate(Feed feed, long lastEpisodeDate);

    /**
     * Update the number of seconds to skip the beginning of audio track for this feed
     * @param feed
     * @param skipFirstSeconds
     * @return
     */
    public abstract Feed updateFeedSkipFirstSeconds(Feed feed, int skipFirstSeconds);

    /**
     * Update the number of seconds to skip the end of audio track for this feed
     * @param feed
     * @param skipLastSeconds
     * @return
     */
    public abstract Feed updateFeedSkipLastSeconds(Feed feed, int skipLastSeconds);

    /**
     * Update whether or not episodes from this feed get bumped to the top of the queue immediately
     * after being downloaded
     * @param feed
     * @param prioritize
     * @return
     */
    public abstract Feed updateFeedPriority(Feed feed, boolean prioritize);

    /**
     * Update whether or not episodes from this feed are downloaded when downloading all
     * @param feed
     * @param disabled
     * @return
     */
    public abstract Feed updateFeedDisabled(Feed feed, boolean disabled);

    /**
     * Get the metadata associated with this feed url
     * @param url
     * @return
     */
    public abstract Feed getFeed(String url);

    /**
     * Delete this feed from the database.  Its currently-downloaded episodes will remain in the
     * listen queue
     * @param feed
     */
    public abstract void deleteFeed(Feed feed);

    /**
     * Get a list of all of the feeds
     * @return
     */
    public abstract List<Feed> getFeeds();

    /**
     * Get the total number of episodes in the listen queue
     * @return
     */
    public abstract int getNumberEpisodes();

    /**
     * Get the episode from the supplied ID
     * @param episodeId
     * @return
     */
    public abstract Episode getEpisode(Long episodeId);

    /**
     * Get the episode number from the queue, starting at 0
     * @param episodeNumber
     * @return
     */
    public abstract Episode getEpisode(int episodeNumber);

    /**
     * Move the episode to the position in the listening queue
     * @param srcLocs
     * @param position
     */
    public abstract void shiftEpisodeToPosition(List<Integer> srcLocs, int position);

    /**
     * Return the position of the episode within the listening queue
     * @param episode
     * @return
     */
    public abstract int getEpisodeNumber(Episode episode);

    /**
     * Return the number of episodes remaining in the database associated with this feed
     * @param feedUrl
     * @return
     */
    public abstract int getNumberEpisodesForFeed(String feedUrl);

    /**
     * Get all of the unique identifiers for all episodes in the database
     * @return
     */
    public abstract Long[] getAllIds();

    /**
     * get the int associated with the selected storage.  0 is internal, 1 is first mounted SD, usb, etc.
     * @return
     */
    public abstract int getSelectedStorage();

    /**
     * Set the int associated with the selected storage.  0 is internal, 1 is first mounted SD, usb, etc.
     * @return
     */
    public abstract void setSelectedStorage(int i);

    /**
     * Return a list of storage options for audio on this phone.  The index of the list corresponds to
     * what can be set with setSelectedStorage and displays with getSelectedStorage
     * @param context
     * @return
     */
    public abstract List<String> getStorageOptionsList(Context context);

    /**
     * Move a file within the phone to a different directory typically from internal to external storage or vice-versa
     * @param theFile the file to move
     * @param dstDir the destination directory
     * @param sentListener a listener to update with progress of move
     * @throws IOException
     * @throws EmergencyDownloadStopException
     */
    public abstract void moveFile(File theFile, File dstDir, AmountSentCommunicator sentListener) throws IOException, EmergencyDownloadStopException;

    /**
     * Returns the dir associated with this position, position is from getStorageOptionsList
     * @param i
     * @return
     * @throws ResourceAllocationException
     */
    public abstract File getEpisodesDirForStoragePosition(int i) throws ResourceAllocationException;

    /**
     * return data to backup device
     * @param onlyMetadata if true, only dumps configuration info, not the associated mp3 files.  This is
     *                     in case user wants to copy the large amount of mp3 data another way
     * @return an iterator of content names and inputstreams
     * @throws IOException
     */
    public abstract Iterator<DumpInfo> getDBInputStream(boolean onlyMetadata) throws IOException, ResourceAllocationException;

    /**
     * Reads a file previously dumped withgetDBInputStream and places audio files into currently-selected
     * storage and metadata files into internal storage.  (Metadata placed in internal storage
     * because frequent changes and access could reduce life of SD card)
     * @param name
     * @param in
     * @throws IOException
     * @throws ResourceAllocationException
     * @throws EmergencyDownloadStopException
     */
    public abstract void putBackupData(String name, InputStream in) throws IOException, ResourceAllocationException, EmergencyDownloadStopException;

    public class DumpInfo {
        private final String name;
        private final InputStream inputStream;
        public DumpInfo(String name, InputStream inputStream) {
            this.name = name;
            this.inputStream = inputStream;
        }
        public String getName() {
            return this.name;
        }
        public InputStream getInputStream() {
            return this.inputStream;
        }
    }
}
