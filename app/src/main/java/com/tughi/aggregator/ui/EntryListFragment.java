package com.tughi.aggregator.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
    private ProgressBar progressBar;
    private View emptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getActivity().getApplicationContext();

        entriesUri = getArguments().getParcelable(ARG_ENTRIES_URI);
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
        entriesRecyclerView.addOnItemTouchListener(new OnItemTouchListener());

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
    };
    private static final int FEED_ID = 0;
    private static final int FEED_TITLE = 1;

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
                // update the activity title
                if (cursor.moveToFirst()) {
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

    private void markEntryJunk(final long id) {
        // TODO: mark entry as junk
        Toast.makeText(applicationContext, "Not implemented", Toast.LENGTH_SHORT).show();
    }

    /**
     * A {@link RecyclerView.OnItemTouchListener} that detects an item swipe.
     */
    private class OnItemTouchListener implements RecyclerView.OnItemTouchListener {

        private final float touchSlopSquare;
        private final int animationTime;
        private final int swipeGestureTrigger;

        private MotionEvent downEvent;
        private float downX;
        private float downY;

        private boolean swipeDetection;
        private boolean swipeCancelled;

        private EntryListAdapter.ViewHolder viewHolder;

        private int junkColor;
        private int readColor;
        private int unreadColor;

        private OnItemTouchListener() {
            ViewConfiguration configuration = ViewConfiguration.get(applicationContext);
            final int touchSlop = configuration.getScaledTouchSlop();
            touchSlopSquare = touchSlop * touchSlop;

            Resources resources = applicationContext.getResources();
            swipeGestureTrigger = (int) (120 * resources.getDisplayMetrics().density);

            animationTime = resources.getInteger(android.R.integer.config_shortAnimTime);
            junkColor = resources.getColor(R.color.junk);
            readColor = resources.getColor(R.color.entry_read);
            unreadColor = resources.getColor(R.color.entry_unread);
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();

            switch (event.getAction() & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    swipeDetection = true;

                    downX = x;
                    downY = y;

                    if (downEvent != null) {
                        downEvent.recycle();
                    }
                    downEvent = MotionEvent.obtain(event);

                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (swipeDetection) {
                        final int deltaX = (int) (x - downX);
                        final int deltaY = (int) (y - downY);
                        final int distance = (deltaX * deltaX) + (deltaY * deltaY);
                        if (distance > touchSlopSquare) {
                            // scroll detected

                            swipeDetection = false;

                            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                // swipe detected

                                View itemView = recyclerView.findChildViewUnder(downX, downY);
                                if (itemView != null) {
                                    viewHolder = (EntryListAdapter.ViewHolder) recyclerView.getChildViewHolder(itemView);

                                    // start swiping
                                    swipeCancelled = false;
                                    onTouchEvent(recyclerView, event);

                                    // receive next swipe events in onTouchEvent(...)
                                    return true;
                                }
                            }
                        }
                    }

                    break;
                }
                case MotionEvent.ACTION_UP: {
                    // item clicked

                    // TODO: open item

                    return true;
                }
            }

            // intercept next event
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            if (swipeCancelled) {
                return;
            }

            final View swipeContentView = viewHolder.swipeContentView;
            final float width = swipeContentView.getWidth();

            final float deltaX = event.getX() - downX;

            switch (event.getAction() & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_MOVE: {
                    // move view
                    swipeContentView.setTranslationX(deltaX);

                    if (deltaX < 0) {
                        // gesture: mark as junk
                        viewHolder.swipeJunkView.setVisibility(View.VISIBLE);
                        swipeContentView.setAlpha(1 - Math.abs(deltaX) / width);

                        if (-deltaX > Math.min(swipeGestureTrigger << 1, width / 2)) {
                            swipeCancelled = true;

                            // apply gesture
                            final long entryId = viewHolder.getItemId();
                            swipeContentView.animate()
                                    .translationX(-width)
                                    .alpha(0)
                                    .setDuration(animationTime)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            markEntryJunk(entryId);
                                        }
                                    });
                        }
                    } else {
                        // gesture: toggle read state
                        int fromColor;
                        int toColor;
                        if (viewHolder.isRead()) {
                            fromColor = readColor;
                            toColor = unreadColor;
                        } else {
                            fromColor = unreadColor;
                            toColor = readColor;
                        }
                        int delta = Math.min((int) deltaX, swipeGestureTrigger);
                        int deltaColor = Color.rgb(
                                (Color.red(toColor) - Color.red(fromColor)) * delta / swipeGestureTrigger + Color.red(fromColor),
                                (Color.green(toColor) - Color.green(fromColor)) * delta / swipeGestureTrigger + Color.green(fromColor),
                                (Color.blue(toColor) - Color.blue(fromColor)) * delta / swipeGestureTrigger + Color.blue(fromColor)
                        );
                        viewHolder.swipeJunkView.setVisibility(View.GONE);
                        viewHolder.stateView.setBackgroundColor(deltaColor);

                        if (deltaX > swipeGestureTrigger) {
                            swipeCancelled = true;

                            // apply gesture
                            final long entryId = viewHolder.getItemId();
                            final boolean entryNewRead = !viewHolder.isRead();
                            swipeContentView.animate()
                                    .translationX(0)
                                    .setDuration(animationTime)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            markEntryRead(entryId, entryNewRead);
                                        }
                                    });
                        }
                    }

                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    // swipe cancelled

                    if (deltaX < 0) {
                        swipeContentView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(animationTime);
                    } else {
                        viewHolder.stateView.setBackgroundColor(viewHolder.isRead() ? readColor : unreadColor);
                        swipeContentView.animate()
                                .translationX(0)
                                .setDuration(animationTime);
                    }

                    break;
                }
            }
        }

    }

}
