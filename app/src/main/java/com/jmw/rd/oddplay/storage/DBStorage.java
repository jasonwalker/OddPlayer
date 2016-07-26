package com.jmw.rd.oddplay.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.jmw.rd.oddplay.AmountSentCommunicator;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class DBStorage extends Storage {
    private static final String ODDPLAY_DB = "episodes.db";
    private static final String EPISODE_TABLE = "episodes";
    private static final String FEED_TABLE = "feeds";
    private static final String FEED = "feed";
    private static final String FEED_TITLE = "feedTitle";
    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String PUBLISH_DATE = "publishDate";
    private static final String DOWNLOAD_DATE = "downloadDate";
    private static final String DESCRIPTION = "description";
    private static final String AUDIO_URL = "audioUrl";
    private static final String AUDIO_SIZE = "audioSize";
    private static final String AUDIO_LOCATION = "audioLocation";
    private static final String AUDIO_TYPE = "audioType";
    private static final String FILENAME = "filename";
    private static final String LAST_EPISODE_DATE = "lastEpisodeDate";
    private static final String COPYRIGHT = "copyright";
    private static final String IMAGE_URL = "imageUrl";
    private static final String LANGUAGE = "language";
    private static final String ID = "id";
    private static final String AUDIO_DURATION = "duration";

    private static final String SKIP_FIRST = "skipFirst";
    private static final String SKIP_LAST = "skipLast";
    private static final String PRIORITIZE = "prioritize";
    private static final String DISABLED = "disabled";

    private static final String PREFS_SELECTED_STORAGE = "SELECTED_STORAGE";


    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final int VERSION = 1;
    private static File episodesDir = null;
    private int numberEpisodes = -1;
    private final SQLiteDatabase sql;
    private static DBStorage current;

    private DBStorage(Context context) {
        super(context);
        EpisodeDB db = new EpisodeDB(this.context);
        sql = db.getWritableDatabase();
    }

    public static synchronized DBStorage getDBStorage(Context context) {
        if (current == null) {
            current = new DBStorage(context);
        }
        return current;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static Set<String> makeSet(String... vals) {
        return new HashSet<>(Arrays.asList(vals));
    }

    private static String commaJoinWithType(String... vals) {
        Set integers = makeSet(PUBLISH_DATE, AUDIO_SIZE, LAST_EPISODE_DATE, DOWNLOAD_DATE,
                AUDIO_LOCATION, SKIP_FIRST, SKIP_LAST, PRIORITIZE, DISABLED, AUDIO_DURATION);
        int stopNum = vals.length - 1;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            buf.append(vals[i]);
            if (integers.contains(vals[i])) {
                buf.append(" INTEGER");
            } else {
                buf.append(" TEXT");
            }
            if (i < stopNum) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        System.out.println(commaJoinWithType(FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION,
                AUDIO_URL, AUDIO_SIZE, AUDIO_TYPE, FILENAME, AUDIO_LOCATION,
                DOWNLOAD_DATE) + "," +
                "PRIMARY KEY (" + FEED + ", " + TITLE + ", " + PUBLISH_DATE + "));");

    }

    private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public int getSelectedStorage(){
        int val =  this.prefs.getInt(PREFS_SELECTED_STORAGE, -1);
        if (val < 0) {
            if (isExternalStorageWritable()) {
                List<File> files = getFilesDirs();
                if (files.size() > 1) {
                    return 1;
                }
            }
            return 0;
        }
        return val;
    }

    public void setSelectedStorage(int i) {
        synchronized (PREFS_SELECTED_STORAGE) {
            episodesDir = null;
            prefs.edit().putInt(PREFS_SELECTED_STORAGE, i).apply();
        }
    }

    private String getStorageString(File file){
        String[] pathSplit = file.getAbsolutePath().split("[/]");
        if (pathSplit.length > 2){
            return pathSplit[0] + "/" + pathSplit[1] + "/" + pathSplit[2];
        } else if (pathSplit.length > 1) {
            return pathSplit[0] + "/" + pathSplit[1];
        } else {
            return pathSplit[0];
        }
    }

    @Override
    public List<String> getStorageOptionsList(Context context){
        List<File> files = getFilesDirs();
        List<String> retVal = new ArrayList<>();
        retVal.add(getStorageString(files.get(0)) + " (internal)");
        for (int i = 1 ; i < files.size() ; i++){
            retVal.add(getStorageString(files.get(i)) + " (external)");
        }
        return retVal;
    }

    @Override
    public File getEpisodesDir() throws ResourceAllocationException {
        synchronized (PREFS_SELECTED_STORAGE) {
            if (null == episodesDir) {
                episodesDir = getEpisodesDirForStoragePosition(getSelectedStorage());
            }
            return episodesDir;
        }
    }

    private List<File> getFilesDirs() {
        List<File> retFiles = new ArrayList<>();
        File[] files = this.context.getExternalFilesDirs(null);
        if (files == null) {
            retFiles.add(secondChanceGetFile());
        } else {
            for (File file : files){
                if (file != null) {
                    retFiles.add(file);
                }
            }
            if (retFiles.size() == 0) {
                retFiles.add(secondChanceGetFile());
            }
        }
        return retFiles;
    }

    private File secondChanceGetFile() {
        File extFile = this.context.getExternalFilesDir(null);
        if (extFile == null) {
            extFile = this.context.getFilesDir();
        }
        return extFile;
    }

    @Override
    public File getEpisodesDirForStoragePosition(int i) throws ResourceAllocationException {
        List<File> files = getFilesDirs();
        if (i >= files.size()) {
            i = files.size() - 1;
        }
        File dir = new File(files.get(i), "episodes");
        if (!dir.exists()) {
            if (!dir.mkdir()){
                throw new ResourceAllocationException(context.getString(R.string.couldNotCreateDirectory));
            }
        }
        return dir;
    }

    @Override
    public void moveFile(File theFile, File dstDir, AmountSentCommunicator sentListener) throws IOException,
        EmergencyDownloadStopException {
        String name = theFile.getName();
        File output = new File(dstDir, name);
        if (output.exists()) {
            if (!output.delete()) {
                throw new IOException("Could not delete file");
            }
        }
        if (!output.createNewFile()) {
            throw new IOException("Could not create file");
        }
        try (FileOutputStream fos = new FileOutputStream(output);
             FileInputStream fis = new FileInputStream(theFile)){
            Utils.IsToOs(fis, fos, sentListener);
        }
        if (!theFile.delete()) {
            throw new IOException("Could not delete file");
        }
    }

    private long getMediaDuration(File file) {
        /*
        MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(file));
        mp.reset();
        long duration = mp.getDuration();
        mp.release();
        return duration;
        */

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(file.getAbsolutePath());
        String metaDuration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try {
            return Long.parseLong(metaDuration);
        } catch(NumberFormatException | NullPointerException e) {
            return 0L;
        }
    }


    @Override
    public void putEpisode(Episode episode, InputStream is, AmountSentCommunicator listener)
            throws ResourceAllocationException, IOException,
            EmergencyDownloadStopException {
        String filename = String.format("pod-%s.mp3", Long.toString(System.currentTimeMillis()));
        long contentLength = Utils.saveStreamToFile(getEpisodesDir(), filename, is, listener);
        long downloadDate = Calendar.getInstance().getTimeInMillis();
        episode.setAudioFileName(filename);
        episode.setDownloadDate(downloadDate);
        episode.setAudioSize(contentLength);
        long duration = getMediaDuration(new File(getEpisodesDir(), filename));
        episode.setAudioDuration(duration);
        ContentValues cv = new ContentValues();
        cv.put(FEED, episode.getFeed());
        cv.put(FEED_TITLE, episode.getFeedTitle());
        cv.put(TITLE, episode.getTitle());
        cv.put(PUBLISH_DATE, episode.getPublishDate());
        cv.put(DESCRIPTION, episode.getDescription());
        cv.put(AUDIO_URL, episode.getAudioUrl());
        cv.put(AUDIO_TYPE, episode.getAudioType());
        cv.put(AUDIO_LOCATION, 0);
        cv.put(AUDIO_SIZE, episode.getAudioSize());
        cv.put(FILENAME, filename);
        cv.put(DOWNLOAD_DATE, downloadDate);
        cv.put(AUDIO_DURATION, duration);
        sql.insert(EPISODE_TABLE, TITLE, cv);
        numberEpisodes = -1;
    }

    @Override
    public int getNumberEpisodes() {
        if (numberEpisodes == -1) {
            String query = String.format("SELECT COUNT(*) AS total FROM %s", EPISODE_TABLE);
            try (Cursor result = sql.rawQuery(query, null)) {
                if (result.moveToFirst()) {
                    numberEpisodes = result.getInt(0);
                } else {
                    numberEpisodes = 0;
                }
            }
        }
        return numberEpisodes;
    }

    private String escape(String input) {
        return input.replaceAll("'", "''");
    }
    
    @Override
    public int getEpisodeNumber(Episode episode) {

        if (episode == null) {
            return -1;
        }
        /*
            Using query below instead of "SELECT COUNT(*) AS total FROM EPISODE_TABLE WHERE DOWNLOAD_DATE <= 'episodeDownloadDate'",
            because with episode reordering the episode download date may be stale.
        */
        String query = String.format("SELECT COUNT(*) AS total FROM %s WHERE %s <= " +
                        "(SELECT %s FROM %s WHERE %s = '%s' AND %s= '%s' AND %s = '%s');",
                EPISODE_TABLE, DOWNLOAD_DATE, DOWNLOAD_DATE, EPISODE_TABLE, FEED, escape(episode.getFeed()), TITLE,
                escape(episode.getTitle()), PUBLISH_DATE, episode.getPublishDate());
        try (Cursor result = sql.rawQuery(query, null)) {
            int number = -1;
            if (result.moveToFirst()) {
                number = result.getInt(0) - 1;
            }
            return number;
        }
    }

    @Override
    public void shiftEpisodeToPosition(List<Integer> srcLocs, int desiredPosition) {
        int currentEpisodeNumber = this.fast.getCurrentEpisodeNumber();
        int totalEpisodes = this.getNumberEpisodes();
        int adjuster = 0;
        for (int currentPositionToMove : srcLocs) {
            int adjustedCurrentPosition = currentPositionToMove;
            if (currentEpisodeNumber < currentPositionToMove) {
                adjustedCurrentPosition += adjuster++;
            }
            if (adjustedCurrentPosition == desiredPosition) {
                continue;
            }
            // handle case where desired position is at end of list
            if (desiredPosition >= totalEpisodes) {
                long desiredPositionDownloadDate = getDownloadDateForPosition(totalEpisodes - 1) + 100;
                String id = getIdForPosition(adjustedCurrentPosition);
                updateDownloadDate(desiredPositionDownloadDate, id);
            } else {
                shiftEpisodeHelper(adjustedCurrentPosition, desiredPosition);
            }
            if (adjustedCurrentPosition < currentEpisodeNumber && desiredPosition > currentEpisodeNumber) {
                this.fast.setCurrentEpisodeNumber(--currentEpisodeNumber);
            }
        }
    }

    private void shiftEpisodeHelper(int startPosition, int endPosition) {
        //find one millisecond before desired position
        long desiredPositionDownloadDate = getDownloadDateForPosition(endPosition) - 1;

        String id = getIdForPosition(startPosition);
        // download date before desired position
        long previousPositionDownloadDate;
        if (endPosition == 0) {
            previousPositionDownloadDate = 0;
        } else {
            previousPositionDownloadDate = getDownloadDateForPosition(endPosition - 1);
        }
        // if there is an episode sitting where you want to be, move that blocking episode backwards
        if (desiredPositionDownloadDate == previousPositionDownloadDate) {
            shiftEpisodeHelper(endPosition - 1, endPosition - 1);
        }
        updateDownloadDate(desiredPositionDownloadDate, id);
    }

    private void updateDownloadDate(long date, String id) {
        ContentValues cv = new ContentValues();
        cv.put(DOWNLOAD_DATE, date);
        sql.update(EPISODE_TABLE, cv, String.format("%s=?", ID), new String[]{id});
    }

    private String getIdForPosition(int position) {
        try (Cursor result = sql.query(EPISODE_TABLE, new String[] {ID}, null, null, null, null, DOWNLOAD_DATE, position + ",1")){
            result.moveToFirst();
            return Integer.toString(result.getInt(0));
        }
    }

    private long getDownloadDateForPosition(int position) {
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{DOWNLOAD_DATE}, null, null, null, null, DOWNLOAD_DATE, position + ",1")){
            result.moveToFirst();
            return result.getLong(0);
        }
    }

    public Episode getEpisode(Long episodeId) {
        Episode episode = null;
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION, AUDIO_URL, AUDIO_SIZE,
                AUDIO_TYPE, FILENAME, DOWNLOAD_DATE, AUDIO_LOCATION, AUDIO_DURATION}, String.format("%s = ?", ID), new String[] {episodeId.toString()}, null, null, null, null)){
            if (result.moveToNext()) {
                episode = makeEpisodeFromCursor(result);
            }
        }
        return episode;
    }

    @Override
    public Episode getEpisode(int episodeNumber) {
        if (episodeNumber >= 0) {
            Episode episode = null;
            try (Cursor result = sql.query(EPISODE_TABLE, new String[]{FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION, AUDIO_URL, AUDIO_SIZE,
                    AUDIO_TYPE, FILENAME, DOWNLOAD_DATE, AUDIO_LOCATION, AUDIO_DURATION}, null, null, null, null, DOWNLOAD_DATE, episodeNumber + ",1")){
                if (result.moveToNext()) {
                    episode = makeEpisodeFromCursor(result);
                }
            }
            return episode;
        } else {
            return null;
        }
    }

    public Long[] getAllIds() {
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{ID}, null, null, null, null, DOWNLOAD_DATE)){
            List<Long> retVal = new ArrayList<>();
            while (result.moveToNext()){
                retVal.add(result.getLong(0));
            }
            return retVal.toArray(new Long[retVal.size()]);
        } catch (SQLiteException e) {
            return null;
        }
    }


    @Override
    public List<Episode> getAllEpisodes() {
        List<Episode> episodes = new ArrayList<>();
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION, AUDIO_URL, AUDIO_SIZE,
                AUDIO_TYPE, FILENAME, DOWNLOAD_DATE, AUDIO_LOCATION, AUDIO_DURATION}, null, null, null, null, DOWNLOAD_DATE)){
            while (result.moveToNext()) {
                Episode episode = makeEpisodeFromCursor(result);
                episodes.add(episode);
            }
        }
        return episodes;
    }

    @Override
    public Episode updateEpisodeAudioLocation(Episode episode, int location) {
        ContentValues cv = new ContentValues();
        cv.put(AUDIO_LOCATION, location);
        episode.setAudioLocation(location);
        sql.update(EPISODE_TABLE, cv, String.format("%s=? AND %s=? AND %s=?", FEED, TITLE, PUBLISH_DATE),
                new String[]{episode.getFeed(), episode.getTitle(), Long.toString(episode.getPublishDate())}
        );
        return episode;
    }

    @Override
    public Uri getEpisode(Episode episode) throws ResourceAllocationException {
        File episodeFile = new File(getEpisodesDir(), episode.getAudioFileName());
        return Uri.fromFile(episodeFile);
    }


    @Override
    public void deleteEpisode(Episode episode) throws ResourceAllocationException {
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{ID, DOWNLOAD_DATE}, String.format("%s=? AND %s=? and %s=?", FEED, PUBLISH_DATE, TITLE),
                new String[] {episode.getFeed(), Long.toString(episode.getPublishDate()), episode.getTitle()}, null, null, null)){
            if (result.moveToFirst()) {
                int id = result.getInt(0);
                long date = result.getLong(1);
                Episode currentEpisode = getEpisode(this.fast.getCurrentEpisodeNumber());
                if (date <= currentEpisode.getDownloadDate()) {
                    fast.setCurrentEpisodeNumber(this.fast.getCurrentEpisodeNumber() - 1);
                }
                sql.delete(EPISODE_TABLE, String.format("%s=?", ID), new String[]{Integer.toString(id)});
                numberEpisodes = -1;
                File toDelete = new File(getEpisodesDir(), episode.getAudioFileName());
                if (!toDelete.delete()) {
                    throw new ResourceAllocationException("Could not remove file from device");
                }
            } else {
                throw new ResourceAllocationException("Could not find episode to delete");
            }
        }
    }

    private static class IDAndLocation {
        public final String ID;
        public final String location;
        public IDAndLocation(int ID, String location) {
            this.ID = Integer.toString(ID);
            this.location = location;
        }

    }

    @Override
    public void deleteEpisodesBefore(int episodeNumber) throws ResourceAllocationException{
        List<IDAndLocation> episodes = new ArrayList<>();
        try (Cursor result = sql.query(EPISODE_TABLE, new String[]{ID, FILENAME, TITLE}, null , null, null, null, DOWNLOAD_DATE, "0," + episodeNumber)){
            while (result.moveToNext()) {
                IDAndLocation idl = new IDAndLocation(result.getInt(0), result.getString(1));
                episodes.add(idl);
            }
        }
        int currEpisodeNumber = this.fast.getCurrentEpisodeNumber();
        for (int i = 0 ; i < episodes.size() ; i++) {
            IDAndLocation episode = episodes.get(i);
            if (i <= currEpisodeNumber) {
                this.fast.setCurrentEpisodeNumber(--currEpisodeNumber);
            }
            sql.delete(EPISODE_TABLE, String.format("%s=?", ID), new String[]{episode.ID});
            numberEpisodes = -1;
            File toDelete = new File(getEpisodesDir(), episode.location);
            if (!toDelete.delete()){
                throw new ResourceAllocationException("Could not remove file from device");
            }
        }
        fast.setCurrentEpisodeNumber(0);
    }

    @Override
    public void putFeed(Feed feed) throws DuplicateException {
        ContentValues cv = new ContentValues();
        cv.put(URL, feed.getUrl());
        cv.put(TITLE, feed.getTitle());
        cv.put(DESCRIPTION, feed.getDescription());
        cv.put(COPYRIGHT, feed.getCopyright());
        cv.put(IMAGE_URL, feed.getImageurl());
        cv.put(LANGUAGE, feed.getLanguage());
        cv.put(LAST_EPISODE_DATE, 0);
        cv.put(SKIP_FIRST, 0);
        cv.put(SKIP_LAST, 0);
        cv.put(PRIORITIZE, 0);
        cv.put(DISABLED, 0);
        try {
            sql.insertOrThrow(FEED_TABLE, URL, cv);
        } catch (SQLiteConstraintException exception) {
            throw new DuplicateException(String.format(context.getString(R.string.feedAlreadyExists), feed.getTitle()));
        }
    }

    @Override
    public Feed updateFeedLastEpisodeDate(Feed feed, long lastEpisodeDate) {
        ContentValues cv = new ContentValues();
        cv.put(LAST_EPISODE_DATE, lastEpisodeDate);
        feed.setLastEpisodeDate(lastEpisodeDate);
        sql.update(FEED_TABLE, cv, String.format("%s=?", URL), new String[]{feed.getUrl()});
        return feed;
    }

    @Override
    public Feed updateFeedSkipFirstSeconds(Feed feed, int skipFirstSeconds) {
        ContentValues cv = new ContentValues();
        cv.put(SKIP_FIRST, skipFirstSeconds);
        feed.setSkipFirstSeconds(skipFirstSeconds);
        sql.update(FEED_TABLE, cv, String.format("%s=?", URL), new String[]{feed.getUrl()});
        return feed;
    }

    @Override
    public Feed updateFeedSkipLastSeconds(Feed feed, int skipLastSeconds) {
        ContentValues cv = new ContentValues();
        cv.put(SKIP_LAST, skipLastSeconds);
        feed.setSkipLastSeconds(skipLastSeconds);
        sql.update(FEED_TABLE, cv, String.format("%s=?", URL), new String[]{feed.getUrl()});
        return feed;
    }

    @Override
    public Feed updateFeedPriority(Feed feed, boolean prioritize) {
        ContentValues cv = new ContentValues();
        cv.put(PRIORITIZE, prioritize ? 1 : 0);
        feed.setPrioritized(prioritize);
        sql.update(FEED_TABLE, cv, String.format("%s=?", URL), new String[]{feed.getUrl()});
        return feed;
    }

    @Override
    public Feed updateFeedDisabled(Feed feed, boolean disabled) {
        ContentValues cv = new ContentValues();
        cv.put(DISABLED, disabled ? 1 : 0);
        feed.setDisabled(disabled);
        sql.update(FEED_TABLE, cv, String.format("%s=?", URL), new String[]{feed.getUrl()});
        return feed;
    }

    @Override
    public Feed getFeed(String url) {
        try (Cursor result = sql.query(FEED_TABLE, new String[]{URL, TITLE, LAST_EPISODE_DATE, DESCRIPTION, COPYRIGHT, IMAGE_URL, LANGUAGE,
                SKIP_FIRST, SKIP_LAST, PRIORITIZE, DISABLED}, String.format("%s=?", URL), new String[]{url}, null, null, null)){
            if (result.getCount() < 1) {
                return null;
            }
            result.moveToFirst();
            Feed feed = new Feed(result.getString(0));
            feed.setTitle(result.getString(1));
            feed.setLastEpisodeDate(result.getLong(2));
            feed.setDescription(result.getString(3));
            feed.setCopyright(result.getString(4));
            feed.setImageurl(result.getString(5));
            feed.setLanguage(result.getString(6));
            feed.setSkipFirstSeconds(result.getInt(7));
            feed.setSkipLastSeconds(result.getInt(8));
            feed.setPrioritized(result.getInt(9) != 0);
            feed.setDisabled(result.getInt(10) != 0);
            return feed;
        }
    }

    @Override
    public void deleteFeed(Feed feed) {
        sql.delete(FEED_TABLE, String.format("%s=?", URL), new String[]{feed.getUrl()});
    }

    @Override
    public List<Feed> getFeeds() {
        List<Feed> feeds = new ArrayList<>();
        try (Cursor result = sql.query(FEED_TABLE, new String[]{URL, TITLE, LAST_EPISODE_DATE, DESCRIPTION, COPYRIGHT, IMAGE_URL, LANGUAGE,
                SKIP_FIRST, SKIP_LAST, PRIORITIZE, DISABLED}, null, null, null, null, null)){
            while (result.moveToNext()) {
                Feed feed = new Feed(result.getString(0));
                feed.setTitle(result.getString(1));
                feed.setLastEpisodeDate(result.getLong(2));
                feed.setDescription(result.getString(3));
                feed.setCopyright(result.getString(4));
                feed.setImageurl(result.getString(5));
                feed.setLanguage(result.getString(6));
                feed.setSkipFirstSeconds(result.getInt(7));
                feed.setSkipLastSeconds(result.getInt(8));
                feed.setPrioritized(result.getInt(9) != 0);
                feed.setDisabled(result.getInt(10) != 0);
                feeds.add(feed);
            }
            return feeds;
        }
    }

    @Override
    public int getNumberEpisodesForFeed(String feedUrl) {
        String query = String.format("SELECT COUNT(*) AS total FROM %s WHERE %s = '%s'",
                EPISODE_TABLE, FEED, escape(feedUrl));
        try (Cursor result = sql.rawQuery(query, null)) {
            if (result.moveToFirst()) {
                return result.getInt(0);
            } else {
                return 0;
            }
        }
    }

    private String convertURLToFileName(String url) {
        try {
            if (url == null) {
                return "";
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(url.getBytes("UTF-8"));
            return bytesToHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void putFeedImage(Feed feed, InputStream is) throws IOException, ResourceAllocationException,
        EmergencyDownloadStopException{
        File dataFile = new File(getEpisodesDir(), convertURLToFileName(feed.getUrl()));
        if (dataFile.exists()) {
            if (!dataFile.delete()) {
                throw new ResourceAllocationException(context.getString(R.string.imageFileNotDeleted));
            }
        }
        if (!dataFile.createNewFile()) {
            throw new ResourceAllocationException(context.getString(R.string.couldNotCreateImageFile));
        }
        try (FileOutputStream fos = new FileOutputStream(dataFile)){
            Utils.IsToOs(is, fos, null);
        }
    }

    @Override
    public File getFeedImageFile(String url) throws ResourceAllocationException {
        File episodeFile = new File(getEpisodesDir(), convertURLToFileName(url));
        if (!episodeFile.exists()) {
            return null;
        } else {
            return episodeFile;
        }
    }


    private Episode makeEpisodeFromCursor(Cursor result) {
        Episode episode = new Episode(result.getString(0), result.getString(1));
        episode.setTitle(result.getString(2));
        episode.setPublishDate(result.getLong(3));
        episode.setDescription(result.getString(4));
        episode.setAudioUrl(result.getString(5));
        episode.setAudioSize(result.getLong(6));
        episode.setAudioType(result.getString(7));
        episode.setAudioFileName(result.getString(8));
        episode.setDownloadDate(result.getLong(9));
        episode.setAudioLocation(result.getInt(10));
        episode.setAudioDuration(result.getLong(11));
        return episode;
    }

    private String join(String joinVal, String... vals) {
        StringBuilder buf = new StringBuilder();
        int stopNum = vals.length - 1;
        for (int i = 0; i < vals.length; i++) {
            buf.append(vals[i]);
            if (i < stopNum) {
                buf.append(joinVal);
            }
        }
        return buf.toString();
    }

    public void putBackupData(String name, InputStream in) throws IOException, ResourceAllocationException, EmergencyDownloadStopException{
        File outputFile;
        if (name.equals(ODDPLAY_DB)) {
            outputFile = context.getDatabasePath(ODDPLAY_DB);
        } else if (name.equals("prefs")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String prefs = br.readLine();
            setPrefsFromString(prefs);
            return;
        } else {
            outputFile = new File(getEpisodesDir(), name);
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            Utils.IsToOs(in, fos, null);
        }
    }

    public Iterator<DumpInfo> getDBInputStream(boolean onlyMetaData) throws IOException, ResourceAllocationException {
        return new DumpInfoIterator(onlyMetaData);
    }

    private class DumpInfoIterator implements Iterator<DumpInfo> {
        private List<DumpInfo> infoList = new ArrayList<>();
        private final File[] allFiles;
        private int currentEpisode;
        private final boolean onlyMetaData;
        public DumpInfoIterator(boolean onlyMetaData) throws IOException, ResourceAllocationException{
            this.onlyMetaData = onlyMetaData;
            infoList.add(new DumpInfo(ODDPLAY_DB, new FileInputStream(context.getDatabasePath(ODDPLAY_DB))));
            InputStream prefsStream = new ByteArrayInputStream(DBStorage.this.getPrefsAsString().getBytes());
            infoList.add(new DumpInfo("prefs", prefsStream));
            allFiles = getEpisodesDir().listFiles();
        }

        @Override
        public boolean hasNext() {
            if (infoList != null && !infoList.isEmpty()) {
                return true;
            } else if (onlyMetaData) {
                return false;
            } else {
                return currentEpisode < allFiles.length;
            }
        }

        @Override
        public DumpInfo next() {
            try {
                if (infoList != null) {
                    if (!infoList.isEmpty()) {
                        return infoList.remove(0);
                    } else {
                        infoList = null;
                        currentEpisode = 0;
                    }
                }
                File nextFile = allFiles[currentEpisode++];
                return new DumpInfo(nextFile.getName(), new FileInputStream(nextFile));
            } catch (FileNotFoundException e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }

        @Override
        public void remove() {
            // not implemented
        }
    }

    private class EpisodeDB extends SQLiteOpenHelper implements AutoCloseable {

        public EpisodeDB(Context context) {
            super(context.getApplicationContext(), ODDPLAY_DB, null, VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + EPISODE_TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY, " +
                    commaJoinWithType(FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION,
                            AUDIO_URL, AUDIO_SIZE, AUDIO_TYPE, FILENAME, AUDIO_LOCATION,
                            DOWNLOAD_DATE, AUDIO_DURATION) + ");");
            db.execSQL("CREATE TABLE " + FEED_TABLE + " (" +
                    commaJoinWithType(URL, TITLE, LAST_EPISODE_DATE, DESCRIPTION, COPYRIGHT, IMAGE_URL,
                            LANGUAGE, SKIP_FIRST, SKIP_LAST, PRIORITIZE, DISABLED) + ", " +
                    "PRIMARY KEY (" + URL + "));");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d("On Upgrade", "oldV: " + oldVersion + ", newV: " + newVersion);
            if (oldVersion == 10 && newVersion == 11) {
                // db.beginTransaction();

                db.execSQL("CREATE TEMPORARY TABLE EP_BACKUP (" +
                        commaJoinWithType(FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION,
                                AUDIO_URL, AUDIO_SIZE, AUDIO_TYPE, FILENAME, AUDIO_LOCATION,
                                DOWNLOAD_DATE) + ", " +
                        "PRIMARY KEY (" + FEED + ", " + TITLE + ", " + PUBLISH_DATE + "));");
                db.execSQL("INSERT OR IGNORE INTO EP_BACKUP SELECT " +
                        join(", ",
                                FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION, AUDIO_URL, AUDIO_SIZE,
                                AUDIO_TYPE, FILENAME, AUDIO_LOCATION, DOWNLOAD_DATE) +
                        " FROM " + EPISODE_TABLE + ";");

                db.execSQL("DROP TABLE " + EPISODE_TABLE + ";");
                db.execSQL("CREATE TABLE " + EPISODE_TABLE + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        commaJoinWithType(FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION,
                                AUDIO_URL, AUDIO_SIZE, AUDIO_TYPE, FILENAME, AUDIO_LOCATION,
                                DOWNLOAD_DATE, AUDIO_DURATION) +
                        ");");
                db.execSQL("INSERT OR IGNORE INTO " + EPISODE_TABLE + " SELECT " +
                        join(", ",
                                "ROWID", FEED, FEED_TITLE, TITLE, PUBLISH_DATE, DESCRIPTION, AUDIO_URL,
                                AUDIO_SIZE, AUDIO_SIZE, AUDIO_TYPE, FILENAME, AUDIO_LOCATION,
                                DOWNLOAD_DATE) +
                        " FROM EP_BACKUP;");

                db.execSQL("DROP TABLE EP_BACKUP;");
                //db.endTransaction();
            }

        }

    }

}
