package com.tughi.aggregator.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.feeds.FeedsFinder;

import java.io.IOException;
import java.util.List;

/**
 * A {@link Fragment} used to search for feeds.
 */
public class FindFeedsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private SearchView searchView;
    private ProgressBar progressBar;

    private FeedsListAdapter feedsListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_feed_query_fragment, container, false);

        searchView = (SearchView) view.findViewById(R.id.query);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        progressBar = (ProgressBar) view.findViewById(R.id.progress);


        ListView feedsListView = (ListView) view.findViewById(R.id.feeds);
        feedsListView.setAdapter(feedsListAdapter = new FeedsListAdapter());
        feedsListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Intent intent = getActivity().getIntent();
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                // activity was started as a share intent
                String query = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString();
                searchView.setQuery(query, true);
            }
        }
    }

    private void onSearch(String query) {
        // show progress indicator
        progressBar.setVisibility(View.VISIBLE);

        // reset previous search, if any
        feedsListAdapter.setFeeds(null);

        // start searching
        new SearchTask().execute(query);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((AddFeedActivity) getActivity()).onFeedSelected(feedsListAdapter.getItem(position));
    }

    private class SearchTask extends AsyncTask<Object, Object, FeedsFinder.Result> {

        @Override
        protected FeedsFinder.Result doInBackground(Object... objects) {
            String query = (String) objects[0];

            try {
                // URL-based search
                return FeedsFinder.find(query);
            } catch (IOException exception) {
                Log.e(getClass().getName(), "Feeds search failed.", exception);
            }

            return null;
        }

        @Override
        protected void onPostExecute(FeedsFinder.Result result) {
            // hide progress indicator
            progressBar.setVisibility(View.GONE);

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
