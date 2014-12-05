package com.tughi.aggregator.ui;


import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.SyncLogColumns;

import java.util.ArrayList;

/**
 * A {@link Fragment} used to display the sync log.
 */
public class SyncLogFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_SYNC_LOG_URI = "sync_log_uri";

    private static final int STEP_TIME = 15 * 60 * 1000; // 15 minutes

    private static final int SYNC_LOG_LOADER = 1;
    private static final String SYNC_LOG_LOADER_FIRST_POLL = "first_poll";

    private static final String[] SYNC_LOG_PROJECTION = {
            SyncLogColumns.POLL,
            SyncLogColumns.ERROR,
            SyncLogColumns.ENTRIES_TOTAL,
            SyncLogColumns.ENTRIES_NEW,
    };
    private static final int SYNC_LOG_POLL = 0;
    private static final int SYNC_LOG_ERROR = 1;
    private static final int SYNC_LOG_ENTRIES_TOTAL = 2;
    private static final int SYNC_LOG_ENTRIES_NEW = 3;

    private Context context;

    private SyncLogView syncLogView;

    public void setScaleFactor(float scaleFactor) {
        if (syncLogView != null) {
            syncLogView.setScaleFactor(scaleFactor);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        context = activity.getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return syncLogView = new SyncLogView(context);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loader, Bundle args) {
        Uri syncLogUri = getArguments().getParcelable(ARG_SYNC_LOG_URI);
        return new CursorLoader(context, syncLogUri, SYNC_LOG_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            ArrayList<LogItem> logItemList = new ArrayList<LogItem>(cursor.getCount());
            LogItem logItem = new LogItem();

            do {
                long poll = cursor.getLong(SYNC_LOG_POLL);
                if (logItem.poll != poll) {
                    logItem = new LogItem();
                    logItem.poll = poll;
                    logItemList.add(logItem);
                }
                if (!cursor.isNull(SYNC_LOG_ERROR)) {
                    logItem.error = true;
                } else {
                    int entriesNew = cursor.getInt(SYNC_LOG_ENTRIES_NEW);
                    if (entriesNew == 0) {
                        logItem.empty = true;
                    } else {
                        float entriesTotal = cursor.getFloat(SYNC_LOG_ENTRIES_TOTAL);
                        logItem.success = Math.max(logItem.success, entriesNew / entriesTotal);
                    }
                }
            } while (cursor.moveToNext());

            syncLogView.setLogItems(logItemList.toArray(new LogItem[logItemList.size()]));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        syncLogView.setLogItems(null);
    }

    private class LogItem {
        private long poll;
        private boolean error;
        private boolean empty;
        private float success;
    }

    /**
     * Custom {@link View} used to draw the log.
     */
    private class SyncLogView extends View {

        private int stroke;
        private int step;

        private Paint successPaint;
        private int successPaintAlpha;
        private Paint emptyPaint;
        private Paint errorPaint;
        private int errorPaintAlpha;

        private LogItem[] logItems;

        private float scaleFactor = 1;

        public SyncLogView(Context context) {
            super(context);

            Resources resources = context.getResources();
            stroke = Math.round(resources.getDisplayMetrics().density * 2);
            if (stroke % 2 == 1) {
                stroke++;
            }

            step = stroke * 2;

            successPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            successPaint.setColor(resources.getColor(R.color.primary_dark));
            successPaintAlpha = successPaint.getAlpha();
            successPaint.setStrokeWidth(stroke);
            successPaint.setStrokeCap(Paint.Cap.ROUND);
            successPaint.setStyle(Paint.Style.FILL);

            emptyPaint = new Paint(successPaint);
            emptyPaint.setColor(resources.getColor(R.color.primary));

            errorPaint = new Paint(successPaint);
            errorPaint.setColor(resources.getColor(R.color.sync_error));
            errorPaintAlpha = errorPaint.getAlpha();
        }

        public void setLogItems(LogItem[] logItems) {
            this.logItems = logItems;
            invalidate();
        }

        public void setScaleFactor(float scaleFactor) {
            this.scaleFactor = scaleFactor;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (w > 0) {
                Bundle args = new Bundle();
                args.putLong(SYNC_LOG_LOADER_FIRST_POLL, System.currentTimeMillis() - w * STEP_TIME / syncLogView.step);
                getLoaderManager().restartLoader(SYNC_LOG_LOADER, args, SyncLogFragment.this);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (scaleFactor > 0) {
                successPaint.setAlpha(Math.max(0, Math.min(Math.round(scaleFactor * successPaintAlpha), 255)));
                errorPaint.setAlpha(Math.max(0, Math.min(Math.round(scaleFactor * errorPaintAlpha), 255)));

                final int width = getWidth();
                final int height = getHeight();
                final int bottom = height - 2 * stroke;
                canvas.drawLine(0, bottom, width, bottom, successPaint);

                long currentTime = System.currentTimeMillis();

                if (logItems != null) {
                    int size = logItems.length;
                    for (int index = 0; index < size; index++) {
                        LogItem logItem = logItems[index];
                        int x = width - (int) ((currentTime - logItem.poll) / (float) STEP_TIME * step) - stroke / 2;
                        if (logItem.success > 0) {
                            float y = Math.round(stroke * 2 + (bottom - stroke * 4) * logItem.success) * scaleFactor;
                            canvas.drawRect(x - stroke / 2, bottom - y, x + stroke / 2, bottom, successPaint);
                        }
                        if (logItem.error) {
                            canvas.drawRect(x - stroke, bottom - stroke, x + stroke, bottom + stroke, errorPaint);
                        }
                        if (logItem.empty) {
                            canvas.drawRect(x - stroke / 2, bottom - stroke / 2, x + stroke / 2, bottom + stroke / 2, emptyPaint);
                        }
                    }
                }
            }
        }

    }

}
