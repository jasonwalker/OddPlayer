package com.jmw.rd.oddplay.feed;

import com.jmw.rd.oddplay.HttpConnection;
import com.thebuzzmedia.sjxp.XMLParser;
import com.thebuzzmedia.sjxp.rule.DefaultRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeedsSearchDigitalPodcast implements FeedsSearch {

    private static final String address = "http://api.digitalpodcast.com/v2r/search/?appid=%s&results=10&sort=rel&format=rss&keywords=%s&start=%d&results=%d";
    private static final String appId = "";//go to http://api.digitalpodcast.com/get_appid.html to get an application ID


    private String searchString;

    private static String getUrl(String searchString, int offset, int amount) {
        return String.format(Locale.US, address, appId, searchString, offset, amount);
    }

    public List<FeedSearchItem> searchFeeds(String searchString) throws IOException {
        this.searchString = searchString;
        return search(0, 50);
    }

    private List<FeedSearchItem> search(int offset, int amount) throws IOException {
        @SuppressWarnings("unchecked")
        XMLParser<List<FeedSearchItem>> parser = new XMLParser<>(
                new SearchFeedTagOpenRule(),
                new SearchFeedUrlRule(),
                new SearchFeedTitleRule(),
                new SearchFeedDescriptionRule());
        List<FeedSearchItem> feedList = new ArrayList<>();

        try (HttpConnection connection = new HttpConnection(getUrl(searchString, offset, amount));
             InputStream is = connection.getInputStream()){
            parser.parse(is, feedList);
            return feedList;
        }
    }


    private static class SearchFeedUrlRule extends DefaultRule<List<FeedSearchItem>> {
        SearchFeedUrlRule() {
            super(Type.CHARACTER, "/rss/channel/item/source");
        }

        @Override
        public void handleParsedCharacters(XMLParser<List<FeedSearchItem>> parser, String url, List<FeedSearchItem> feed) {
            feed.get(feed.size()-1).setUrl(url);
        }
    }

    private static class SearchFeedTitleRule extends DefaultRule<List<FeedSearchItem> > {
        SearchFeedTitleRule() {
            super(Type.CHARACTER, "/rss/channel/item/title");
        }

        @Override
        public void handleParsedCharacters(XMLParser<List<FeedSearchItem> > parser, String title, List<FeedSearchItem>  feed) {
            feed.get(feed.size()-1).setTitle(title);
        }
    }

    private static class SearchFeedDescriptionRule extends DefaultRule<List<FeedSearchItem> > {
        SearchFeedDescriptionRule() {
            super(Type.CHARACTER, "/rss/channel/item/description");
        }

        @Override
        public void handleParsedCharacters(XMLParser<List<FeedSearchItem> > parser, String description, List<FeedSearchItem>  feed) {
            feed.get(feed.size()-1).setDescription(description);
        }
    }

    private class SearchFeedTagOpenRule extends DefaultRule<List<FeedSearchItem>> {
        SearchFeedTagOpenRule() {
            super(Type.TAG, "/rss/channel/item");
        }

        @Override
        public void handleTag(XMLParser<List<FeedSearchItem>> parser, boolean isStartTag, List<FeedSearchItem> holder) {
            if (isStartTag) {
                holder.add(new FeedSearchItem());
            }
        }
    }
}
