package com.tughi.aggregator.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.tughi.android.database.sqlite.DatabaseOpenHelper;

/**
 * A {@link ContentProvider} that stores the aggregated feeds.
 */
public class DatabaseContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.tughi.aggregator";

    private SQLiteOpenHelper helper;

    private static final String TABLE_FEED = "feed";
    private static final String TABLE_ENTRY_SYNC = "entry_sync";
    private static final String TABLE_ENTRY_USER = "entry_user";

    private static final String VIEW_FEED = "feed_view";
    private static final String VIEW_ENTRY = "entry_view";

    @Override
    public boolean onCreate() {
        helper = new DatabaseOpenHelper(getContext(), "content.db", 1);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException(uri.toString());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        String feedId;
        switch (Uris.match(uri)) {
            case Uris.MATCHED_FEED_URI:
                feedId = uri.getPathSegments().get(1);
                if (selection == null) {
                    selection = "_id = " + feedId;
                } else {
                    selection = "_id = " + feedId + " AND (" + selection + ")";
                }
            case Uris.MATCHED_FEEDS_URI:
                return queryFeeds(uri, projection, selection, selectionArgs, orderBy);
            case Uris.MATCHED_FEED_ENTRIES_URI:
                feedId = uri.getPathSegments().get(1);
                if (selection == null) {
                    selection = "feed_id = " + feedId;
                } else {
                    selection = "feed_id = " + feedId + " AND (" + selection + ")";
                }
                return queryEntries(uri, projection, selection, selectionArgs, orderBy);
        }
        throw new UnsupportedOperationException(uri.toString());
    }

    private Cursor queryFeeds(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.query(VIEW_FEED, projection, selection, selectionArgs, null, null, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private Cursor queryEntries(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.query(VIEW_ENTRY, projection, selection, selectionArgs, null, null, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException(uri.toString());
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException(uri.toString());
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException(uri.toString());
    }

}
