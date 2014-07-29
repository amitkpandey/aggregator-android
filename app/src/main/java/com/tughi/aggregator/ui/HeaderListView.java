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
public class HeaderListView extends ListView {

    private View headerView;
    private TextView headerTextView;
    private HeaderListAdapter adapter;

    public HeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        headerView = LayoutInflater.from(context).inflate(R.layout.entry_list_header, null);
        headerTextView = (TextView) headerView.findViewById(R.id.header);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        this.adapter = (HeaderListAdapter) adapter;
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

        if (adapter != null) {
            // draw header
            headerTextView.setText(adapter.getItemHeader(getFirstVisiblePosition()));
            headerView.draw(canvas);
        }
    }

}
