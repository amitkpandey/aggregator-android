package com.tughi.aggregator.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.Uris;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entry_list_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.updateSections();
    }

    @Override
    public void onListItemClick(ListView view, View itemView, int position, long id) {
        // mark entry as read
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                final Context context = (Context) params[0];
                final long id = (Long) params[1];

                ContentValues values = new ContentValues();
                values.put(EntryColumns.FLAG_READ, true);
                return context.getContentResolver().update(Uris.newUserEntryUri(id), values, null, null) == 1;
            }
        }.execute(applicationContext, id);
    }

    private static final String[] ENTRY_PROJECTION = {
            EntryColumns.ID,
            EntryColumns.TITLE,
            EntryColumns.UPDATED,
            EntryColumns.FEED_TITLE,
            EntryColumns.FLAG_READ,
    };
    private static final String ENTRY_SELECTION = EntryColumns.RO_FLAG_READ + " = 0";
    private static final String ENTRY_ORDER = EntryColumns.UPDATED + " ASC";
    private static final int ENTRY_TITLE_INDEX = 1;
    private static final int ENTRY_UPDATED_INDEX = 2;
    private static final int ENTRY_FEED_TITLE_INDEX = 3;
    private static final int ENTRY_FLAG_READ_INDEX = 4;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = getArguments().getParcelable(ARG_ENTRIES_URI);
        switch (id) {
            case LOADER_FEED:
                String uriString = uri.toString();
                Uri feedUri = Uri.parse(uriString.substring(0, uriString.lastIndexOf("/")));
                return new CursorLoader(applicationContext, feedUri, null, null, null, null);
            case LOADER_ENTRIES:
                return new CursorLoader(applicationContext, uri, ENTRY_PROJECTION, ENTRY_SELECTION, null, ENTRY_ORDER);
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

    private class EntryListAdapter extends CursorAdapter implements SectionListAdapter {

        private DateFormat timeFormat;
        private DateFormat dateFormat;

        private Calendar calendar = Calendar.getInstance();
        private long todayStart;
        private long yesterdayStart;

        private SparseArray<String> sections = new SparseArray<String>(2000);

        public EntryListAdapter(Context context) {
            super(context, null, false);

            timeFormat = android.text.format.DateFormat.getTimeFormat(context);
            dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
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
            tag.stateImageView = (ImageView) view.findViewById(R.id.state);
            tag.headerTextView = (TextView) view.findViewById(R.id.header);
            view.setTag(tag);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewTag tag = (ViewTag) view.getTag();
            tag.section = sections.get(cursor.getPosition());
            tag.titleTextView.setText(cursor.getString(ENTRY_TITLE_INDEX));
            int flagRead = cursor.getInt(ENTRY_FLAG_READ_INDEX);
            tag.titleTextView.setTypeface(flagRead == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tag.stateImageView.setImageLevel(flagRead);
            tag.feedTextView.setText(cursor.getString(ENTRY_FEED_TITLE_INDEX));
            tag.dateTextView.setText(timeFormat.format(cursor.getLong(ENTRY_UPDATED_INDEX)));
            if (tag.headerTextView != null) {
                tag.headerTextView.setText(tag.section);
            }
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            sections.clear();

            updateSections();

            return super.swapCursor(newCursor);
        }

        private void updateSections() {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (todayStart != calendar.getTimeInMillis()) {
                sections.clear();

                todayStart = calendar.getTimeInMillis();

                calendar.add(Calendar.DATE, -1);
                yesterdayStart = calendar.getTimeInMillis();

                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemViewType(int position) {
            String itemSection = getItemSection(position);

            if (position == 0) {
                return 1;
            }

            String previousItemSection = getItemSection(position - 1);
            if (!itemSection.equals(previousItemSection)) {
                return 1;
            }

            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public String getItemSection(int position) {
            String section = sections.get(position);
            if (section == null) {
                section = getItemSection((Cursor) getItem(position));
                sections.put(position, section);
            }
            return section;
        }

        private String getItemSection(Cursor cursor) {
            long updated = cursor.getLong(ENTRY_UPDATED_INDEX);
            if (updated >= todayStart) {
                return getString(R.string.today);
            }
            if (updated >= yesterdayStart) {
                return getString(R.string.yesterday);
            }
            return dateFormat.format(updated);
        }

        private class ViewTag implements SectionTag {
            private String section;

            private TextView titleTextView;
            private TextView feedTextView;
            private TextView dateTextView;
            private ImageView stateImageView;
            private TextView headerTextView;

            @Override
            public String getSection() {
                return section;
            }
        }

    }

}
