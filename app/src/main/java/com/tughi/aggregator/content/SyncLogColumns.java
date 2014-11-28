package com.tughi.aggregator.content;

/**
 * Defines the columns of the sync_log view.
 */
public interface SyncLogColumns {

    public static final String FEED_ID = "feed_id";

    public static final String POLL = "poll";

    public static final String POLL_DELTA = "poll_delta";

    public static final String ERROR = "error";

    public static final String ENTRIES_TOTAL = "entries_total";

    public static final String ENTRIES_NEW = "entries_new";

}
