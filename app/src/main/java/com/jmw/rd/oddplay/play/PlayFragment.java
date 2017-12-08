package com.jmw.rd.oddplay.play;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jmw.rd.oddplay.ImageHolder;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.lang.ref.WeakReference;

public class PlayFragment extends Fragment {
    private int position;
    private long episodeId;

    private TextView feedName;
    private TextView feedDate;
    private TextView title;
    private TextView description;
    private ImageView feedImage;

    static PlayFragment newInstance(int position, long id) {
        PlayFragment frag = new PlayFragment();
        frag.init(position, id);
        return frag;
    }

    public Long getEpisodeId() {
        return this.episodeId;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    void init(int position, long id) {
        setPosition(position);
        this.episodeId = id;
        // must know episode immediately
        //this.episode = StorageUtil.getStorage(getContext()).getEpisode(position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play, container, false);
        feedName = (TextView) view.findViewById(R.id.feedName);
        feedDate = (TextView) view.findViewById(R.id.feedDate);
        title = (TextView) view.findViewById(R.id.playTitle);
        feedImage = (ImageView) view.findViewById(R.id.feedImage);
        description = (TextView) view.findViewById(R.id.playDisplay);
        ScrollingMovementMethod scrollFragment = new ScrollingMovementMethod();
        description.setMovementMethod(scrollFragment);
        PopulateViewTask populate = new PopulateViewTask(this);
        populate.execute();
        return view;
    }

    private static class PopulateViewTask extends AsyncTask<Void, Object, Void> {
        private Bitmap image;
        private String dateString;
        private Episode episode;
        private WeakReference<PlayFragment> fragRef;

        private PopulateViewTask(PlayFragment fragment) {
            this.fragRef = new WeakReference<>(fragment);
        }

        @Override
        protected Void doInBackground(Void... unused) {
            PlayFragment fragment = fragRef.get();
            if (fragment != null) {
                Storage storage = StorageUtil.getStorage(fragment.getContext());
                episode = storage.getEpisode(fragment.episodeId);
                if (episode != null) {
                    publishProgress(episode);
                    dateString = Utils.dateStringFromLong(episode.getPublishDate());
                    publishProgress(dateString);
                    image = ImageHolder.getImageFromFeedUrl(storage, episode.getFeed());
                    publishProgress(image);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... vals) {
            PlayFragment fragment = fragRef.get();
            if (fragment != null) {
                for (Object val : vals) {
                    if (val instanceof Episode) {
                        fragment.feedName.setText(Html.fromHtml(episode.getFeedTitle()));
                        fragment.title.setText(Html.fromHtml(episode.getTitle()));
                        fragment.description.setText(Html.fromHtml(episode.getDescription()));
                    } else if (val instanceof String) {
                        fragment.feedDate.setText(dateString);
                    } else if (val instanceof Bitmap) {
                        fragment.feedImage.setImageBitmap(image);
                    }
                }
            }
        }
    }

}
