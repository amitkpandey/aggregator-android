package com.tughi.aggregator.content;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;

/**
 * Utility methods for Aggregator {@link Uri}s.
 */
public class AggregatorUris {

    private static final Uri BASE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AggregatorContentProvider.AUTHORITY)
            .build();

    /**
     * Creates an {@link Uri} for all entries.
     */
    public static Uri newEntriesUri() {
        return Uri.withAppendedPath(BASE_URI, "entries");
    }

    /**
     * Creates an {@link Uri} for all feeds.
     */
    public static Uri newFeedsUri() {
        return Uri.withAppendedPath(BASE_URI, "feed");
    }

    /**
     * Creates an {@link Uri} for a feed's entries.
     */
    public static Uri newFeedEntriesUri(long feedId) {
        return Uri.withAppendedPath(BASE_URI, "feed/" + feedId + "/entries");
    }

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int MATCHED_FEEDS_URI = 1;
    public static final int MATCHED_FEED_ENTRIES_URI = 2;
    public static final int MATCHED_ENTRIES_URI = 3;

    static {
        URI_MATCHER.addURI(AggregatorContentProvider.AUTHORITY, "feed", MATCHED_FEEDS_URI);
        URI_MATCHER.addURI(AggregatorContentProvider.AUTHORITY, "feed/#/entries", MATCHED_FEED_ENTRIES_URI);
        URI_MATCHER.addURI(AggregatorContentProvider.AUTHORITY, "entries", MATCHED_ENTRIES_URI);
    }

    /**
     * Tries to match an Aggregator {@link Uri}.
     */
    public static int match(Uri uri) {
        return URI_MATCHER.match(uri);
    }

}
