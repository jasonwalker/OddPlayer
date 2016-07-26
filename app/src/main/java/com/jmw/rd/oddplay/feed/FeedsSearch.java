package com.jmw.rd.oddplay.feed;


import java.io.IOException;
import java.util.List;

interface FeedsSearch {

    List<FeedSearchItem> searchFeeds(String keywords) throws IOException;

}
