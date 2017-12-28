package com.tughi.aggregator.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tughi.aggregator.BuildConfig;

/**
 * An {@link ImageView} that adjusts its baseline based on the specified text appearance.
 */
public class InlineImageView extends AppCompatImageView {

    private Paint.FontMetricsInt fontMetrics;

    public InlineImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray;

        // get text appearance resource
        typedArray = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.textAppearance });
        int textAppearance = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        if (BuildConfig.DEBUG) {
            if (textAppearance == 0) {
                throw new IllegalStateException("Missing 'android:textAppearance' attribute");
            }
        }

        // get text size
        typedArray = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.textSize }, 0, textAppearance);
        float textSize = typedArray.getDimension(0, -1);
        typedArray.recycle();

        // get font metrics
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        fontMetrics = paint.getFontMetricsInt();
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        int height = params.height;

        if (BuildConfig.DEBUG) {
            // safe check
            if (height == ViewGroup.LayoutParams.MATCH_PARENT || height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                throw new IllegalArgumentException("The provided height must be exact");
            }
        }

        int baseline = Math.round((height - fontMetrics.descent + fontMetrics.ascent) / 2.f - fontMetrics.ascent);
        setBaseline(baseline);

        super.setLayoutParams(params);
    }

}
