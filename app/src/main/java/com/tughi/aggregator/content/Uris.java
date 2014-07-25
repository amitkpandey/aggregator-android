package com.tughi.aggregator.content;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;

/**
 * Utility methods for Aggregator {@link Uri}s.
 */
public class Uris {

    private static final Uri BASE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(DatabaseContentProvider.AUTHORITY)
            .build();

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

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int MATCHED_FEEDS_URI = 1;
    public static final int MATCHED_FEED_URI = 2;
    public static final int MATCHED_FEED_ENTRIES_URI = 3;

    static {
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds", MATCHED_FEEDS_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#", MATCHED_FEED_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-1/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/-2/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(DatabaseContentProvider.AUTHORITY, "feeds/#/entries", MATCHED_FEED_ENTRIES_URI);
    }

    /**
     * Tries to match an Aggregator {@link Uri}.
     */
    public static int match(Uri uri) {
        return URI_MATCHER.match(uri);
    }

}
