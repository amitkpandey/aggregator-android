package com.tughi.aggregator.content;

/**
 * Defines the columns of the feed sync stats query.
 */
public interface FeedSyncStatsColumns {

    public static final String LAST_POLL = "last_poll";

    public static final String LAST_ENTRIES_TOTAL = "last_entries_total";

    public static final String LAST_ENTRIES_NEW = "last_entries_new";

    public static final String POLL_COUNT = "poll_count";

    public static final String POLL_DELTA_AVERAGE = "poll_delta_average";

    public static final String ENTRIES_NEW_AVERAGE = "entries_new_average";

    public static final String ENTRIES_NEW_MEDIAN = "entries_new_median";

}
