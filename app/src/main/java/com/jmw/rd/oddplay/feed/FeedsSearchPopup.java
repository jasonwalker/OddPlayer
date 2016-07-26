package com.jmw.rd.oddplay.feed;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FeedsSearchPopup extends PopupDialogFragment implements FeedsSearchAdapter.FeedsSelectCountListener{
    private static final String SEARCH_STRING = "search";
    private Button addSearchFeedButton;
    private FeedsSearchAdapter adapter;
    private String searchString;
    private FeedController feedController;
    private FeedsSearch searcher;

    public static FeedsSearchPopup newInstance(String searchString) {
        FeedsSearchPopup popup = new FeedsSearchPopup();
        Bundle args = new Bundle();
        args.putString(SEARCH_STRING, searchString);
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchString = getArguments().getString(SEARCH_STRING);
        searcher = new FeedsSearchDigitalPodcast();
        adapter = new FeedsSearchAdapter(getActivity());
        adapter.addSelectCountListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.feeds_search_detail, null);
        ListView feedsList = (ListView) dialogLayout.findViewById(R.id.feedsList);

        feedsList.setAdapter(adapter);
        addSearchFeedButton = (Button) dialogLayout.findViewById(R.id.subscribeToEpisodeButton);
        addSearchFeedButton.setOnTouchListener(new OnSubscribeClickListener());
        Button dismissButton = (Button) dialogLayout.findViewById(R.id.dismissButton);
        dismissButton.setOnTouchListener(new OnCancelClickListener());
        TextView searchTermView = (TextView) dialogLayout.findViewById(R.id.searchTermText);
        searchTermView.setText(this.searchString);
        feedController = new FeedController(getActivity());
        GetSearch search = new GetSearch(searchString);
        search.execute();
        return dialogLayout;
    }

    class GetSearch extends AsyncTask<Void, List<FeedSearchItem>, Void> {
        private final String keywords;
        public GetSearch(String keywords){
            this.keywords = keywords;
        }

        @Override
        protected Void doInBackground(Void... unused) {
            if (false) {
                List<FeedSearchItem> items = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    FeedSearchItem fsi = new FeedSearchItem();
                    fsi.setTitle("Title: " + i);
                    fsi.setUrl("Url: " + i);
                    fsi.setDescription("Description: " + i);
                    items.add(fsi);
                }
                publishProgress(items);
            } else {
                try {
                    List<FeedSearchItem> newFeeds = searcher.searchFeeds(keywords);
                    publishProgress(newFeeds);
                } catch (IOException e) {
                    //TODO
                }
            }

            return null;
        }


        @SafeVarargs
        @Override
        protected final void onProgressUpdate(List<FeedSearchItem>... newFeeds) {
            for (List<FeedSearchItem> newFeed : newFeeds) {
                adapter.addAll(newFeed);
            }
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public void countChanged(int count) {
        addSearchFeedButton.setEnabled(count != 0);
    }

    private class OnSubscribeClickListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                for (FeedSearchItem feed : adapter.getSelectedFeeds()) {
                    feedController.broadcastAddFeed(feed.getUrl());
                }
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
