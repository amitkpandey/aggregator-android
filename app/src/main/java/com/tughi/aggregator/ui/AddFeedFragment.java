package com.tughi.aggregator.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.feeds.FeedParser;
import com.tughi.aggregator.feeds.FeedParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * A {@link Fragment} used to preview and configure a feed before adding.
 */
public class AddFeedFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_URL = "url";
    public static final String ARG_TITLE = "title";

    private EditText titleEditText;

    private EntryListAdapter entryListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        entryListAdapter = new EntryListAdapter(getActivity());

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_feed_fragment, container, false);

        titleEditText = (EditText) view.findViewById(R.id.title);

        ListView listView = (ListView) view.findViewById(R.id.entries);
        listView.setAdapter(entryListAdapter);

        if (savedInstanceState == null) {
            titleEditText.setText(getArguments().getString(ARG_TITLE));
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.add_feed_fragment, menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loader, Bundle args) {
        return new FeedLoader(getActivity(), getArguments().getString(ARG_URL));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        titleEditText.setText(((FeedLoader) loader).result.feed.title);
        entryListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        entryListAdapter.swapCursor(null);
    }

    private static class FeedLoader extends AsyncTaskLoader<Cursor> {

        private final String feedUrl;

        private volatile FeedParser.Result result;

        public FeedLoader(Context context, String feedUrl) {
            super(context);

            this.feedUrl = feedUrl;
        }

        @Override
        public Cursor loadInBackground() {
            try {
                // parse feed
                URL url = new URL(feedUrl);
                URLConnection urlConnection = url.openConnection();
                result = FeedParser.parse(urlConnection);

                // build cursor
                if (result.status == HttpURLConnection.HTTP_OK) {
                    MatrixCursor cursor = new MatrixCursor(EntryListAdapter.ENTRY_PROJECTION, result.feed.entries.size());

                    for (FeedParser.Result.Feed.Entry entry : result.feed.entries) {
                        cursor.newRow()
                                .add(0)
                                .add(entry.title)
                                .add(entry.updatedTimestamp)
                                .add(result.feed.title)
                                .add(0);
                    }

                    return cursor;
                }
            } catch (IOException exception) {
                Log.e(getClass().getName(), "Loader failed", exception);
            } catch (FeedParserException exception) {
                Log.e(getClass().getName(), "Loader failed", exception);
            }

            return null;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

    }

}
