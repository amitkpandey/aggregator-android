package com.tughi.aggregator.ui;

import android.widget.ListAdapter;

/**
 * Extends the {@link ListAdapter} with the {@link #getItemHeader(int)} method
 * which is required by {@link HeaderListView}.
 */
public interface HeaderListAdapter extends ListAdapter {

    /**
     * Returns the header of a specific item.
     */
    public String getItemHeader(int position);

}
