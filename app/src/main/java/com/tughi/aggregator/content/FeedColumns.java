package com.tughi.aggregator.content;

/**
 * Defines the columns of the feed view.
 */
public interface FeedColumns {

    public static final String FEED_ID = "_id";

    public static final String FEED_TITLE = "title";

    /**
     * A synthetic column of the feed view that contains the count of unread entries of each feed.
     * With one exception: the <i>starred</i> feed's <i>unread_count</i> contains the count of all starred entries.
     */
    public static final String FEED_UNREAD_COUNT = "unread_count";

}
