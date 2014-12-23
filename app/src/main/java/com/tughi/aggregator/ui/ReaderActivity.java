package com.tughi.aggregator.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;

public class ReaderActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener {

    public static final String EXTRA_CURSOR_POSITION = "cursor_position";

    private ViewPager pager;
    protected Adapter adapter = new Adapter();
    protected Cursor cursor;

    private Intent resultData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity);

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOnPageChangeListener(this);
        pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.reader_pager_margin));
        pager.setPageMarginDrawable(R.drawable.reader_pager_margin);
        pager.setAdapter(adapter);

        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(0, null, this);

        resultData = new Intent().putExtra(EXTRA_CURSOR_POSITION, getIntent().getIntExtra(EXTRA_CURSOR_POSITION, 0));
        setResult(RESULT_OK, resultData);

        if (savedInstanceState != null) {
            resultData.putExtra(EXTRA_CURSOR_POSITION, savedInstanceState.getInt(EXTRA_CURSOR_POSITION));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CURSOR_POSITION, resultData.getIntExtra(EXTRA_CURSOR_POSITION, 0));

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                // TODO: make volume navigation optional
                pager.setCurrentItem(pager.getCurrentItem() + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? -1 : 1));
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private static final String[] ENTRY_PROJECTION = {
            EntryColumns.ID,
            EntryColumns.TITLE,
            EntryColumns.UPDATED,
            EntryColumns.FLAG_READ,
            EntryColumns.FLAG_STAR,
            EntryColumns.DATA,
    };
    protected static final int ENTRY_ID = 0;
    protected static final int ENTRY_TITLE = 1;
    protected static final int ENTRY_UPDATED = 2;
    protected static final int ENTRY_FLAG_READ = 3;
    protected static final int ENTRY_FLAG_STAR = 4;
    protected static final int ENTRY_DATA = 5;

    private static final String ENTRY_SELECTION = EntryColumns.RO_FLAG_READ + " = 0";
    private static final String ENTRY_ORDER = EntryColumns.UPDATED + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri entriesUri = getIntent().getData();
        return new CursorLoader(this, entriesUri, ENTRY_PROJECTION, ENTRY_SELECTION, null, ENTRY_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.cursor = cursor;
        adapter.notifyDataSetChanged();

        pager.setCurrentItem(resultData.getIntExtra(EXTRA_CURSOR_POSITION, 0), false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursor = null;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // nothing to do here
    }

    @Override
    public void onPageSelected(int position) {
        resultData.putExtra(EXTRA_CURSOR_POSITION, position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // nothing to do here
    }

    protected class Adapter extends FragmentStatePagerAdapter {

        public Adapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            cursor.moveToPosition(position);
            Bundle arguments = new Bundle();
            arguments.putInt(ReaderEntryFragment.ARG_CURSOR_POSITION, position);
            return Fragment.instantiate(ReaderActivity.this, ReaderEntryFragment.class.getName(), arguments);
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (cursor != null && cursor.moveToPosition(position)) {
                if (object != null) {
                    ((ReaderEntryFragment) object).markRead(ReaderActivity.this);
                }
            }

            super.setPrimaryItem(container, position, object);
        }

    }

}
