package com.tughi.aggregator.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * A {@link ListFragment} for feed entries.
 * The displayed entries depend on the provided entries {@link Uri}.
 */
public class EntryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The entries {@link Uri}
     */
    public static final String ARG_ENTRIES_URI = "uri";

    private static final int LOADER_FEED = 1;
    private static final int LOADER_ENTRIES = 2;

    private Context applicationContext;

    private EntryListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getActivity().getApplicationContext();

        setListAdapter(adapter = new EntryListAdapter(applicationContext));

        getLoaderManager().initLoader(LOADER_FEED, null, this);
        getLoaderManager().initLoader(LOADER_ENTRIES, null, this);
    }

    private static final String[] ENTRY_PROJECTION = {
            EntryColumns.ID,
            EntryColumns.TITLE,
            EntryColumns.UPDATED,
            EntryColumns.FEED_TITLE,
    };
    private static final String ENTRY_ORDER = EntryColumns.UPDATED;
    private static final int ENTRY_TITLE_INDEX = 1;
    private static final int ENTRY_UPDATED_INDEX = 2;
    private static final int ENTRY_FEED_TITLE_INDEX = 3;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = getArguments().getParcelable(ARG_ENTRIES_URI);
        switch (id) {
            case LOADER_FEED:
                String uriString = uri.toString();
                Uri feedUri = Uri.parse(uriString.substring(0, uriString.lastIndexOf("/")));
                return new CursorLoader(applicationContext, feedUri, null, null, null, null);
            case LOADER_ENTRIES:
                return new CursorLoader(applicationContext, uri, ENTRY_PROJECTION, null, null, ENTRY_ORDER);
        }

        // never happens
        throw new IllegalStateException();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_FEED:
                // update the activity title
                if (cursor.moveToFirst()) {
                    String title;
                    switch (cursor.getInt(cursor.getColumnIndex(FeedColumns.ID))) {
                        case -2:
                            title = getString(R.string.starred_feed);
                            break;
                        case -1:
                            title = getString(R.string.unread_feed);
                            break;
                        default:
                            title = cursor.getString(cursor.getColumnIndex(FeedColumns.TITLE));
                    }
                    getActivity().setTitle(title);
                }
                break;
            case LOADER_ENTRIES:
                adapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_ENTRIES) {
            // release the loader's cursor
            adapter.swapCursor(null);
        }
    }

    private class EntryListAdapter extends CursorAdapter {

        private DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);

        private Calendar calendar = Calendar.getInstance();

        private long previousDate = -1;
        private int previousPosition = -1;

        public EntryListAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            int layout;
            switch (getItemViewType(cursor.getPosition())) {
                case 1:
                    layout = R.layout.entry_list_header_item;
                    break;
                default:
                    layout = R.layout.entry_list_item;
            }
            View view = LayoutInflater.from(context).inflate(layout, parent, false);

            ViewTag tag = new ViewTag();
            tag.titleTextView = (TextView) view.findViewById(R.id.title);
            tag.feedTextView = (TextView) view.findViewById(R.id.feed);
            tag.dateTextView = (TextView) view.findViewById(R.id.date);
            tag.headerTextView = (TextView) view.findViewById(R.id.header);
            view.setTag(tag);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewTag tag = (ViewTag) view.getTag();
            tag.titleTextView.setText(cursor.getString(ENTRY_TITLE_INDEX));
            tag.feedTextView.setText(cursor.getString(ENTRY_FEED_TITLE_INDEX));
            tag.dateTextView.setText(timeFormat.format(cursor.getLong(ENTRY_UPDATED_INDEX)));
            if (tag.headerTextView != null) {
                tag.headerTextView.setText(dateFormat.format(cursor.getLong(ENTRY_UPDATED_INDEX)));
            }
        }

        @Override
        public int getItemViewType(int position) {
            Cursor cursor = (Cursor) getItem(position);
            calendar.setTimeInMillis(cursor.getLong(ENTRY_UPDATED_INDEX));
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long currentDate = calendar.getTimeInMillis();

            try {
                if (position == 0) {
                    return 1;
                } else if (previousPosition == position - 1 && previousDate != currentDate) {
                    return 1;
                } else {
                    if (cursor.moveToPrevious()) {
                        calendar.setTimeInMillis(cursor.getLong(ENTRY_UPDATED_INDEX));
                        calendar.set(Calendar.HOUR, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        previousDate = calendar.getTimeInMillis();
                    }
                    cursor.moveToPosition(position);
                    if (previousDate != currentDate) {
                        return 1;
                    }
                }

                return 0;
            } finally {
                previousDate = currentDate;
                previousPosition = position;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        private class ViewTag {
            private TextView titleTextView;
            private TextView feedTextView;
            private TextView dateTextView;
            private TextView headerTextView;
        }

    }

}
