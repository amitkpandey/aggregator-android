package com.tughi.aggregator.content;

/**
 * Defines the columns of the feed view.
 */
public interface FeedColumns {

    public static final String ID = "_id";

    public static final String URL = "url";

    public static final String TITLE = "title";

    /**
     * Specifies when should a new update be executed.
     */
    public static final String NEXT_POLL = "next_poll";

    /**
     * A synthetic column of the feed view that contains the count of unread entries of each feed.
     * With one exception: the <i>starred</i> feed's <i>unread_count</i> contains the count of all starred entries.
     */
    public static final String UNREAD_COUNT = "unread_count";

}
