package com.tughi.aggregator.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tughi.aggregator.BuildConfig;

/**
 * An {@link ImageView} that can align itself with a {@link TextView} by providing a baseline.
 */
public class BaselinedImageView extends ImageView {

    private static final int[] ATTRS = {
            android.R.attr.textAppearance,
            android.R.attr.textSize
    };
    private static final int ATTR_TEXT_APPEARANCE = 0;
    private static final int ATTR_TEXT_SIZE = 1;

    private Paint.FontMetricsInt fontMetrics;

    public BaselinedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray;

        // get text appearance resource
        typedArray = context.obtainStyledAttributes(attrs, ATTRS);
        int textAppearance = typedArray.getResourceId(ATTR_TEXT_APPEARANCE, 0);
        typedArray.recycle();

        if (BuildConfig.DEBUG) {
            if (textAppearance == 0) {
                throw new IllegalStateException("Missing 'android:textAppearance' attribute");
            }
        }

        // get text size
        typedArray = context.obtainStyledAttributes(null, ATTRS, 0, textAppearance);
        float textSize = typedArray.getDimension(ATTR_TEXT_SIZE, -1);
        typedArray.recycle();

        // get font metrics
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        fontMetrics = paint.getFontMetricsInt();
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);

        int height = params.height;

        if (height == ViewGroup.LayoutParams.MATCH_PARENT || height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            throw new IllegalArgumentException("The provided height must be exact");
        }

        setBaseline((height - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top);
    }

}
