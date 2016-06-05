package com.tughi.aggregator.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.FeedSyncStatsColumns;
import com.tughi.aggregator.content.Uris;

/**
 * A {@link Fragment} for feed entries.
 * The displayed entries depend on the provided entries {@link Uri}.
 */
public class EntryListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The entries {@link Uri}
     */
    public static final String ARG_ENTRIES_URI = "uri";

    private static final int LOADER_FEED = 1;
    private static final int LOADER_ENTRIES = 2;

    private Context applicationContext;

    private Uri entriesUri;
    private Uri feedUri;
    private long feedId;

    private EntryListAdapter adapter;

    private RecyclerView entriesRecyclerView;
    private TextView unreadCountTextView;
    private ProgressBar progressBar;
    private View emptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getActivity().getApplicationContext();

        entriesUri = getArguments().getParcelable(ARG_ENTRIES_URI);
        assert entriesUri != null;
        feedId = Long.parseLong(entriesUri.getPathSegments().get(1));
        feedUri = Uris.newFeedUri(feedId);

        adapter = new EntryListAdapter(applicationContext);

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(LOADER_FEED, null, this);
        loaderManager.initLoader(LOADER_ENTRIES, null, this);

        if (feedId > 0) {
            setHasOptionsMenu(true);
        }

        if (BuildConfig.DEBUG) {
            if (feedId > 0) {
                loaderManager.initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                    private final String[] FEED_SYNC_STATS_PROJECTION = {
                            FeedSyncStatsColumns.POLL_COUNT,
                            FeedSyncStatsColumns.LAST_POLL,
                            FeedSyncStatsColumns.LAST_ENTRIES_TOTAL,
                            FeedSyncStatsColumns.LAST_ENTRIES_NEW,
                            FeedSyncStatsColumns.POLL_DELTA_AVERAGE,
                            FeedSyncStatsColumns.ENTRIES_NEW_AVERAGE,
                            FeedSyncStatsColumns.ENTRIES_NEW_MEDIAN,
                    };
                    private final int FEED_SYNC_STATS_POLL_COUNT = 0;
                    private final int FEED_SYNC_STATS_LAST_POLL = 1;
                    private final int FEED_SYNC_STATS_LAST_ENTRIES_TOTAL = 2;
                    private final int FEED_SYNC_STATS_LAST_ENTRIES_NEW = 3;
                    private final int FEED_SYNC_STATS_POLL_DELTA_AVERAGE = 4;
                    private final int FEED_SYNC_STATS_ENTRIES_NEW_AVERAGE = 5;
                    private final int FEED_SYNC_STATS_ENTRIES_NEW_MEDIAN = 6;

                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        Uri feedSyncLogStatsUri = Uris.newFeedSyncLogStatsUri(feedId);
                        return new CursorLoader(applicationContext, feedSyncLogStatsUri, FEED_SYNC_STATS_PROJECTION, null, null, null);
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                        if (cursor.moveToFirst()) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_POLL_COUNT)).append(": ").append(cursor.getInt(FEED_SYNC_STATS_POLL_COUNT)).append("\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_LAST_POLL)).append(": ").append(String.format("%tc", cursor.getLong(FEED_SYNC_STATS_LAST_POLL))).append("\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_LAST_ENTRIES_TOTAL)).append(": ").append(cursor.getInt(FEED_SYNC_STATS_LAST_ENTRIES_TOTAL)).append("\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_LAST_ENTRIES_NEW)).append(": ").append(cursor.getInt(FEED_SYNC_STATS_LAST_ENTRIES_NEW)).append("\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_POLL_DELTA_AVERAGE)).append(": ").append(cursor.getLong(FEED_SYNC_STATS_POLL_DELTA_AVERAGE) / 3600000f).append("h\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_ENTRIES_NEW_AVERAGE)).append(": ").append(cursor.getFloat(FEED_SYNC_STATS_ENTRIES_NEW_AVERAGE)).append("\n");
                            builder.append(cursor.getColumnName(FEED_SYNC_STATS_ENTRIES_NEW_MEDIAN)).append(": ").append(cursor.getInt(FEED_SYNC_STATS_ENTRIES_NEW_MEDIAN));
                            Toast.makeText(applicationContext, builder, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {
                    }
                });
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entry_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        entriesRecyclerView = (RecyclerView) view.findViewById(R.id.entries);
        entriesRecyclerView.setAdapter(adapter);
        // TODO: register item touch listener

        unreadCountTextView = (TextView) view.findViewById(R.id.unread_count);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);
        emptyView = view.findViewById(R.id.empty);
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.updateSections();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry_list_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mode:
                startActivity(new Intent(applicationContext, FeedUpdateModeActivity.class).setData(feedUri));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String[] FEED_PROJECTION = {
            FeedColumns.ID,
            FeedColumns.TITLE,
            FeedColumns.UNREAD_COUNT,
    };
    private static final int FEED_ID = 0;
    private static final int FEED_TITLE = 1;
    private static final int FEED_UNREAD_COUNT = 2;

    private static final String ENTRY_SELECTION = EntryColumns.RO_FLAG_READ + " = 0";
    private static final String ENTRY_ORDER = EntryColumns.UPDATED + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_FEED:
                return new CursorLoader(applicationContext, feedUri, FEED_PROJECTION, null, null, null);
            case LOADER_ENTRIES:
                return new CursorLoader(applicationContext, entriesUri, EntryListAdapter.ENTRY_PROJECTION, ENTRY_SELECTION, null, ENTRY_ORDER);
        }

        // never happens
        throw new IllegalStateException();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_FEED:
                if (cursor.moveToFirst()) {
                    // update the activity title
                    String title;
                    switch (cursor.getInt(FEED_ID)) {
                        case -2:
                            title = getString(R.string.starred_feed);
                            break;
                        case -1:
                            title = getString(R.string.unread_feed);
                            break;
                        default:
                            title = cursor.getString(FEED_TITLE);
                    }
                    getActivity().setTitle(title);

                    // show the remaining unread count
                    int unreadCount = cursor.getInt(FEED_UNREAD_COUNT);
                    if (unreadCount > 0) {
                        unreadCountTextView.setVisibility(View.VISIBLE);
                        unreadCountTextView.setText(Integer.toString(unreadCount));
                    } else {
                        unreadCountTextView.setVisibility(View.GONE);
                    }
                }
                break;
            case LOADER_ENTRIES:
                progressBar.setVisibility(View.GONE);
                if (cursor.getCount() > 0) {
                    entriesRecyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                } else {
                    entriesRecyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }

                adapter.setCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_ENTRIES) {
            // release the loader's cursor
            adapter.setCursor(null);
        }
    }

    private void markEntryRead(final long id, final boolean read) {
        // mark entry as read
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                ContentValues values = new ContentValues();
                values.put(EntryColumns.FLAG_READ, read);
                return applicationContext.getContentResolver().update(Uris.newUserEntryUri(id), values, null, null) == 1;
            }
        }.execute();
    }

}
