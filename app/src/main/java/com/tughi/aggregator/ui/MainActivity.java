package com.tughi.aggregator.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerListView = (ListView) drawerLayout.findViewById(R.id.drawer);

        drawerListAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{"title"},
                new int[]{android.R.id.text1},
                0
        );
        drawerListView.setAdapter(drawerListAdapter);
        drawerListView.setOnItemClickListener(this);

        getLoaderManager().initLoader(LOADER_FEEDS, null, this);
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
