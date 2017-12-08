package com.jmw.rd.oddplay.feed;

import android.os.Parcel;
import android.os.Parcelable;

public class FeedSearchItem implements Parcelable {

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<FeedSearchItem> CREATOR = new Parcelable.Creator<FeedSearchItem>() {
        public FeedSearchItem createFromParcel(Parcel pc) {
            return new FeedSearchItem(pc);
        }

        public FeedSearchItem[] newArray(int size) {
            return new FeedSearchItem[size];
        }
    };
    private String url;
    private String title;
    private String description;

    FeedSearchItem() {
        url = "";
        title = "";
        description = "";
    }


    FeedSearchItem(Parcel pc) {
        url = pc.readString();
        title = pc.readString();
        description = pc.readString();
    }

    public String toString() {
        return String.format("Url: %s\nTitle: %s\nDescription: %s",
                url, title, description);
    }

    public String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }


    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeString(url);
        pc.writeString(title);
        pc.writeString(description);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
