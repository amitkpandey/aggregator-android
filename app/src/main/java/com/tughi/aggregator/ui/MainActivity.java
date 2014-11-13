package com.tughi.aggregator.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tughi.aggregator.R;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.Uris;
import com.tughi.aggregator.service.FeedsSyncService;

public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final int LOADER_FEEDS = 1;

    private SyncLogFragment syncLogFragment;

    private DrawerLayout drawerLayout;

    private ListView drawerListView;
    private CursorAdapter drawerListAdapter;

    private ActionBarDrawerToggle drawerToggle;

    private CharSequence title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        Toolbar actionBar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(actionBar);

        syncLogFragment = (SyncLogFragment) getSupportFragmentManager().findFragmentById(R.id.sync_log);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, actionBar, 0, 0) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                getSupportActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                syncLogFragment.setScaleFactor(1 - slideOffset);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        drawerListAdapter = new FeedListAdapter();

        drawerListView = (ListView) drawerLayout.findViewById(R.id.drawer);
        drawerListView.setAdapter(drawerListAdapter);
        drawerListView.setOnItemClickListener(this);

        getLoaderManager().initLoader(LOADER_FEEDS, null, this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        title = getTitle();

        // show the 'unread' feed by default
        selectDrawerItem(0, -1);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            getContentResolver().call(Uris.BASE_URI, Uris.CALL_COMMIT_ENTRIES_READ_STATE, null, null);
        }

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawerListView)) {
            drawerLayout.closeDrawer(drawerListView);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerListView);

        menu.setGroupVisible(R.id.content, !drawerOpen);
        menu.setGroupVisible(R.id.drawer, drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.subscribe:
                startActivity(new Intent(this, AddFeedActivity.class));
                return true;
            case R.id.sync:
                startService(new Intent(this, FeedsSyncService.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onItemClick(AdapterView<?> view, View itemView, int position, long id) {
        selectDrawerItem(position, id);
    }

    private void selectDrawerItem(int position, long feedId) {
        // create fragment
        Fragment fragment = new EntryListFragment();
        Bundle args = new Bundle();
        args.putParcelable(EntryListFragment.ARG_ENTRIES_URI, Uris.newFeedEntriesUri(feedId));
        fragment.setArguments(args);

        // replace existing fragment with the new one
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();

        // mark position as active
        if (drawerListAdapter.getCursor() != null) {
            // if loaded only... otherwise it will be done in onLoadFinished
            drawerListView.setItemChecked(position, true);
        }

        // finish
        drawerLayout.closeDrawer(drawerListView);

        syncLogFragment.setSyncLogUri(Uris.newFeedSyncLogUri(feedId));
    }

    private static final String[] FEED_PROJECTION = {
            FeedColumns.ID,
            FeedColumns.TITLE,
            FeedColumns.FAVICON,
            FeedColumns.UNREAD_COUNT
    };
    private static final int FEED_ID_INDEX = 0;
    private static final int FEED_TITLE_INDEX = 1;
    private static final int FEED_FAVICON_INDEX = 2;
    private static final int FEED_UNREAD_COUNT_INDEX = 3;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Uris.newFeedsUri(), FEED_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        drawerListAdapter.swapCursor(cursor);

        if (drawerListView.getCheckedItemCount() == 0) {
            // mark the 'unread' feed as active
            drawerListView.setItemChecked(0, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        drawerListAdapter.swapCursor(null);
    }

    /**
     * A {@link ListAdapter} for the drawer view.
     */
    private class FeedListAdapter extends CursorAdapter {

        public FeedListAdapter() {
            super(MainActivity.this, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View view = getLayoutInflater().inflate(R.layout.drawer_feed_item, viewGroup, false);

            ViewTag tag = new ViewTag();
            tag.titleTextView = (TextView) view.findViewById(R.id.title);
            tag.unreadCountTextView = (TextView) view.findViewById(R.id.unread_count);
            tag.faviconImageView = (ImageView) view.findViewById(R.id.favicon);
            view.setTag(tag);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewTag tag = (ViewTag) view.getTag();
            int faviconPlaceholder;

            // set feed title
            switch (cursor.getInt(FEED_ID_INDEX)) {
                case -1:
                    tag.titleTextView.setText(R.string.unread_feed);
                    faviconPlaceholder = R.drawable.favicon_unread;
                    break;
                case -2:
                    tag.titleTextView.setText(R.string.starred_feed);
                    faviconPlaceholder = R.drawable.favicon_starred;
                    break;
                default:
                    tag.titleTextView.setText(cursor.getString(FEED_TITLE_INDEX));
                    faviconPlaceholder = R.drawable.favicon_placeholder;
            }

            // set feed unread items count
            if (cursor.getInt(FEED_UNREAD_COUNT_INDEX) > 0) {
                tag.unreadCountTextView.setText(cursor.getString(FEED_UNREAD_COUNT_INDEX));
                tag.unreadCountTextView.setVisibility(View.VISIBLE);
            } else {
                tag.unreadCountTextView.setVisibility(View.GONE);
            }

            // set feed favicon
            if (!cursor.isNull(FEED_FAVICON_INDEX)) {
                Picasso.with(context)
                        .load(cursor.getString(FEED_FAVICON_INDEX))
                        .placeholder(faviconPlaceholder)
                        .into(tag.faviconImageView);
            } else {
                tag.faviconImageView.setImageResource(faviconPlaceholder);
            }
        }

        private class ViewTag {
            private TextView titleTextView;
            private TextView unreadCountTextView;
            private ImageView faviconImageView;
        }

    }

}
