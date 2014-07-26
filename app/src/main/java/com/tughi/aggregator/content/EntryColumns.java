package com.tughi.aggregator.content;

/**
 * Defines the columns of the entry view.
 */
public interface EntryColumns {

    public static final String ID = "_id";

    public static final String FEED_ID = "feed_id";

    public static final String GUID = "guid";

    public static final String TITLE = "title";

    public static final String POLL = "poll";

    public static final String UPDATED = "updated";

    public static final String DATA = "data";

    /**
     * Boolean flag that marks an entry as read.
     */
    public static final String FLAG_READ = "flag_read";

    /**
     * Boolean flag that marks an entry as starred.
     */
    public static final String FLAG_STAR = "flag_star";

}
