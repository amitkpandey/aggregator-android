package com.tughi.aggregator.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.tughi.aggregator.R;

/**
 * A {@link ViewGroup} that centers its only child on a text line.
 */
public class SpecialTextView extends ViewGroup {

    private final int lineHeight;
    private final int lineCenter;

    public SpecialTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray;

        // get text appearance resource
        typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpecialTextView, 0, 0);
        int textAppearance = typedArray.getResourceId(R.styleable.SpecialTextView_android_textAppearance, 0);
        typedArray.recycle();

        // get text size
        int[] textAttrs = {android.R.attr.textSize};
        typedArray = context.obtainStyledAttributes(null, textAttrs, 0, textAppearance);
        float textSize = typedArray.getDimension(0, -1);
        typedArray.recycle();

        // get font metrics
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        lineHeight = fontMetrics.bottom - fontMetrics.top;
        lineCenter = fontMetrics.descent / 2 + fontMetrics.ascent / 2 - fontMetrics.top;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View childView = getChildAt(0);
        childView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        int childWidth = childView.getMeasuredWidth();
        int childHeight = childView.getMeasuredHeight();

        if (childHeight > lineHeight) {
            throw new IllegalStateException("Child view is taller than the line height");
        }

        setMeasuredDimension(childWidth, lineHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View childView = getChildAt(0);

        int childHeight = childView.getMeasuredHeight();
        childView.layout(0, lineCenter - childHeight / 2, right - left, lineCenter + childHeight / 2);
    }

}
