package com.tughi.aggregator.ui;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * A {@link CursorAdapter} used to populate a {@link SectionListView} with feed entries.
 */
/*local*/ class EntryListAdapter extends RecyclerView.Adapter<EntryListAdapter.ViewHolder> implements SectionListAdapter {

    public static final String[] ENTRY_PROJECTION = {
            EntryColumns.ID,
            EntryColumns.TITLE,
            EntryColumns.UPDATED,
            EntryColumns.FEED_TITLE,
            EntryColumns.FEED_FAVICON,
            EntryColumns.FLAG_READ,
    };
    public static final int ENTRY_ID = 0;
    public static final int ENTRY_TITLE = 1;
    public static final int ENTRY_UPDATED = 2;
    public static final int ENTRY_FEED_TITLE = 3;
    public static final int ENTRY_FEED_FAVICON = 4;
    public static final int ENTRY_FLAG_READ = 5;

    private Context context;
    private int unreadColor;
    private int readColor;

    private DateFormat timeFormat;
    private DateFormat dateFormat;

    private Calendar calendar = Calendar.getInstance();
    private long todayStart;
    private long yesterdayStart;

    private SparseArray<String> sections = new SparseArray<>();

    private Cursor cursor;

    public EntryListAdapter(Context context) {
        this.context = context;

        Resources resources = context.getResources();
        readColor = ResourcesCompat.getColor(resources, R.color.entry_read, null);
        unreadColor = ResourcesCompat.getColor(resources, R.color.entry_unread, null);

        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        dateFormat = android.text.format.DateFormat.getLongDateFormat(context);

        setHasStableIds(true);
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;

        sections.clear();
        updateSections();

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case 1:
                layout = R.layout.entry_list_header_item;
                break;
            default:
                layout = R.layout.entry_list_item;
        }
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Cursor cursor = this.cursor;

        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Invalid cursor position: " + position);
        }

        holder.entryView.setTranslationX(0);
        holder.entryView.setAlpha(1);

        holder.section = sections.get(cursor.getPosition());
        holder.titleTextView.setText(Html.fromHtml(cursor.getString(ENTRY_TITLE)));
        if (cursor.getInt(ENTRY_FLAG_READ) == 0) {
            holder.read = false;
            holder.stateView.setBackgroundColor(unreadColor);
        } else {
            holder.read = true;
            holder.stateView.setBackgroundColor(readColor);
        }
        holder.feedTextView.setText(cursor.getString(ENTRY_FEED_TITLE));
        holder.dateTextView.setText(timeFormat.format(cursor.getLong(ENTRY_UPDATED)));
        if (holder.headerTextView != null) {
            holder.headerTextView.setText(holder.section);
        }

        if (!cursor.isNull(ENTRY_FEED_FAVICON)) {
            // TODO: cursor.getString(ENTRY_FEED_FAVICON)
            holder.faviconImageView.setImageResource(R.drawable.favicon_placeholder);
        } else {
            holder.faviconImageView.setImageResource(R.drawable.favicon_placeholder);
        }
    }

    /**
     * Updates the section text if necessary.
     */
    public void updateSections() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (todayStart != calendar.getTimeInMillis()) {
            sections.clear();

            todayStart = calendar.getTimeInMillis();

            calendar.add(Calendar.DATE, -1);
            yesterdayStart = calendar.getTimeInMillis();

            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        String itemSection = getItemSection(position);

        if (position == 0) {
            return 1;
        }

        String previousItemSection = getItemSection(position - 1);
        if (!itemSection.equals(previousItemSection)) {
            return 1;
        }

        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (cursor.moveToPosition(position)) {
            return cursor.getLong(ENTRY_ID);
        }

        throw new IllegalArgumentException("Invalid position: " + position);
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    @Override
    public String getItemSection(int position) {
        String section = sections.get(position);

        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Invalid cursor position: " + position);
        }

        if (section == null) {
            section = getItemSection(cursor);
            sections.put(position, section);
        }
        return section;
    }

    private String getItemSection(Cursor cursor) {
        long updated = cursor.getLong(ENTRY_UPDATED);
        if (updated >= todayStart) {
            return context.getString(R.string.today);
        }
        if (updated >= yesterdayStart) {
            return context.getString(R.string.yesterday);
        }
        return dateFormat.format(updated);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private String section;
        private boolean read;

        final TextView titleTextView;
        final ImageView faviconImageView;
        final TextView feedTextView;
        final TextView dateTextView;
        final View stateView;
        final TextView headerTextView;

        final View entryView;

        public ViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.title);
            faviconImageView = (ImageView) itemView.findViewById(R.id.favicon);
            feedTextView = (TextView) itemView.findViewById(R.id.feed);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
            stateView = itemView.findViewById(R.id.state);
            headerTextView = (TextView) itemView.findViewById(R.id.header);

            entryView = itemView.findViewById(R.id.entry);
        }

        public String getSection() {
            return section;
        }

        public boolean isRead() {
            return read;
        }

    }

}
