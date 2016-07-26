package com.jmw.rd.oddplay.episode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Filter;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import java.util.ArrayList;
import java.util.List;

class EpisodeListAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final Storage storage;
    private final List<EpisodeSelectedTracker> allEpisodes;
    private final List<Integer> displayedEpisodes;
    private final boolean highlightCurrentEpisode;
    private final boolean disableExistingEpisodes;
    private final boolean showDuration;
    private CharSequence currentFilter = "";
    private boolean useFeedTitle = true;
    private int numberEpisodesSelected = 0;
    private final List<EpisodeSelectCountListener> selectCountListeners;

    /**
     * @param context the context
     * @param highlightCurrentEpisode make current episode's color different from others
     * @param disableExistingEpisodes disable clicking on episodes that can be found in db
     */
    public EpisodeListAdapter(Context context,
                              boolean highlightCurrentEpisode, boolean disableExistingEpisodes,
                              boolean showDuration) {
        super();
        this.context = context;
        this.storage = StorageUtil.getStorage(context);
        this.allEpisodes = new ArrayList<>();
        this.displayedEpisodes = new ArrayList<>();
        this.highlightCurrentEpisode = highlightCurrentEpisode;
        this.disableExistingEpisodes = disableExistingEpisodes;
        this.showDuration = showDuration;
        selectCountListeners = new ArrayList<>();
        runFilter();
    }

    public void addSelectCountListener(EpisodeSelectCountListener listener) {
        this.selectCountListeners.add(listener);
    }

    public void removeSelectCountListener(EpisodeSelectCountListener listener) {
        this.selectCountListeners.remove(listener);
    }

    public void setCurrentFilterText(CharSequence filter) {
        this.currentFilter = filter;
        runFilter();
    }

    public void setCurrentFilterSource(boolean feedTitle) {
        useFeedTitle = feedTitle;
    }

    public void runFilter() {
        getFilter().filter(this.currentFilter);
    }

    public void runFilter(Filter.FilterListener listener) {
        getFilter().filter(this.currentFilter, listener);
    }

    public void add(final Episode episode) {
        this.allEpisodes.add(new EpisodeSelectedTracker(episode));
        if (passesFilter(episode, this.currentFilter.toString().toLowerCase())){
            this.displayedEpisodes.add(this.allEpisodes.size()-1);
        }
    }

    public Episode remove(int position) {
        return this.allEpisodes.remove(position).episode;
    }

    public ArrayList<Episode> getSelectedEpisodes(boolean deselectEpisodes) {
        ArrayList<Episode> episodes = new ArrayList<>();
        for (int i = allEpisodes.size()-1; i >= 0 ; i--) {
            EpisodeSelectedTracker tracker = allEpisodes.get(i);
            if (tracker.selected) {
                episodes.add(tracker.episode);
                if (deselectEpisodes) {
                    tracker.selected = false;
                }
            }
        }
        return episodes;
    }

    public void moveEpisodes(List<Integer> startPositions, int endPosition) {
        int adjuster = 0;
        int currentEpisodeNumber = storage.fast.getCurrentEpisodeNumber();
        for (int startPosition : startPositions) {
            int adjustedPosition = startPosition;
            if (currentEpisodeNumber < startPosition) {
                adjustedPosition += adjuster++;
            }
            EpisodeSelectedTracker tracker = this.allEpisodes.remove(adjustedPosition);
            if (startPosition < endPosition) {
                endPosition--;
            }
            tracker.selected = false;
            this.allEpisodes.add(endPosition, tracker);
        }
    }

    public void resetNumberSelected() {
        numberEpisodesSelected = 0;
        for (EpisodeSelectCountListener listener : selectCountListeners){
            listener.countChanged(numberEpisodesSelected);
        }
    }

    public ArrayList<Integer> getSelectedTruePositions() {
        final ArrayList<Integer> positions = new ArrayList<>();
        for (int i = allEpisodes.size() - 1; i >= 0; i--) {
            if (this.allEpisodes.get(i).selected) {
                positions.add(i);
            }
        }
        return positions;
    }

    public Episode getItemAtTruePosition(int position) {
        return this.allEpisodes.get(position).episode;
    }

    @Override
    public Episode getItem(int position) {
        return this.allEpisodes.get(this.displayedEpisodes.get(position)).episode;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return this.displayedEpisodes.size();
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int realPosition = displayedEpisodes.get(position);
        EpisodeSelectedTracker tracker = allEpisodes.get(realPosition);
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.episodes_layout, null);

            tracker.holder = new ViewHolder();
            tracker.holder.episodeSelectClick = convertView.findViewById(R.id.episodeSelectClick);
            tracker.holder.episodeLabel = (TextView) convertView.findViewById(R.id.episodeLabel);
            tracker.holder.episodeDate = (TextView) convertView.findViewById(R.id.episodeDate);
            tracker.holder.episodeDuration = (TextView) convertView.findViewById(R.id.episodeDuration);
            if (!showDuration) {
                tracker.holder.episodeDuration.setHeight(0);
            }
            tracker.holder.episodePosition = (TextView) convertView.findViewById(R.id.episodeNumber);
            tracker.holder.feedName = (TextView) convertView.findViewById(R.id.feedName);
            tracker.holder.checkbox = (CheckBox) convertView.findViewById(R.id.episodeCheckbox);
            tracker.holder.episodeSelectClick.setOnClickListener(new EpisodeSelectClickListener(tracker.holder));
            convertView.setTag(tracker.holder);
        } else {
            tracker.holder = (ViewHolder) convertView.getTag();
        }
        if (!disableExistingEpisodes) {
            if (highlightCurrentEpisode) {
                boolean isCurrentEpisode = this.storage.fast.getCurrentEpisodeNumber() == realPosition;
                tracker.holder.checkbox.setEnabled(!isCurrentEpisode);
                convertView.setEnabled(!isCurrentEpisode);
                if (isCurrentEpisode) {
                    convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.currentEpisodeInList));
                } else {
                    convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.defaultEpisodeInList));
                }
            }
        }
        tracker.holder.episodeLabel.setText(Html.fromHtml(tracker.episode.getTitle()));
        tracker.holder.episodeDate.setText(String.format(context.getString(R.string.listPubDate), Utils.dateStringFromLong(tracker.episode.getPublishDate())));
        tracker.holder.episodeDuration.setText(String.format(context.getString(R.string.listDuration), Utils.formatTime(tracker.episode.getAudioDuration())));
        tracker.holder.feedName.setText(Html.fromHtml(tracker.episode.getFeedTitle()));
        tracker.holder.position = realPosition;
        tracker.holder.checkbox.setChecked(tracker.selected);
        tracker.holder.episodePosition.setText(String.format("#%d", realPosition + 1));
        if (disableExistingEpisodes) {
            PopulateHolderTask populate = new PopulateHolderTask(convertView, tracker);
            populate.execute();
        }

        return convertView;
    }

    private class PopulateHolderTask extends AsyncTask<Void, Void, Void> {

        private final View convertView;
        private final EpisodeSelectedTracker tracker;
        private int episodeNumber;
        public PopulateHolderTask(View convertView, EpisodeSelectedTracker tracker) {
            this.convertView = convertView;
            this.tracker = tracker;
        }
        @Override
        protected Void doInBackground(Void... notUsed) {
            episodeNumber = storage.getEpisodeNumber(tracker.episode);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (episodeNumber >= 0) {
                tracker.holder.checkbox.setEnabled(false);
                convertView.setEnabled(false);
                convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.downloadedEpisodeInList));
            } else {
                tracker.holder.checkbox.setEnabled(true);
                convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.defaultEpisodeInList));
            }
        }
    }

    private boolean passesFilter(Episode episode, String filter) {
        String dataNames;
        if (useFeedTitle) {
            dataNames = episode.getFeedTitle();
        } else {
            dataNames = episode.getTitle();
        }
        return dataNames.toLowerCase().contains(filter);
    }

    @Override
    public android.widget.Filter getFilter() {
        return new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayedEpisodes.clear();
                if (results.values != null) {
                    displayedEpisodes.addAll((List<Integer>)results.values);
                }
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                List<Integer> FilteredArray = new ArrayList<>();

                String filter = constraint.toString().toLowerCase();
                int counter = 0;

                for (EpisodeSelectedTracker episode : allEpisodes) {
                    if (passesFilter(episode.episode, filter)){
                        FilteredArray.add(counter);
                    }
                    counter++;
                }

                results.count = FilteredArray.size();
                results.values = FilteredArray;

                return results;
            }
        };
    }

    private static class ViewHolder {
        TextView episodeLabel;
        TextView feedName;
        TextView episodeDate;
        TextView episodeDuration;
        TextView episodePosition;
        CheckBox checkbox;
        View episodeSelectClick;
        int position;
    }

    private static class EpisodeSelectedTracker {
        final Episode episode;
        boolean selected;
        public ViewHolder holder;

        public EpisodeSelectedTracker(Episode episode) {
            this.episode = episode;
            this.selected = false;
        }
    }

    private class EpisodeSelectClickListener implements View.OnClickListener {

        private final ViewHolder holder;

        public EpisodeSelectClickListener(ViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            if (!holder.checkbox.isEnabled()) {
                return;
            }
            if (holder.checkbox.isChecked()) {
                numberEpisodesSelected++;
            } else {
                numberEpisodesSelected--;
            }
            for (EpisodeSelectCountListener listener : selectCountListeners){
                listener.countChanged(numberEpisodesSelected);
            }
            holder.checkbox.setChecked(!holder.checkbox.isChecked());
            allEpisodes.get(holder.position).selected = holder.checkbox.isChecked();
        }
    }

    /**
     * called whenever list item is selected or deselected
     */
    interface EpisodeSelectCountListener {
        void countChanged(int count);
    }
}
