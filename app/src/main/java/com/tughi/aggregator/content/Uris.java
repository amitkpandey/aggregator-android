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

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int MATCHED_FEEDS_URI = 1;
    public static final int MATCHED_FEED_URI = 2;
    public static final int MATCHED_FEED_ENTRIES_URI = 3;
    public static final int MATCHED_USER_ENTRIES_URI = 4;
    public static final int MATCHED_USER_ENTRY_URI = 5;

    static {
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds", MATCHED_FEEDS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#/entries", MATCHED_FEED_ENTRIES_URI);
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
