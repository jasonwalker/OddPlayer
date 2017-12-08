package com.jmw.rd.oddplay.episode;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.jmw.rd.oddplay.play.PlayController;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.ImageHolder;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

import java.lang.ref.WeakReference;

public class EpisodeDetailPopup extends PopupDialogFragment {

    private static final String EPISODE_KEY = "episode";
    private Episode episode;
    private Storage storage;
    private boolean notDownloaded;
    private TextView episodeDetailEpisodeNumber;
    private ImageView feedImage;

    private PlayController playController;

    public static EpisodeDetailPopup newInstance(Episode episode, boolean notDownloaded) {
        EpisodeDetailPopup popup = new EpisodeDetailPopup();
        Bundle args = new Bundle();
        args.putParcelable(EPISODE_KEY, episode);
        popup.setArguments(args);
        popup.notDownloaded = notDownloaded;
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.episode = getArguments().getParcelable(EPISODE_KEY);
        playController = new PlayController(getActivity());
        this.storage = StorageUtil.getStorage(this.getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Window window = getDialog().getWindow();
        if (window == null) {
            throw new RuntimeException("Could not get window for episode detail popup.");
        }
        window.requestFeature(Window.FEATURE_NO_TITLE);

        View dialogLayout = inflater.inflate(R.layout.episode_detail, container, false);
        feedImage = (ImageView) dialogLayout.findViewById(R.id.episodeDetailImage);
        TextView episodeDetailTitle = (TextView) dialogLayout.findViewById(R.id.episodeDetailTitle);
        episodeDetailTitle.setText(this.episode.getTitle());
        TextView episodeDetailFeedName = (TextView) dialogLayout.findViewById(R.id.episodeDetailFeedName);
        episodeDetailFeedName.setText(this.episode.getFeedTitle());
        TextView episodeDetailPublishDate = (TextView) dialogLayout.findViewById(R.id.episodeDetailPublishDate);
        episodeDetailPublishDate.setText(Utils.dateStringFromLong(this.episode.getPublishDate()));
        episodeDetailPublishDate.setText(String.format(getActivity().getString(R.string.pubDate), Utils.dateStringFromLong(this.episode.getPublishDate())));
        TextView episodeDetailDuration = (TextView) dialogLayout.findViewById(R.id.episodeDetailDuration);
        episodeDetailDuration.setText(String.format(getActivity().getString(R.string.duration), Utils.formatTime(this.episode.getAudioDuration())));
        episodeDetailEpisodeNumber = (TextView) dialogLayout.findViewById(R.id.episodeDetailEpisodeNumber);

        if (this.notDownloaded) {
            episodeDetailDuration.setHeight(0);
            episodeDetailEpisodeNumber.setHeight(0);
        }
        TextView episodeDescription = (TextView) dialogLayout.findViewById(R.id.episodeDetailDescription);
        episodeDescription.setText(Html.fromHtml(episode.getDescription()));
        episodeDescription.setMovementMethod(new ScrollingMovementMethod());
        Button episodeDetailGotoButton = (Button) dialogLayout.findViewById(R.id.episodeDetailGotoButton);
        if (this.notDownloaded) {
            episodeDetailDuration.setHeight(0);
            episodeDetailEpisodeNumber.setHeight(0);
            episodeDetailGotoButton.setHeight(0);
            episodeDetailGotoButton.setVisibility(View.INVISIBLE);
        }
        PopulateViewTask populate = new PopulateViewTask(this);
        populate.execute();
        episodeDetailGotoButton.setOnTouchListener(new OnGotoListener());
        Button episodeDetailCancelButton = (Button) dialogLayout.findViewById(R.id.episodeDetailCancelButton);
        episodeDetailCancelButton.setOnTouchListener(new OnCancelClickListener());
        return dialogLayout;
    }

    private static class PopulateViewTask extends AsyncTask<Void, Object, Void> {
        private int episodeNumber;
        private Bitmap bitmap;
        private WeakReference<EpisodeDetailPopup> popupRef;

        PopulateViewTask(EpisodeDetailPopup popup) {
            popupRef = new WeakReference<>(popup);
        }

        @Override
        protected Void doInBackground(Void... unused) {
            EpisodeDetailPopup popup = popupRef.get();
            if (popup != null) {
                episodeNumber = popup.storage.getEpisodeNumber(popup.episode) + 1;
                bitmap = ImageHolder.getImageFromFeedUrl(popup.storage, popup.episode.getFeed());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            EpisodeDetailPopup popup = popupRef.get();
            if (popup != null) {
                popup.feedImage.setImageBitmap(bitmap);
                popup.episodeDetailEpisodeNumber.setText(String.format(popup.getActivity().getString(R.string.episodeNumber), episodeNumber));
            }
        }
    }

    private class OnGotoListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.e("JJJ", "On GOTO Listener");
                playController.goToEpisodeNumber(storage.getEpisodeNumber(episode));
                dismiss();
            }
            return false;
        }
    }

    private class OnCancelClickListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismiss();
            }
            return false;
        }
    }

}



