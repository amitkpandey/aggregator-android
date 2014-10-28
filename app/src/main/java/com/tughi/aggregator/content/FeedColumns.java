package com.tughi.aggregator.content;

/**
 * Defines the columns of the feed view.
 */
public interface FeedColumns {

    public static final String ID = "_id";

    public static final String URL = "url";

    public static final String TITLE = "title";

    /**
     * Contains the home of the feed as URL string.
     */
    public static final String LINK = "link";

    /**
     * Stores the count of entries found in the feed XML.
     */
    public static final String ENTRY_COUNT = "entry_count";

    /**
     * Specifies the feed update mode.
     * See {@link FeedUpdateModes} for allowed values.
     */
    public static final String UPDATE_MODE = "update_mode";

    /**
     * Specifies next scheduled feed update as timestamp.
     */
    public static final String NEXT_SYNC = "next_sync";

    /**
     * A synthetic column of the feed view that contains the count of unread entries of each feed.
     * With one exception: the <i>starred</i> feed's <i>unread_count</i> contains the count of all starred entries.
     */
    public static final String UNREAD_COUNT = "unread_count";

}
