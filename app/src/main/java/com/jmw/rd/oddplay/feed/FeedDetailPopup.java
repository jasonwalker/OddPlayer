package com.jmw.rd.oddplay.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jmw.rd.oddplay.storage.Feed;
import com.jmw.rd.oddplay.ImageHolder;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.episode.OldEpisodesPopup;
import com.jmw.rd.oddplay.widgets.Dialog;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

public class FeedDetailPopup extends PopupDialogFragment {

    private static final String FEED_KEY = "feed";

    private Feed feed;

    private FragmentActivity context;
    private Storage storage;
    private CheckBox prioritizeDownload;
    private CheckBox disableFeed;
    private BaseAdapter adapter;
    private TextView numberEpisodesView;
    private ImageView feedImage;

    public FeedDetailPopup() {

    }

    public static FeedDetailPopup newInstance(BaseAdapter adapter, Feed feed) {
        FeedDetailPopup popup = new FeedDetailPopup();
        popup.adapter = adapter;
        Bundle args = new Bundle();
        args.putParcelable(FEED_KEY, feed);
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();
        storage = StorageUtil.getStorage(context);
        this.feed = getArguments().getParcelable(FEED_KEY);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.feed_detail, null);
        feedImage = (ImageView) dialogLayout.findViewById(R.id.feedDetailImage);
        TextView titleView = (TextView) dialogLayout.findViewById(R.id.feedDetailTitle);
        titleView.setText(this.feed.getTitle());
        TextView urlView = (TextView) dialogLayout.findViewById(R.id.feedDetailUrl);
        urlView.setText(this.feed.getUrl());
        TextView lastPublishDateView = (TextView) dialogLayout.findViewById(R.id.feedDetailLastPublishDate);
        lastPublishDateView.setText(String.format(context.getString(R.string.lastPubDate), Utils.dateStringFromLong(this.feed.getLastEpisodeDate())));
        numberEpisodesView = (TextView) dialogLayout.findViewById(R.id.feedDetailNumberEpisodes);
        Button downloadOlderEpisodesButton = (Button) dialogLayout.findViewById(R.id.feedDetailDownloadOlderButton);
        downloadOlderEpisodesButton.setOnTouchListener(new OnDownloadOlderEpisodesListener());
        prioritizeDownload = (CheckBox) dialogLayout.findViewById(R.id.prioritizeFeed);
        prioritizeDownload.setChecked(feed.isPrioritized());
        prioritizeDownload.setOnClickListener(new PrioritizeFeedListener());
        disableFeed = (CheckBox) dialogLayout.findViewById(R.id.disableFeed);
        disableFeed.setChecked(feed.isDisabled());
        disableFeed.setOnClickListener(new DisableFeedListener());
        EditText skipFirstText = (EditText) dialogLayout.findViewById(R.id.skipFirstInput);
        skipFirstText.setText(Integer.toString(this.feed.getSkipFirstSeconds()));
        skipFirstText.addTextChangedListener(new SkipFirstSecondsListener());
        EditText skipLastText = (EditText) dialogLayout.findViewById(R.id.skipLastInput);
        skipLastText.setText(Integer.toString(this.feed.getSkipLastSeconds()));
        skipLastText.addTextChangedListener(new SkipLastSecondsListener());
        Button feedDetailCancelButton = (Button) dialogLayout.findViewById(R.id.feedDetailCancelButton);
        feedDetailCancelButton.setOnTouchListener(new OnCancelClickListener());
        PopulateViewTask populate =new PopulateViewTask();
        populate.execute();
        return dialogLayout;
    }

    private class PopulateViewTask extends AsyncTask<Void, Object, Void> {
        private String numberEpisodes;
        private Bitmap bitmap;
        @Override
        protected Void doInBackground(Void... unused) {
            bitmap = ImageHolder.getImageFromFeedUrl(storage, feed.getUrl());
            numberEpisodes = String.format(context.getString(R.string.numberEpisodes), storage.getNumberEpisodesForFeed(feed.getUrl()));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            feedImage.setImageBitmap(bitmap);
            numberEpisodesView.setText(numberEpisodes);
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

    private class OnDownloadOlderEpisodesListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (Utils.isDataOn(context)) {
                    OldEpisodesPopup oldEpisodesPopup = OldEpisodesPopup.newInstance(feed);
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    oldEpisodesPopup.show(transaction, "oldEpisodesPopup");
                } else {
                    if (storage.fast.getUseOnlyWIFI()) {
                        Dialog.showOK(context, context.getString(R.string.enableWifiForEpisodeList));
                    } else {
                        Dialog.showOK(context, context.getString(R.string.enableWifiOrMobileForEpisodeList));
                    }
                }
                dismiss();
            }
            return false;
        }
    }

    private class SkipFirstSecondsListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String val = s.toString();
            int toSkipFirst = 0;
            try {
                if (val.length() > 0) {
                    toSkipFirst = Integer.parseInt(val);
                }
                storage.updateFeedSkipFirstSeconds(feed, toSkipFirst);
            } catch (NumberFormatException e) {
                Dialog.showOK(getActivity(), String.format(context.getString(R.string.skipFirstBadNumber), val));
            }
        }
    }

    private class SkipLastSecondsListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String val = s.toString();
            int toSkipLast = 0;
            try {
                if (val.length() > 0) {
                    toSkipLast = Integer.parseInt(val);
                }
                storage.updateFeedSkipLastSeconds(feed, toSkipLast);
            } catch (NumberFormatException e) {
                Dialog.showOK(getActivity(), String.format(context.getString(R.string.skipLastBadNumber), val));
            }
        }
    }

    private class PrioritizeFeedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            storage.updateFeedPriority(FeedDetailPopup.this.feed, prioritizeDownload.isChecked());
        }
    }

    private class DisableFeedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            storage.updateFeedDisabled(FeedDetailPopup.this.feed, disableFeed.isChecked());
            adapter.notifyDataSetChanged();
        }
    }

}



