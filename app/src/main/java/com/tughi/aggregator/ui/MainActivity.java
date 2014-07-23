package com.tughi.aggregator.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.AggregatorUris;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final int LOADER_FEEDS = 1;

    private DrawerLayout drawerLayout;

    private ListView drawerListView;
    private CursorAdapter drawerListAdapter;

    private ActionBarDrawerToggle drawerToggle;

    private CharSequence title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, 0, 0) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                getActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        drawerListAdapter = new SimpleCursorAdapter(
                this,
                R.layout.drawer_feed_item,
                null,
                new String[]{"title", "unread_count"},
                new int[]{R.id.title, R.id.unread_count},
                0
        );

        drawerListView = (ListView) drawerLayout.findViewById(R.id.drawer);
        drawerListView.setAdapter(drawerListAdapter);
        drawerListView.setOnItemClickListener(this);

        getLoaderManager().initLoader(LOADER_FEEDS, null, this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        title = getTitle();

        selectDrawerItem(0, 1);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerListView);
        getFragmentManager().findFragmentById(R.id.content).setHasOptionsMenu(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    @Override
    public void onItemClick(AdapterView<?> view, View itemView, int position, long id) {
        selectDrawerItem(position, id);
    }

    private void selectDrawerItem(int position, long id) {
        // create fragment
        Fragment fragment = new EntryListFragment();
        Bundle args = new Bundle();
        args.putParcelable(EntryListFragment.ARG_ENTRIES_URI, AggregatorUris.newFeedEntriesUri(id));
        fragment.setArguments(args);

        // replace existing fragment with the new one
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();

        drawerListView.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerListView);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, AggregatorUris.newFeedsUri(), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        drawerListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        drawerListAdapter.swapCursor(null);
    }

}
