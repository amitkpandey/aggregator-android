package com.tughi.aggregator.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.feeds.FeedsFinder;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * An {@link Activity} for adding a feed.
 */
public class AddFeedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // activity was not recreated

            Bundle queryFragmentArgs = new Bundle();

            Intent intent = getIntent();
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                queryFragmentArgs.putCharSequence(QueryFragment.ARG_QUERY, intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
            }

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, Fragment.instantiate(this, QueryFragment.class.getName(), queryFragmentArgs))
                    .commit();
        }
    }

    public static class QueryFragment extends Fragment {

        private static final String ARG_QUERY = "query";

        private EditText queryEditText;
        private ImageButton searchButton;
        private ProgressBar progressBar;

        private FeedsListAdapter feedsListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.add_feed_query_fragment, container, false);

            queryEditText = (EditText) view.findViewById(R.id.query);
            queryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        onSearch();
                        return true;
                    }
                    return false;
                }
            });
            queryEditText.setFocusable(savedInstanceState != null || getArguments().get(ARG_QUERY) == null);

            searchButton = (ImageButton) view.findViewById(R.id.search);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSearch();
                }
            });

            progressBar = (ProgressBar) view.findViewById(R.id.progress);


            ListView feedsListView = (ListView) view.findViewById(R.id.feeds);
            feedsListView.setAdapter(feedsListAdapter = new FeedsListAdapter());

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            if (savedInstanceState == null) {
                CharSequence query = getArguments().getCharSequence(ARG_QUERY);
                if (query != null) {
                    queryEditText.setText(query);
                    onSearch();
                }
            }
        }

        private void onSearch() {
            // disable query field
            queryEditText.clearFocus();
            queryEditText.setEnabled(false);

            // show progress indicator
            searchButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            // hide software keyboard
            Context context = queryEditText.getContext();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(queryEditText.getWindowToken(), 0);

            // reset previous search, if any
            feedsListAdapter.setFeeds(null);

            // start searching
            new SearchTask().execute(queryEditText.getText().toString());
        }

        private class SearchTask extends AsyncTask<Object, Object, FeedsFinder.Result> {

            @Override
            protected FeedsFinder.Result doInBackground(Object... objects) {
                String query = (String) objects[0];

                try {
                    // URL-based search
                    URL url = new URL(query);
                    return FeedsFinder.find(url.openConnection());
                } catch (IOException exception) {
                    Log.e(getClass().getName(), "Feeds search failed.", exception);
                }

                return null;
            }

            @Override
            protected void onPostExecute(FeedsFinder.Result result) {
                // hide progress indicator
                searchButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                // enable query field
                queryEditText.setFocusableInTouchMode(true);
                queryEditText.setEnabled(true);

                // show result
                if (result != null) {
                    feedsListAdapter.setFeeds(result.feeds);
                }
            }

        }

        private class FeedsListAdapter extends BaseAdapter {

            private List<FeedsFinder.Result.Feed> feeds;

            public void setFeeds(List<FeedsFinder.Result.Feed> feeds) {
                this.feeds = feeds;
                notifyDataSetInvalidated();
            }

            public int getCount() {
                return feeds == null ? 0 : feeds.size();
            }

            @Override
            public FeedsFinder.Result.Feed getItem(int position) {
                return feeds.get(position);
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public long getItemId(int position) {
                return -1;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.add_feed_list_item, parent, false);
                }

                FeedsFinder.Result.Feed feed = getItem(position);

                TextView titleTextView = (TextView) convertView.findViewById(R.id.title);
                titleTextView.setText(feed.title);
                TextView urlTextView = (TextView) convertView.findViewById(R.id.url);
                urlTextView.setText(feed.href);

                return convertView;
            }

        }

    }

}
