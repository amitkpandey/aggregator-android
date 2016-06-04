package com.tughi.aggregator.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.FeedUpdateModes;
import com.tughi.aggregator.content.Uris;

/**
 * An {@link Activity} to configure the mode in which the feed gets updated.
 */
public class FeedUpdateModeActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final int LOADER_FEED = 0;

    private Uri feedUri;

    private int currentUpdateMode;

    private ModeSpinnerAdapter modeSpinnerAdapter;

    private Spinner modeSpinner;
    private ViewGroup autoModeDetails;
    private ViewGroup disabledModeDetails;
    private ImageButton saveImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedUri = getIntent().getData();

        modeSpinnerAdapter = new ModeSpinnerAdapter();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setSubtitle(R.string.feed_update_mode);

        setContentView(R.layout.feed_update_mode_acivity);

        modeSpinner = (Spinner) findViewById(R.id.mode);
        assert modeSpinner != null;
        modeSpinner.setOnItemSelectedListener(this);

        autoModeDetails = (ViewGroup) findViewById(R.id.mode_auto_details);
        disabledModeDetails = (ViewGroup) findViewById(R.id.mode_disabled_details);

        saveImageButton = (ImageButton) findViewById(R.id.save);
        assert saveImageButton != null;
        saveImageButton.setOnClickListener(this);

        modeSpinner.setAdapter(modeSpinnerAdapter);

        getLoaderManager().initLoader(LOADER_FEED, null, this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, final long id) {
        int selectedUpdateMode = (int) id;

        autoModeDetails.setVisibility(selectedUpdateMode == FeedUpdateModes.AUTO ? View.VISIBLE : View.GONE);
        disabledModeDetails.setVisibility(selectedUpdateMode == FeedUpdateModes.DISABLED ? View.VISIBLE : View.GONE);

        saveImageButton.setVisibility(selectedUpdateMode != currentUpdateMode ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // ignored
    }

    private static final String[] FEED_PROJECTION = {
            FeedColumns.TITLE,
            FeedColumns.UPDATE_MODE,
    };
    private static final int FEED_TITLE = 0;
    private static final int FEED_UPDATE_MODE = 1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, feedUri, FEED_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            String title = cursor.getString(FEED_TITLE);
            setTitle(title);

            currentUpdateMode = cursor.getInt(FEED_UPDATE_MODE);
            modeSpinner.setSelection(modeSpinnerAdapter.getItemPosition(currentUpdateMode));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // ignored
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                saveAndFinish();
                break;
        }
    }

    private void saveAndFinish() {
        // save on a background thread
        int updateMode = (int) modeSpinner.getSelectedItemId();
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                Context context = (Context) objects[0];
                Uri feedUri = (Uri) objects[1];
                int updateMode = (Integer) objects[2];

                Uri userFeedUri = Uris.newUserFeedUri(Long.parseLong(feedUri.getLastPathSegment()));
                ContentValues values = new ContentValues(1);
                values.put(FeedColumns.UPDATE_MODE, updateMode);
                context.getContentResolver().update(userFeedUri, values, null, null);

                return null;
            }
        }.execute(getApplicationContext(), feedUri, updateMode);

        // finish activity
        finish();
    }

    private class ModeSpinnerAdapter extends BaseAdapter {

        private int[] ids;
        private String[] titles;
        private String[] descriptions;

        public ModeSpinnerAdapter() {
            Resources resources = getResources();
            ids = resources.getIntArray(R.array.feed_update_mode_ids);
            titles = resources.getStringArray(R.array.feed_update_mode_titles);
            descriptions = resources.getStringArray(R.array.feed_update_mode_descriptions);
        }

        @Override
        public int getCount() {
            return ids.length;
        }

        @Override
        public String getItem(int position) {
            return descriptions[position];
        }

        @Override
        public long getItemId(int position) {
            return ids[position];
        }

        public int getItemPosition(int id) {
            for (int position = 0; position < ids.length; position++) {
                if (ids[position] == id) {
                    return position;
                }
            }

            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.feed_update_mode_spinner_item, parent, false);
            }

            ((TextView) convertView.findViewById(R.id.title)).setText(titles[position]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.feed_update_mode_spinner_dropdown_item, parent, false);
            }

            ((TextView) convertView.findViewById(R.id.title)).setText(titles[position]);
            ((TextView) convertView.findViewById(R.id.description)).setText(descriptions[position]);

            return convertView;
        }

    }

}
