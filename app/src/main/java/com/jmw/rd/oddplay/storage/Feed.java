package com.jmw.rd.oddplay.storage;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Feed implements Parcelable,Serializable {

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Feed> CREATOR = new Parcelable.Creator<Feed>() {
        public Feed createFromParcel(Parcel pc) {
            return new Feed(pc);
        }

        public Feed[] newArray(int size) {
            return new Feed[size];
        }
    };
    private final String url;
    private String title;
    private long lastEpisodeDate;
    private String copyright;
    private String imageUrl;
    private String description;
    private int skipFirstSeconds;
    private int skipLastSeconds;
    private String language;
    private boolean prioritizeDownload;
    private boolean disabled;

    public Feed(String url) {
        this.url = url;
        title = "";
        lastEpisodeDate = 0;
        copyright = "";
        imageUrl = "";
        description = "";
        language = "";
        skipFirstSeconds = 0;
        skipLastSeconds = 0;
        prioritizeDownload = false;
        disabled = false;
    }


    public Feed(Parcel pc) {
        url = pc.readString();
        title = pc.readString();
        lastEpisodeDate = pc.readLong();
        copyright = pc.readString();
        imageUrl = pc.readString();
        description = pc.readString();
        language = pc.readString();
        skipFirstSeconds = pc.readInt();
        skipLastSeconds = pc.readInt();
        prioritizeDownload = pc.readByte() != 0;
        disabled = pc.readByte() != 0;
    }

    public String toString() {
        return String.format("Title: %s\nURL: %s\nDate Last Ep: %d\nCopyright: %s\nDescription: %s\n" +
                "Language: %s\nImage: %s\nSkip first: %d\nSkip last: %d\nPrioritize: %b\n" +
                        "Disabled: %b",
                title, url, lastEpisodeDate, copyright, description, language, this.getImageurl(),
                skipFirstSeconds, skipLastSeconds, prioritizeDownload, disabled);
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public long getLastEpisodeDate() {
        return lastEpisodeDate;
    }

    void setLastEpisodeDate(long date) {
        this.lastEpisodeDate = date;
    }

    public String getUrl() {
        return url;
    }

    public String getCopyright() {
        return copyright;
    }

    void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getImageurl() {
        return imageUrl;
    }

    void setImageurl(String imageurl) {
        this.imageUrl = imageurl;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    void setLanguage(String language) {
        this.language = language;
    }

    public int getSkipFirstSeconds() {
        return this.skipFirstSeconds;
    }

    void setSkipFirstSeconds(int seconds) {
        this.skipFirstSeconds = seconds;
    }

    public int getSkipLastSeconds() {
        return this.skipLastSeconds;
    }

    void setSkipLastSeconds(int seconds) {
        this.skipLastSeconds = seconds;
    }

    public boolean isPrioritized() {
        return this.prioritizeDownload;
    }

    void setPrioritized(boolean priority) {
        this.prioritizeDownload = priority;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeString(url);
        pc.writeString(title);
        pc.writeLong(lastEpisodeDate);
        pc.writeString(copyright);
        pc.writeString(imageUrl);
        pc.writeString(description);
        pc.writeString(language);
        pc.writeInt(skipFirstSeconds);
        pc.writeInt(skipLastSeconds);
        pc.writeByte((byte) (this.prioritizeDownload ? 1 : 0));
        pc.writeByte((byte) (this.disabled ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
