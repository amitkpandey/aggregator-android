package com.tughi.aggregator.ui;

import android.widget.ListAdapter;

/**
 * Extends the {@link ListAdapter} with the {@link #getItemSection(int)} method
 * which is required by {@link SectionListView}.
 */
public interface SectionListAdapter extends ListAdapter {

    /**
     * Returns the section of a specific item.
     */
    public String getItemSection(int position);

}
