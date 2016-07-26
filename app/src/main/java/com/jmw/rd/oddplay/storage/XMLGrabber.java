package com.jmw.rd.oddplay.storage;

import com.jmw.rd.oddplay.HttpConnection;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.thebuzzmedia.sjxp.XMLParser;
import com.thebuzzmedia.sjxp.rule.DefaultRule;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;

public class XMLGrabber {

    public XMLGrabber() {
    }

    private class EpisodeHolder {
        Episode episode;
    }

    /**
     *
     * @param feedUrl Feed's rss url
     * @param feedTitle Feed's title
     * @param lastDatePulled long representing date of last time feed server was contacted
     * @param maxNumberToGrab parser will stop after hitting this number
     * @param function use this function to collect episodes
     * @throws IOException
     */
    public void passEpisodesIntoFunction(String feedUrl, String feedTitle, long lastDatePulled,
                                    long maxNumberToGrab, CallEpisodeFunction function)
            throws IOException {
        @SuppressWarnings("unchecked")
        XMLParser<EpisodeHolder> parser = new XMLParser<>(
                new EpisodeTagOpenRule(feedUrl, feedTitle, maxNumberToGrab, function),
                new EpisodeTitleRule(),
                new EpisodeDateRule(lastDatePulled),
                new EpisodeDescriptionRule(),
                new EpisodeEnclosureRule()
        );
        try (HttpConnection connection = new HttpConnection(feedUrl);
             InputStream is = connection.getInputStream()){
            parser.parse(is, new EpisodeHolder());
        }
    }

    public Feed getFeed(String feedUrl) throws IOException {
        @SuppressWarnings("unchecked")
        XMLParser<Feed> parser = new XMLParser<>(
                new FeedTitleRule(),
                new FeedDescriptionRule(),
                new FeedCopyrightRule(),
                new FeedLanguageRule(),
                new FeedImageRule(),
                new FeedITunesImageRule());

        try (HttpConnection connection = new HttpConnection(feedUrl);
              InputStream is = connection.getInputStream()){
            Feed feed = new Feed(feedUrl);
            parser.parse(is, feed);
            return feed;
        }
    }

    /**
     * This interface receives callbacks about completed episodes
     */
    public interface CallEpisodeFunction {
        void call(Episode episode);
        EmergencyDownloadStopException doIStop();
    }

    static class EpisodeTitleRule extends DefaultRule<EpisodeHolder> {
        public EpisodeTitleRule() {
            super(Type.CHARACTER, "/rss/channel/item/title");
        }

        @Override
        public void handleParsedCharacters(XMLParser<EpisodeHolder> parser, String title, EpisodeHolder holder) {
            holder.episode.setTitle(title);
        }
    }

    static class EpisodeDateRule extends DefaultRule<EpisodeHolder> {
        static final DateTimeFormatter fmt = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z");
        static final DateTimeFormatter fmt2 = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
        private final long lastPubDate;

        public EpisodeDateRule(long lastPubDate) {
            super(Type.CHARACTER, "/rss/channel/item/pubDate");
            this.lastPubDate = lastPubDate;
        }

        @Override
        public void handleParsedCharacters(XMLParser<EpisodeHolder> parser, String date, EpisodeHolder holder) {
            try {
                long pubDate = fmt.parseDateTime(date).getMillis();
                if (pubDate <= this.lastPubDate) {
                    parser.stop();
                } else {
                    holder.episode.setPublishDate(pubDate);
                }
            } catch (IllegalArgumentException e) {
                long pubDate = fmt2.parseDateTime(date).getMillis();
                if (pubDate <= this.lastPubDate) {
                    parser.stop();
                } else {
                    holder.episode.setPublishDate(pubDate);
                }
            }
        }
    }

    static class EpisodeDescriptionRule extends DefaultRule<EpisodeHolder> {
        public EpisodeDescriptionRule() {
            super(Type.CHARACTER, "/rss/channel/item/description");
        }

        @Override
        public void handleParsedCharacters(XMLParser<EpisodeHolder> parser, String title, EpisodeHolder holder) {
            holder.episode.setDescription(title);
        }
    }

    static class EpisodeEnclosureRule extends DefaultRule<EpisodeHolder> {
        public EpisodeEnclosureRule() {
            super(Type.ATTRIBUTE, "/rss/channel/item/enclosure", "url", "length", "type");
        }

        @Override
        public void handleParsedAttribute(XMLParser<EpisodeHolder> parser, int index, String value, EpisodeHolder holder) {
            switch (index) {
                case 0:
                    holder.episode.setAudioUrl(value);
                    break;
                case 1:
                    try {
                        holder.episode.setAudioSize(Long.parseLong(value));
                    } catch (NumberFormatException nfe) {
                        // pass
                    }
                    break;
                case 2:
                    holder.episode.setAudioType(value);
                    break;
            }
        }
    }

    static class FeedTitleRule extends DefaultRule<Feed> {
        public FeedTitleRule() {
            super(Type.CHARACTER, "/rss/channel/title");
        }

        @Override
        public void handleParsedCharacters(XMLParser<Feed> parser, String title, Feed feed) {
            feed.setTitle(title);
        }
    }

    static class FeedDescriptionRule extends DefaultRule<Feed> {
        public FeedDescriptionRule() {
            super(Type.CHARACTER, "/rss/channel/description");
        }

        @Override
        public void handleParsedCharacters(XMLParser<Feed> parser, String desc, Feed feed) {
            feed.setDescription(desc);
        }
    }

    static class FeedCopyrightRule extends DefaultRule<Feed> {
        public FeedCopyrightRule() {
            super(Type.CHARACTER, "/rss/channel/copyright");
        }

        @Override
        public void handleParsedCharacters(XMLParser<Feed> parser, String copyright, Feed feed) {
            feed.setCopyright(copyright);
        }
    }

    static class FeedLanguageRule extends DefaultRule<Feed> {
        public FeedLanguageRule() {
            super(Type.CHARACTER, "/rss/channel/language");
        }

        @Override
        public void handleParsedCharacters(XMLParser<Feed> parser, String language, Feed feed) {
            feed.setLanguage(language);
        }
    }

    static class FeedImageRule extends DefaultRule<Feed> {
        public FeedImageRule() {
            super(Type.CHARACTER, "/rss/channel/image/url");
        }

        @Override
        public void handleParsedCharacters(XMLParser<Feed> parser, String image, Feed feed) {
            if (!image.equals("") && (feed.getImageurl() == null || feed.getImageurl().equals(""))) {
                feed.setImageurl(image);
            }
        }
    }

    static class FeedITunesImageRule extends DefaultRule<Feed> {
        public FeedITunesImageRule() {
            super(Type.ATTRIBUTE, "/rss/channel/[http://www.itunes.com/dtds/podcast-1.0.dtd]image", "href");
        }

        @Override
        public void handleParsedAttribute(XMLParser<Feed> parser, int index, String image, Feed feed) {
            if (!image.equals("") && (feed.getImageurl() == null || feed.getImageurl().equals(""))) {
                feed.setImageurl(image);
            }
        }
    }

    class EpisodeTagOpenRule extends DefaultRule<EpisodeHolder> {
        private final String feedUrl;
        private final String feedTitle;
        private final long maxNumberToGrab;
        private int currentCount;
        private final CallEpisodeFunction callFunction;

        /**
         *
         * @param feedUrl URL of feed
         * @param feedTitle title of feed
         * @param maxNumberToGrab maximum number of episodes to get from this feed
         * @param callFunction if implemented, will only return episodes through this function
         */
        public EpisodeTagOpenRule(String feedUrl, String feedTitle, long maxNumberToGrab, CallEpisodeFunction callFunction) {
            super(Type.TAG, "/rss/channel/item");
            this.feedUrl = feedUrl;
            this.feedTitle = feedTitle;
            this.maxNumberToGrab = maxNumberToGrab;
            this.currentCount = 0;
            this.callFunction = callFunction;
        }

        @Override
        public void handleTag(XMLParser<EpisodeHolder> parser, boolean isStartTag, EpisodeHolder holder) {
            if (callFunction.doIStop() != null) {
                parser.stop();
                return;
            }
            if (isStartTag) {
                if (++currentCount > maxNumberToGrab && maxNumberToGrab > 0) {
                    parser.stop();
                } else {
                    holder.episode = new Episode(this.feedUrl, this.feedTitle);
                }
            } else {
                this.callFunction.call(holder.episode);
            }
        }
    }
}
