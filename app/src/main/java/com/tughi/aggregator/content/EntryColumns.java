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

    /**
     * Boolean flag that marks an entry as junk.
     */
    public static final String FLAG_JUNK = "flag_junk";

    /**
     * Read-only boolean flag that contains the original value of {@link #FLAG_READ}.
     */
    public static final String RO_FLAG_READ = "ro_flag_read";

    /**
     * The feed's title from the feed table.
     */
    public static final String FEED_TITLE = "feed_title";

    /**
     * The feed's favicon from the feed table.
     */
    public static final String FEED_FAVICON = "feed_favicon";

}
