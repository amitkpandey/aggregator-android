package com.tughi.aggregator.content;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;

/**
 * Utility methods for Aggregator {@link Uri}s.
 */
public class Uris {

    public static final Uri BASE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(DatabaseContentProvider.AUTHORITY)
            .build();

    private static final Uri SYNC_BASE_URI = Uri.withAppendedPath(BASE_URI, "sync");
    private static final Uri USER_BASE_URI = Uri.withAppendedPath(BASE_URI, "user");

    public static final String CALL_COMMIT_ENTRIES_READ_STATE = "commit_entries_read_state";

    /**
     * Creates an {@link Uri} for all feeds.
     */
    public static Uri newFeedsUri() {
        return Uri.withAppendedPath(BASE_URI, "feeds");
    }

    /**
     * Creates a feed {@link Uri}.
     */
    public static Uri newFeedUri(long feedId) {
        return ContentUris.withAppendedId(newFeedsUri(), feedId);
    }

    /**
     * Creates an {@link Uri} that is meant for updates only.
     */
    public static Uri newUserFeedsUri() {
        return Uri.withAppendedPath(USER_BASE_URI, "feeds");
    }

    /**
     * Creates an {@link Uri} that is meant for updates only.
     */
    public static Uri newUserFeedUri(long feedId) {
        return ContentUris.withAppendedId(newUserFeedsUri(), feedId);
    }

    /**
     * Creates an {@link Uri} for a feed's entries.
     */
    public static Uri newFeedEntriesUri(long feedId) {
        return Uri.withAppendedPath(newFeedUri(feedId), "entries");
    }

    /**
     * Creates an {@link Uri} that is meant for update only.
     */
    public static Uri newUserEntriesUri() {
        return Uri.withAppendedPath(USER_BASE_URI, "entries");
    }

    /**
     * Creates an {@link Uri} that is meant for update only.
     */
    public static Uri newUserEntryUri(long entryId) {
        return ContentUris.withAppendedId(newUserEntriesUri(), entryId);
    }

    /**
     * Creates the base {@link Uri} for sync feeds.
     */
    public static Uri newSyncFeedsUri() {
        return Uri.withAppendedPath(SYNC_BASE_URI, "feeds");
    }

    /**
     * Creates an {@link Uri} that is meant for insert, updates and delete only.
     */
    public static Uri newSyncFeedUri(long feedId) {
        return ContentUris.withAppendedId(newSyncFeedsUri(), feedId);
    }

    /**
     * Creates an {@link Uri} that is meant for query only.
     */
    public static Uri newFeedsSyncLogUri() {
        return Uri.withAppendedPath(newSyncFeedsUri(), "log");
    }

    /**
     * Creates an {@link Uri} that is meant for query only.
     */
    public static Uri newFeedSyncLogUri(long feedId) {
        return Uri.withAppendedPath(newSyncFeedUri(feedId), "log");
    }

    /**
     * Creates an {@link Uri} that is meant for query only.
     */
    public static Uri newFeedSyncLogStatsUri(long feedId) {
        return Uri.withAppendedPath(newFeedSyncLogUri(feedId), "stats");
    }

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int MATCHED_FEEDS_URI = 1;
    public static final int MATCHED_FEED_URI = 2;
    public static final int MATCHED_FEED_ENTRIES_URI = 3;
    public static final int MATCHED_SYNC_FEEDS_URI = 4;
    public static final int MATCHED_SYNC_FEED_URI = 5;
    public static final int MATCHED_FEEDS_SYNC_LOG_URI = 6;
    public static final int MATCHED_FEED_SYNC_LOG_URI = 7;
    public static final int MATCHED_FEED_SYNC_LOG_STATS_URI = 8;
    public static final int MATCHED_USER_FEEDS_URI = 9;
    public static final int MATCHED_USER_FEED_URI = 10;
    public static final int MATCHED_USER_ENTRIES_URI = 11;
    public static final int MATCHED_USER_ENTRY_URI = 12;

    static {
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds", MATCHED_FEEDS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds", MATCHED_SYNC_FEEDS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/#", MATCHED_SYNC_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/log", MATCHED_FEEDS_SYNC_LOG_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/-1/log", MATCHED_FEEDS_SYNC_LOG_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/-2/log", MATCHED_FEEDS_SYNC_LOG_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/#/log", MATCHED_FEED_SYNC_LOG_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "sync/feeds/#/log/stats", MATCHED_FEED_SYNC_LOG_STATS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "user/feeds", MATCHED_USER_FEEDS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "user/feeds/#", MATCHED_USER_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "user/entries", MATCHED_USER_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "user/entries/#", MATCHED_USER_ENTRY_URI);
    }

    /**
     * Tries to match an Aggregator {@link Uri}.
     */
    public static int match(Uri uri) {
        return URI_MATCHER.match(uri);
    }

}
