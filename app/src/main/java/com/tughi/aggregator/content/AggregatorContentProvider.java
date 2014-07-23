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
public class AggregatorContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.tughi.aggregator";

    private SQLiteOpenHelper helper;

    private static final String TABLE_FEED = "feed";
    private static final String TABLE_ENTRY = "entry";

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
        switch (AggregatorUris.match(uri)) {
            case AggregatorUris.MATCHED_ENTRIES_URI:
                return queryEntries(uri, projection, selection, selectionArgs, orderBy);
        }
        throw new UnsupportedOperationException(uri.toString());
    }

    private Cursor queryEntries(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.query(TABLE_ENTRY, projection, selection, selectionArgs, null, null, orderBy);
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
