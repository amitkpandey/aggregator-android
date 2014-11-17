package com.tughi.aggregator.ui;

import android.support.v7.widget.RecyclerView;

/**
 * Additional interface to be implemented by {@link RecyclerView.Adapter}s that contain sections.
 */
public interface SectionListAdapter {

    /**
     * Returns the section of a specific item.
     */
    public String getItemSection(int position);

}
