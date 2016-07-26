package com.jmw.rd.oddplay.episode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.jmw.rd.oddplay.download.DownloadController;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.download.NoWifiException;
import com.jmw.rd.oddplay.play.animations.FilterDisplayAnimation;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.storage.Feed;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.widgets.Dialog;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.storage.XMLGrabber;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

import java.io.IOException;
import java.util.ArrayList;

public class OldEpisodesPopup extends PopupDialogFragment implements EpisodeListAdapter.EpisodeSelectCountListener {
    private static final String FEED_KEY = "feed";

    private DownloadController downloadController;
    private Feed feed;
    private Storage storage;
    private EpisodeListAdapter episodeAdapter;
    private Button oldEpisodesDownloadButton;
    private LinearLayout oldEpisodeFilter;
    private EditText oldEpisodeFilterText;

    public static OldEpisodesPopup newInstance(Feed feed) {
        OldEpisodesPopup popup = new OldEpisodesPopup();
        Bundle args = new Bundle();
        args.putParcelable(FEED_KEY, feed);
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = StorageUtil.getStorage(getActivity());
        this.feed = getArguments().getParcelable(FEED_KEY);
        this.downloadController = new DownloadController(getActivity());
        episodeAdapter = new EpisodeListAdapter(getActivity(), false, true, false);
        episodeAdapter.addSelectCountListener(this);
        new PullFeedXMLTask(getActivity()).execute();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.old_episodes_detail, null);

        ListView episodeList = (ListView) dialogLayout.findViewById(R.id.oldEpisodesList);

        episodeList.setAdapter(episodeAdapter);
        episodeList.setOnItemClickListener(new ListClickListener());

        Button openFilterButton = (Button) dialogLayout.findViewById(R.id.oldOpenFilterButton);
        openFilterButton.setOnTouchListener(new OpenFilterListener());
        oldEpisodeFilter = (LinearLayout) dialogLayout.findViewById(R.id.oldEpisodeFilter);
        oldEpisodeFilterText = (EditText) dialogLayout.findViewById(R.id.oldEpisodeFilterText);
        oldEpisodeFilterText.addTextChangedListener(new EpisodeFilterChangeListener());
        episodeAdapter.setCurrentFilterSource(false);
        oldEpisodesDownloadButton = (Button) dialogLayout.findViewById(R.id.downloadOldEpisodesButton);
        oldEpisodesDownloadButton.setOnTouchListener(new DownloadOldEpisodes());
        oldEpisodesDownloadButton.setEnabled(false);
        Button oldEpisodesCancelButton = (Button) dialogLayout.findViewById(R.id.oldEpisodesCancelButton);
        oldEpisodesCancelButton.setOnTouchListener(new OnCancelClickListener());

        return dialogLayout;
    }

    @Override
    public void countChanged(int count) {
        oldEpisodesDownloadButton.setEnabled(count != 0);
    }

    class PullFeedXMLTask extends AsyncTask<Void, Episode, Void> {
        final Context context;
        Exception exception = null;

        public PullFeedXMLTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... unused) {
            XMLGrabber grabber = new XMLGrabber();
            try {
                grabber.passEpisodesIntoFunction(feed.getUrl(), feed.getTitle(), 0, 999, new CallPublish());

            } catch (IOException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void notUsed) {
            if (exception != null) {
                dismiss();
                Dialog.showOK(context, context.getString(R.string.noFeedConnect));
            } else {
                episodeAdapter.runFilter();
            }
        }

        @Override
        protected void onProgressUpdate(Episode... episodes) {
            for (Episode episode : episodes) {
                episodeAdapter.add(episode);
            }
            episodeAdapter.notifyDataSetChanged();
        }

        class CallPublish implements XMLGrabber.CallEpisodeFunction {
            public void call(Episode episode) {
                publishProgress(episode);
            }

            public EmergencyDownloadStopException doIStop() {
                return null;
            }
        }
    }

    private class DownloadOldEpisodes implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try {
                ArrayList<Episode> episodesToDownload = episodeAdapter.getSelectedEpisodes(true);
                downloadController.downloadSomeEpisodes(episodesToDownload);
                dismiss();
                Toast.makeText(getActivity(), String.format(getActivity().getString(R.string.currentlyDownloadingOld), episodesToDownload.size()), Toast.LENGTH_LONG).show();
                return true;
            } catch (NoWifiException e) {
                if (storage.fast.getUseOnlyWIFI()) {
                    Dialog.showOK(getActivity(), getActivity().getString(R.string.enableWifiForFeedsList));
                } else {
                    Dialog.showOK(getActivity(), getActivity().getString(R.string.enableWifiOrMobileForFeedsList));
                }
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

    private class ListClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment popup = EpisodeDetailPopup.newInstance(episodeAdapter.getItem(position), true);
            popup.show(transaction, "dialog");
        }
    }

    private class OpenFilterListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.e("JJJ", "event: " + motionEvent.getAction());
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                FilterDisplayAnimation animation = new FilterDisplayAnimation(getActivity(), oldEpisodeFilter, oldEpisodeFilterText, 90,
                        oldEpisodeFilter.getLayoutParams());
                animation.runAnimation();
            }
            return false;
        }
    }

    private class EpisodeFilterChangeListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            episodeAdapter.setCurrentFilterText(s);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
