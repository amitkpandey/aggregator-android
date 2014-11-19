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
import android.database.Cursor;
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
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getActivity().getApplicationContext();

        entriesUri = getArguments().getParcelable(ARG_ENTRIES_URI);
        feedId = Long.parseLong(entriesUri.getPathSegments().get(1));
        feedUri = Uris.newFeedUri(feedId);

        adapter = new EntryListAdapter(applicationContext);

        getLoaderManager().initLoader(LOADER_FEED, null, this);
        getLoaderManager().initLoader(LOADER_ENTRIES, null, this);

        if (feedId > 0) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entry_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        RecyclerView entriesRecyclerView = (RecyclerView) view.findViewById(R.id.entries);
        entriesRecyclerView.setAdapter(adapter);
        entriesRecyclerView.addOnItemTouchListener(new OnItemTouchListener());
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
            FeedColumns.NEXT_SYNC,
    };
    private static final int FEED_ID = 0;
    private static final int FEED_TITLE = 1;
    private static final int FEED_NEXT_SYNC = 2;

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

                    if (BuildConfig.DEBUG) {
                        if (feedId > 0) {
                            String text = String.format("Next sync: %tc", cursor.getLong(FEED_NEXT_SYNC));
                            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            case LOADER_ENTRIES:
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
        private final int minimumFlingVelocity;
        private final int maximumFlingVelocity;
        private final int animationTime;

        private MotionEvent downEvent;
        private float downX;
        private float downY;

        private boolean swipeDetection;

        private VelocityTracker velocityTracker;

        private EntryListAdapter.ViewHolder viewHolder;

        private OnItemTouchListener() {
            ViewConfiguration configuration = ViewConfiguration.get(applicationContext);
            final int touchSlop = configuration.getScaledTouchSlop();
            touchSlopSquare = touchSlop * touchSlop;
            minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            maximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();

            animationTime = applicationContext.getResources().getInteger(android.R.integer.config_shortAnimTime);
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();

            switch (event.getAction() & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    swipeDetection = true;

                    downX = x;
                    downY = y;

                    if (downEvent != null) {
                        downEvent.recycle();
                    }
                    downEvent = MotionEvent.obtain(event);

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                    }
                    velocityTracker = VelocityTracker.obtain();
                    velocityTracker.addMovement(event);

                    break;
                case MotionEvent.ACTION_MOVE:
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
                                    onTouchEvent(recyclerView, event);

                                    // receive next swipe events in onTouchEvent(...)
                                    return true;
                                }
                            }
                        } else {
                            velocityTracker.addMovement(event);
                        }
                    }

                    break;
            }

            // intercept next event
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            final View swipeContentView = viewHolder.swipeContentView;
            final float width = swipeContentView.getWidth();

            final float deltaX = event.getX() - downX;

            switch (event.getAction() & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_MOVE: {
                    velocityTracker.addMovement(event);

                    if (deltaX < 0) {
                        viewHolder.swipeLeftView.setVisibility(View.GONE);
                        viewHolder.swipeRightView.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.swipeLeftView.setVisibility(View.VISIBLE);
                        viewHolder.swipeRightView.setVisibility(View.GONE);
                    }

                    // move view
                    swipeContentView.setTranslationX(deltaX);
                    swipeContentView.setAlpha(1 - Math.abs(deltaX) / width);

                    break;
                }
                case MotionEvent.ACTION_UP: {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
                    final float velocity = velocityTracker.getXVelocity();

                    final boolean swipe;
                    final boolean swipeRight;
                    if (Math.abs(deltaX) > width / 2) {
                        swipe = true;
                        swipeRight = deltaX > 0;
                    } else if (Math.abs(velocity) >= minimumFlingVelocity && velocity * deltaX > 0) {
                        swipe = true;
                        swipeRight = velocity > 0;
                    } else {
                        swipe = false;
                        swipeRight = false;
                    }

                    if (swipe) {
                        final long entryId = viewHolder.getItemId();
                        final boolean entryNewRead = !viewHolder.isRead();

                        // swiped
                        swipeContentView.animate()
                                .translationX(swipeRight ? width : -width)
                                .alpha(0)
                                .setDuration(animationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (swipeRight) {
                                            markEntryRead(entryId, entryNewRead);
                                        } else {
                                            markEntryJunk(entryId);
                                        }
                                    }
                                });
                    } else {
                        // swipe canceled
                        swipeContentView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(animationTime);
                    }

                    break;
                }
            }
        }

    }

}
