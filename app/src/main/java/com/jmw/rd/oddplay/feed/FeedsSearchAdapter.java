package com.jmw.rd.oddplay.feed;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jmw.rd.oddplay.R;

import java.util.ArrayList;
import java.util.List;

class FeedsSearchAdapter extends BaseAdapter {

    private final Context context;
    private final List<FeedSelectedTracker> items;
    private int numberFeedsSelected = 0;
    private final List<FeedsSelectCountListener> selectCountListeners;

    FeedsSearchAdapter(Context context) {
        this.context = context;
        items = new ArrayList<>();
        selectCountListeners = new ArrayList<>();
    }

    void addAll(List<FeedSearchItem> newItems) {
        for (FeedSearchItem item : newItems) {
            items.add(new FeedSelectedTracker(item));
        }
    }

    List<FeedSearchItem> getSelectedFeeds() {
        final List<FeedSearchItem> feeds = new ArrayList<>();
        for (FeedSelectedTracker item : items) {
            if (item.selected) {
                feeds.add(item.feed);
            }
        }
        return feeds;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            if (vi == null) {
                throw new RuntimeException("Could not get layout inflater.  Something is wrong with your phone");
            }
            convertView = vi.inflate(R.layout.feeds_search_layout, viewGroup, false);
            holder = new ViewHolder();
            holder.feedSearchSelector = convertView.findViewById(R.id.feedSearchSelectClick);
            holder.feedTitle = (TextView) convertView.findViewById(R.id.searchFeedTitle);
            holder.feedUrl = (TextView) convertView.findViewById(R.id.searchFeedUrl);
            holder.feedDescription = (TextView) convertView.findViewById(R.id.searchFeedDescription);
            holder.checkbox = (CheckBox) convertView.findViewById(R.id.searchFeedCheckbox);
            holder.feedSearchSelector.setOnClickListener(new FeedSelectClickListener(holder));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FeedSelectedTracker tracker = items.get(position);
        holder.position = position;
        holder.feedTitle.setText(Html.fromHtml(tracker.feed.getTitle()));
        holder.feedUrl.setText((tracker.feed.getUrl()));
        holder.feedDescription.setText(Html.fromHtml(tracker.feed.getDescription()));
        holder.checkbox.setChecked(tracker.selected);
        return convertView;
    }

    private static class ViewHolder {
        TextView feedTitle;
        TextView feedUrl;
        TextView feedDescription;
        CheckBox checkbox;
        View feedSearchSelector;
        int position;
    }

    private static class FeedSelectedTracker {
        final FeedSearchItem feed;
        boolean selected;

        private FeedSelectedTracker(FeedSearchItem feed) {
            this.feed = feed;
            this.selected = false;
        }
    }


    private class FeedSelectClickListener implements View.OnClickListener {

        private final ViewHolder holder;

        private FeedSelectClickListener(ViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            holder.checkbox.setChecked(!holder.checkbox.isChecked());
            items.get(holder.position).selected = holder.checkbox.isChecked();

            if (holder.checkbox.isChecked()) {
                numberFeedsSelected++;
            } else {
                numberFeedsSelected--;
            }
            for (FeedsSelectCountListener listener : selectCountListeners){
                listener.countChanged(numberFeedsSelected);
            }
        }
    }

    void addSelectCountListener(FeedsSelectCountListener listener) {
        this.selectCountListeners.add(listener);
    }

    void removeSelectCountListener(FeedsSelectCountListener listener) {
        this.selectCountListeners.remove(listener);
    }

    /**
     * called whenever list item is selected or deselected
     */
    interface FeedsSelectCountListener {
        void countChanged(int count);
    }


}
