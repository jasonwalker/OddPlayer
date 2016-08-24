package com.jmw.rd.oddplay.feed;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jmw.rd.oddplay.PodPage;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.storage.Feed;
import com.jmw.rd.oddplay.ImageHolder;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.storage.DuplicateException;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.storage.XMLGrabber;
import com.jmw.rd.oddplay.widgets.Dialog;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedsViewPage extends PodPage {

    private static final boolean PRODUCTION = true;

    private Storage storage;
    private FeedAdapter feedAdapter;
    private UrlAddedReceiver urlAddedReceiver;
    private Button deleteFeedButton;
    private int numberFeedsClicked = 0;
    private FeedController feedController;
    private String urlToAddOnResume = null;
    private UrlInputPopup urlInputDialog;

    public FeedsViewPage() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        feedController = new FeedController(context);
        urlAddedReceiver = new UrlAddedReceiver();
        feedController.registerAddFeedListener(this.urlAddedReceiver);
    }

    public synchronized void setUrlToAdd(String url) {
        if (this.isResumed()) {
            createInputDialog();
            urlInputDialog.showEnterUrlPopup(url);
            //feedController.broadcastAddFeed(url);
        } else {
            this.urlToAddOnResume = url;
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (urlToAddOnResume != null) {
            //feedController.broadcastAddFeed(urlToAddOnResume);
            createInputDialog();
            urlInputDialog.showEnterUrlPopup(urlToAddOnResume);
            urlToAddOnResume = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        feedController.unregisterUrlAddedListener(this.urlAddedReceiver);
        this.urlAddedReceiver = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedController = new FeedController(activity);
        storage = StorageUtil.getStorage(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View result = inflater.inflate(R.layout.main_view_feeds, container, false);
        Button addFeedButton = (Button) result.findViewById(R.id.addFeedButton);
        addFeedButton.setOnTouchListener(new AddFeedListener());
        deleteFeedButton = (Button) result.findViewById(R.id.deleteFeedButton);
        deleteFeedButton.setOnTouchListener(new DeleteFeedListener());
        deleteFeedButton.setEnabled(false);
        PopulateFeedAdapterTask populate = new PopulateFeedAdapterTask();
        populate.execute();
        ListView feedList = (ListView) result.findViewById(R.id.feedList);
        feedAdapter = new FeedAdapter(feedList);
        return result;
    }

    private class PopulateFeedAdapterTask extends AsyncTask<Void, Feed, Void> {
        @Override
        protected Void doInBackground(Void... unused) {
            for(Feed feed : storage.getFeeds()) {
                publishProgress(feed);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Feed... feeds) {
            for(Feed feed : feeds) {
                feedAdapter.addFeed(feed);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            feedAdapter.notifyDataSetChanged();
        }
    }

    private void createInputDialog() {
        urlInputDialog = UrlInputPopup.newInstance();//new UrlInputDialog(myContext);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        urlInputDialog.show(transaction, "urldialog");
    }

    private void addFeed() {
        if (Utils.isDataOn(activity)) {
            if (PRODUCTION) {
                createInputDialog();
            } else {
                String[] feedStrings = new String[7];
                for (int i = 0; i < 7; i++) {
                    feedStrings[i] = "http://192.168.55.111:8080/" + i;
                }
                addFeedUrlAsync(feedStrings);
            }
        }  else {
            if (storage.fast.getUseOnlyWIFI()) {
                Dialog.showOK(activity, activity.getString(R.string.enableWifiToAddFeeds));
            } else {
                Dialog.showOK(activity, activity.getString(R.string.enableWifiOrMobileToAddFeeds));
            }
        }
    }

    private void addFeedUrlAsync(String... url) {
        GetFeedTask testGetFeed = new GetFeedTask();
        testGetFeed.execute(url);
    }

    private void getImageForFeed(Storage storage, Feed feed) {
        InputStream is = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(feed.getImageurl()).openConnection();
            is = connection.getInputStream();
            storage.putFeedImage(feed, is);
        } catch (IOException e) {
            //just failed to get image for podcast, not very bad
            Toast.makeText(activity, "Could not retrieve image for podcast", Toast.LENGTH_SHORT).show();
        } catch(ResourceAllocationException e){
            Dialog.showOK(activity, e.getMessage());
        } catch(EmergencyDownloadStopException e) {
            //don't do anything here, just breaking out of download
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // can't do much here
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void deleteSelected() {
        int counter = 0;
        List<FeedSelectedTracker> tracker = FeedsViewPage.this.feedAdapter.feedTracker;
        for (int i = tracker.size() - 1; i >= 0; i--) {
            if (tracker.get(i).selected) {
                counter++;
            }
        }
        if (counter > 0) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        final DeleteFeedsTask deleteTask = new DeleteFeedsTask();
                        deleteTask.execute();
                    }
                }
            };
            Dialog.showYesNo(activity, String.format(activity.getString(R.string.sureToDeleteFeeds), counter),
                    dialogClickListener, dialogClickListener);
        }
    }

    private class FeedNameNumber {
        public final String name;
        public final int number;
        public FeedNameNumber(String name, int number) {
            this.name = name;
            this.number = number;
        }
    }
    private class DeleteFeedsTask extends AsyncTask<Void, FeedNameNumber, Void> {
        AlertDialog previousDialog;
        public DeleteFeedsTask() {
        }

        @Override
        protected Void doInBackground(Void... unused) {
            List<FeedSelectedTracker> tracker = FeedsViewPage.this.feedAdapter.feedTracker;
            for (int i = tracker.size() - 1; i >= 0; i--) {
                if (tracker.get(i).selected) {
                    String name = tracker.get(i).feed.getTitle();
                    storage.deleteFeed(tracker.get(i).feed);
                    publishProgress(new FeedNameNumber(name, i));
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(FeedNameNumber... items) {
            for (FeedNameNumber item : items) {
                feedAdapter.deleteFeed(item.number);
                feedAdapter.notifyDataSetChanged();
                if (previousDialog != null && previousDialog.isShowing()) {
                    previousDialog.dismiss();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            numberFeedsClicked = 0;
            deleteFeedButton.setEnabled(false);
        }
    }

    private class RefreshImagesTask extends AsyncTask<Void, String, Void> {
        public RefreshImagesTask() {
        }

        @Override
        protected Void doInBackground(Void... unused) {
            for (Feed feed : storage.getFeeds()) {
                getImageForFeed(storage, feed);
            }
            return (null);
        }

        @Override
        protected void onProgressUpdate(String... item) {
            Toast.makeText(activity, item[0], Toast.LENGTH_SHORT).show();
        }
    }

    private class FeedInfo {
        final Feed feed;
        final String info;
        final String url;
        final boolean isException;
        public FeedInfo(Feed feed, String url, String info) {
            this.feed = feed;
            this.info = info;
            this.url = url;
            this.isException = false;
        }
        public FeedInfo(String info, String url, boolean isException) {
            this.feed = null;
            this.url = url;
            this.info = info;
            this.isException = isException;
        }
    }

    class GetFeedTask extends AsyncTask<String, FeedInfo, Void> {
        private AlertDialog previousDialog;
        private FeedInfo lastFeedInfo = null;
        public GetFeedTask() {
        }

        @Override
        protected Void doInBackground(String... urls) {
            XMLGrabber grabber = new XMLGrabber();
            for (String url : urls) {
                try {
                    publishProgress(new FeedInfo(String.format(activity.getString(R.string.attemptingToEnterFeed), url), url, false));
                    Feed newFeed = grabber.getFeed(url);
                    storage.putFeed(newFeed);
                    getImageForFeed(storage, newFeed);
                    publishProgress(new FeedInfo(newFeed, url, String.format(activity.getString(R.string.feedSuccessfullyEntered), newFeed.getTitle())));
                    feedController.broadcastFeedAdded(url, true);
                } catch (DuplicateException e) {
                    publishProgress(new FeedInfo(String.format(activity.getString(R.string.duplicateFeed), e.getMessage()), url, true));
                    feedController.broadcastFeedAdded(url, false);
                } catch (Exception e) {
                    publishProgress(new FeedInfo(String.format(activity.getString(R.string.feedHasMalformedXML), url), url, true));
                    feedController.broadcastFeedAdded(url, false);
                }
            }
            return (null);
        }

        @Override
        protected void onProgressUpdate(FeedInfo... items) {
            for (FeedInfo fi : items) {
                if (previousDialog != null && previousDialog.isShowing() && (lastFeedInfo == null || !lastFeedInfo.isException)) {
                    previousDialog.dismiss();
                }
                lastFeedInfo = fi;
                previousDialog = Dialog.showOK(activity, fi.info);
                if (fi.feed != null) {
                    feedAdapter.addFeed(fi.feed);
                }
            }

        }

        @Override
        protected void onPostExecute(Void result) {
            if (previousDialog != null && previousDialog.isShowing() && (lastFeedInfo == null || !lastFeedInfo.isException)){
                previousDialog.dismiss();
            }
            //TODO This feels like a hack
            // if url input failed, reshow dialog with previous entry so user can fix it
            if (lastFeedInfo != null && lastFeedInfo.isException) {
                previousDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (urlInputDialog != null && urlInputDialog.isVisible()) {
                            urlInputDialog.showEnterUrlPopup(lastFeedInfo.url);
                        }
                    }
                });
            } else {
                if (lastFeedInfo != null) {
                    Toast.makeText(activity, String.format("Successfully entered feed: %1$s", lastFeedInfo.feed.getTitle()), Toast.LENGTH_LONG).show();
                }
                feedAdapter.notifyDataSetChanged();
            }
        }
    }

    class ViewHolder {
        TextView feedName;
        TextView numberEpisodes;
        CheckBox checkbox;
        ImageView feedImage;
        View feedSelectClick;
        int position;
    }

    private class FeedSelectedTracker {
        final Feed feed;
        boolean selected;
        public Bitmap image;
        public int numberEpisodes;
        public ViewHolder holder;

        public FeedSelectedTracker(Feed feed) {
            this.feed = feed;
            this.selected = false;
        }

    }

    /**
     * BUTTON Listeners
     */
    private class AddFeedListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                addFeed();
            }
            return false;
        }
    }

    private class DeleteFeedListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                deleteSelected();
            }
            return false;
        }
    }

    class UrlAddedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(FeedController.URL_ADDRESS);
            addFeedUrlAsync(address);
        }
    }

    private class FeedAdapter extends BaseAdapter {
        final List<FeedSelectedTracker> feedTracker;

        public FeedAdapter(final ListView feedList) {
            super();
            this.feedTracker = new ArrayList<>();
            feedList.setAdapter(this);
            feedList.setOnItemClickListener(new ListClickListener());
        }

        public void addFeed(final Feed feed) {
            this.feedTracker.add(new FeedSelectedTracker(feed));
        }

        public void deleteFeed(int position) {
            this.feedTracker.remove(position);
        }


        @Override
        public int getCount() {
            return this.feedTracker.size();
        }

        @Override
        public Feed getItem(int i) {
            return feedTracker.get(i).feed;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FeedSelectedTracker tracker = feedTracker.get(position);
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) activity.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.feeds_layout, null);

                tracker.holder = new ViewHolder();
                tracker.holder.feedSelectClick = convertView.findViewById(R.id.feedSelectClick);
                tracker.holder.feedName = (TextView) convertView.findViewById(R.id.feedName);
                tracker.holder.checkbox = (CheckBox) convertView.findViewById(R.id.feedCheckbox);
                tracker.holder.feedImage = (ImageView) convertView.findViewById(R.id.feedImage);
                tracker.holder.numberEpisodes = (TextView) convertView.findViewById(R.id.numberEpisodesLabel);
                convertView.setTag(tracker.holder);
                tracker.holder.feedSelectClick.setOnClickListener(new FeedSelectClickListener(tracker.holder));
            } else {
                tracker.holder = (ViewHolder) convertView.getTag();
            }

            tracker.holder.feedName.setText(tracker.feed.getTitle());
            tracker.holder.position = position;
            tracker.holder.checkbox.setChecked(tracker.selected);
            new PopulateHolderTask().execute(tracker);

            if (tracker.feed.isDisabled()) {
                convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.downloadedEpisodeInList));
            } else {
                convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.defaultEpisodeInList));
            }
            feedAdapter.notifyDataSetChanged();
            return convertView;
        }

        private class PopulateHolderTask extends AsyncTask<FeedSelectedTracker, FeedSelectedTracker, Void> {

            @Override
            protected Void doInBackground(FeedSelectedTracker... trackers) {
                for (FeedSelectedTracker tracker : trackers) {
                    tracker.image = ImageHolder.getImageFromFeedUrl(storage, tracker.feed.getUrl());
                    tracker.numberEpisodes = storage.getNumberEpisodesForFeed(tracker.feed.getUrl());
                    publishProgress(tracker);
                }
                return null;
            }
            @Override
            protected void onProgressUpdate(FeedSelectedTracker... trackers) {
                for (FeedSelectedTracker tracker : trackers) {
                    if (tracker.image != null) {
                        tracker.holder.feedImage.setImageBitmap(tracker.image);
                    }
                    tracker.holder.numberEpisodes.setText(String.format(activity.getString(R.string.numberEpisodes), tracker.numberEpisodes));
                }
            }
        }

        class FeedSelectClickListener implements View.OnClickListener {
            private final ViewHolder holder;

            public FeedSelectClickListener(ViewHolder holder) {
                this.holder = holder;
            }

            @Override
            public void onClick(View view) {
                if (holder.checkbox.isChecked()) {
                    numberFeedsClicked++;
                } else {
                    numberFeedsClicked--;
                }
                if (numberFeedsClicked == 0) {
                    deleteFeedButton.setEnabled(false);
                } else {
                    deleteFeedButton.setEnabled(true);
                }
                holder.checkbox.setChecked(!holder.checkbox.isChecked());
                feedTracker.get(holder.position).selected = holder.checkbox.isChecked();
            }
        }

        class ListClickListener implements AdapterView.OnItemClickListener {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedDetailPopup feedDetailPopup = FeedDetailPopup.newInstance(feedAdapter, feedTracker.get(position).feed);
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                feedDetailPopup.show(transaction, "dialog");
            }
        }
    }
}
