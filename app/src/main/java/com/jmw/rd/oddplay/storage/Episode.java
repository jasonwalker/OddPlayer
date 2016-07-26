package com.jmw.rd.oddplay.storage;


import android.os.Parcel;
import android.os.Parcelable;

import org.jsoup.Jsoup;

public class Episode implements Parcelable {
    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Episode> CREATOR = new Parcelable.Creator<Episode>() {
        public Episode createFromParcel(Parcel pc) {
            return new Episode(pc);
        }

        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };
    private final String feed;
    private final String feedTitle;
    private String title;
    private long publishDate;
    private long downloadDate;
    private String description;
    private String audioUrl;
    private long audioSize = 0;
    private int audioLocation;
    private String audioType;
    private String audioFileName;
    private long audioDuration;

    public Episode(String feed, String feedTitle) {
        this.feed = feed;
        this.feedTitle = (feedTitle == null) ? "" : feedTitle;
        title = "";
        publishDate = 0;
        downloadDate = 0;
        description = "";
        audioUrl = "";
        audioSize = 0;
        audioLocation = 0;
        audioType = "";
        audioFileName = "";
        audioDuration = 0;
    }

    public Episode(Parcel pc) {
        feed = pc.readString();
        feedTitle = pc.readString();
        title = pc.readString();
        publishDate = pc.readLong();
        downloadDate = pc.readLong();
        description = pc.readString();
        audioUrl = pc.readString();
        audioSize = pc.readLong();
        audioLocation = pc.readInt();
        audioType = pc.readString();
        audioFileName = pc.readString();
        audioDuration = pc.readLong();
    }

    public String toString() {
        return String.format("Title: %s\nFeed title: %s\nPublish date: %d\nDownload date: %d\nDescription: %s\n" +
            "Audio url: %s\nAudioSize: %d\nAudio location: %d\nAudio type: %s\nAudio filename: %s\n" +
                        "Audio duration: %d",
            title, feedTitle, publishDate, downloadDate, description, audioUrl, audioSize, audioLocation,
                audioType, audioFileName, audioDuration);

    }

    public boolean equals(Episode episode) {
        return episode != null &&
                this.title.equals(episode.getTitle()) &&
                this.publishDate == episode.getPublishDate() &&
                this.feed.equals(episode.getFeed());
    }

    public String getFeed() {
        return feed;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public long getPublishDate() {
        return publishDate;
    }

    void setPublishDate(long publishDate) {
        this.publishDate = publishDate;
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    void setDownloadDate(long downloadDate) {
        this.downloadDate = downloadDate;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        // decided to keep 'uncleaned' data in db in case want to use it later
        this.description = Jsoup.parse(description).text();
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public long getAudioSize() {
        return audioSize;
    }

    void setAudioSize(long audioSize) {
        this.audioSize = audioSize;
    }

    public int getAudioLocation() {
        return audioLocation;
    }

    void setAudioLocation(int audioLocation) {
        this.audioLocation = audioLocation;
    }

    public String getAudioType() {
        return audioType;
    }

    void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public long getAudioDuration() {
        return audioDuration;
    }

    void setAudioDuration(long audioDuration) {
        this.audioDuration = audioDuration;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeString(feed);
        pc.writeString(feedTitle);
        pc.writeString(title);
        pc.writeLong(publishDate);
        pc.writeLong(downloadDate);
        pc.writeString(description);
        pc.writeString(audioUrl);
        pc.writeLong(audioSize);
        pc.writeInt(audioLocation);
        pc.writeString(audioType);
        pc.writeString(audioFileName);
        pc.writeLong(audioDuration);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

