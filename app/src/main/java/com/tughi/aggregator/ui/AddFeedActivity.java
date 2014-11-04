package com.tughi.aggregator.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.tughi.aggregator.R;
import com.tughi.aggregator.feeds.FeedsFinder;

/**
 * An {@link Activity} for adding a feed.
 */
public class AddFeedActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_feed_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, new FindFeedsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Invoked from {@link FindFeedsFragment} when the user clicks on a detected feed.
     */
    public void onFeedSelected(FeedsFinder.Result.Feed feed) {
        Bundle arguments = new Bundle();
        arguments.putString(AddFeedFragment.ARG_URL, feed.href);
        arguments.putString(AddFeedFragment.ARG_TITLE, feed.title);

        AddFeedFragment fragment = new AddFeedFragment();
        fragment.setArguments(arguments);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

}
