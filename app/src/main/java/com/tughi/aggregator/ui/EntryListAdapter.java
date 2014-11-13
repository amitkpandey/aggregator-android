package com.tughi.aggregator.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * A {@link CursorAdapter} used to populate a {@link SectionListView} with feed entries.
 */
/*local*/ class EntryListAdapter extends CursorAdapter implements SectionListAdapter {

    public static final String[] ENTRY_PROJECTION = {
            EntryColumns.ID,
            EntryColumns.TITLE,
            EntryColumns.UPDATED,
            EntryColumns.FEED_TITLE,
            EntryColumns.FEED_FAVICON,
            EntryColumns.FLAG_READ,
    };
    public static final int ENTRY_TITLE_INDEX = 1;
    public static final int ENTRY_UPDATED_INDEX = 2;
    public static final int ENTRY_FEED_TITLE_INDEX = 3;
    public static final int ENTRY_FEED_FAVICON_INDEX = 4;
    public static final int ENTRY_FLAG_READ_INDEX = 5;

    private Context context;

    private DateFormat timeFormat;
    private DateFormat dateFormat;

    private Calendar calendar = Calendar.getInstance();
    private long todayStart;
    private long yesterdayStart;

    private SparseArray<String> sections = new SparseArray<String>(2000);

    public EntryListAdapter(Context context) {
        super(context, null, false);

        this.context = context;

        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layout;
        switch (getItemViewType(cursor.getPosition())) {
            case 1:
                layout = R.layout.entry_list_header_item;
                break;
            default:
                layout = R.layout.entry_list_item;
        }
        View view = LayoutInflater.from(context).inflate(layout, parent, false);

        ViewTag tag = new ViewTag();
        tag.titleTextView = (TextView) view.findViewById(R.id.title);
        tag.faviconImageView = (ImageView) view.findViewById(R.id.favicon);
        tag.feedTextView = (TextView) view.findViewById(R.id.feed);
        tag.dateTextView = (TextView) view.findViewById(R.id.date);
        tag.stateImageView = (ImageView) view.findViewById(R.id.state);
        tag.headerTextView = (TextView) view.findViewById(R.id.header);
        view.setTag(tag);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewTag tag = (ViewTag) view.getTag();
        tag.section = sections.get(cursor.getPosition());
        tag.titleTextView.setText(Html.fromHtml(cursor.getString(ENTRY_TITLE_INDEX)));
        int flagRead = cursor.getInt(ENTRY_FLAG_READ_INDEX);
        tag.titleTextView.setTypeface(flagRead == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tag.stateImageView.setImageLevel(flagRead);
        tag.feedTextView.setText(cursor.getString(ENTRY_FEED_TITLE_INDEX));
        tag.dateTextView.setText(timeFormat.format(cursor.getLong(ENTRY_UPDATED_INDEX)));
        if (tag.headerTextView != null) {
            tag.headerTextView.setText(tag.section);
        }

        if (!cursor.isNull(ENTRY_FEED_FAVICON_INDEX)) {
            Picasso.with(context)
                    .load(cursor.getString(ENTRY_FEED_FAVICON_INDEX))
                    .placeholder(R.drawable.favicon_placeholder)
                    .into(tag.faviconImageView);
        } else {
            tag.faviconImageView.setImageResource(R.drawable.favicon_placeholder);
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        sections.clear();

        updateSections();

        return super.swapCursor(newCursor);
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
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public String getItemSection(int position) {
        String section = sections.get(position);
        if (section == null) {
            section = getItemSection((Cursor) getItem(position));
            sections.put(position, section);
        }
        return section;
    }

    private String getItemSection(Cursor cursor) {
        long updated = cursor.getLong(ENTRY_UPDATED_INDEX);
        if (updated >= todayStart) {
            return context.getString(R.string.today);
        }
        if (updated >= yesterdayStart) {
            return context.getString(R.string.yesterday);
        }
        return dateFormat.format(updated);
    }

    private class ViewTag implements SectionTag {
        private String section;

        private TextView titleTextView;
        private ImageView faviconImageView;
        private TextView feedTextView;
        private TextView dateTextView;
        private ImageView stateImageView;
        private TextView headerTextView;

        @Override
        public String getSection() {
            return section;
        }
    }

}
