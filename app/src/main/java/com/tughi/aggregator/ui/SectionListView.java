package com.tughi.aggregator.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tughi.aggregator.R;

/**
 * A {@link ListView} that displays the top section title on top of the list.
 */
public class SectionListView extends ListView {

    private View headerView;
    private TextView headerTextView;
    private SectionListAdapter adapter;

    public SectionListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        headerView = LayoutInflater.from(context).inflate(R.layout.entry_list_header, null);
        headerTextView = (TextView) headerView.findViewById(R.id.header);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        this.adapter = (SectionListAdapter) adapter;
        super.setAdapter(adapter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w > 0) {
            headerView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.UNSPECIFIED));
            headerView.layout(0, 0, headerView.getMeasuredWidth(), headerView.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // draw current section overlay
        int childCount = getChildCount();
        if (childCount > 1) {
            View firstVisibleItem = getChildAt(0);
            String firstVisibleItemSection = ((SectionTag) firstVisibleItem.getTag()).getSection();

            View secondVisibleItem = getChildAt(1);
            String secondVisibleItemSection = ((SectionTag) secondVisibleItem.getTag()).getSection();

            // update overlay text
            headerTextView.setText(firstVisibleItemSection);

            // draw
            canvas.save();
            if (!firstVisibleItemSection.equals(secondVisibleItemSection)) {
                // clip overlay to current section
                canvas.clipRect(headerView.getLeft(), headerView.getTop(), headerView.getRight(), firstVisibleItem.getBottom());
            }
            headerView.draw(canvas);
            canvas.restore();
        }
    }

}
