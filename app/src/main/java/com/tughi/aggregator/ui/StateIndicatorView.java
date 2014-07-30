package com.tughi.aggregator.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * A {@link View} that draws a circle in the desired color.
 */
public class StateIndicatorView extends View {

    private final Paint paint;

    private final int width;
    private final int height;

    private final float radius;
    private final float cx;
    private final float cy;

    public StateIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float density = getResources().getDisplayMetrics().density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#336699"));
        paint.setStrokeWidth(1.5f * density);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        int[] textAttrs = {android.R.attr.textSize};
        TypedArray typedArray = context.obtainStyledAttributes(null, textAttrs, 0, android.R.style.TextAppearance_Small);
        paint.setTextSize(typedArray.getDimension(0, 14 * density));
        typedArray.recycle();

        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        width = fontMetrics.descent - fontMetrics.ascent;
        height = fontMetrics.bottom - fontMetrics.top;

        radius = width / 2 - paint.getStrokeWidth() * 1.5f;
        cx = width / 2;
        cy = width / 2 - fontMetrics.top + fontMetrics.ascent;
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setState(int state) {
        paint.setStyle(state == 0 ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(cx, cy, radius, paint);
    }

}
