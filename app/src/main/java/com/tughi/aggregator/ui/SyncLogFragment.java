package com.tughi.aggregator.ui;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tughi.aggregator.R;
import com.tughi.aggregator.content.SyncLogColumns;
import com.tughi.aggregator.content.Uris;

import java.util.ArrayList;

/**
 * A {@link Fragment} used to display the sync log.
 */
public class SyncLogFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

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

    private Uri syncLogUri = Uris.newFeedsSyncLogUri();

    private SyncLogView syncLogView;

    public void setSyncLogUri(Uri syncLogUri) {
        this.syncLogUri = syncLogUri;

        Bundle args = new Bundle();
        args.putLong(SYNC_LOG_LOADER_FIRST_POLL, System.currentTimeMillis() - syncLogView.getWidth() * STEP_TIME / syncLogView.step);
        getLoaderManager().restartLoader(SYNC_LOG_LOADER, args, this);
    }

    public void setScaleFactor(float scaleFactor) {
        syncLogView.setScaleFactor(scaleFactor);
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
        return new CursorLoader(context, syncLogUri, SYNC_LOG_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            ArrayList<LogItem> logItemList = new ArrayList<LogItem>(cursor.getCount());

            do {
                LogItem logItem = new LogItem();
                logItem.poll = cursor.getLong(SYNC_LOG_POLL);
                logItem.error = cursor.getString(SYNC_LOG_ERROR);
                logItem.entriesTotal = cursor.getInt(SYNC_LOG_ENTRIES_TOTAL);
                logItem.entriesNew = cursor.getInt(SYNC_LOG_ENTRIES_NEW);
                logItemList.add(logItem);
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
        private String error;
        private long entriesNew;
        private long entriesTotal;
    }

    /**
     * Custom {@link View} used to draw the log.
     */
    private class SyncLogView extends View {

        private int step;

        private Paint logPaint;
        private Paint errorPaint;

        private LogItem[] logItems;

        private float scaleFactor = 1;

        public SyncLogView(Context context) {
            super(context);

            Resources resources = context.getResources();
            int strokeWidth = Math.round(resources.getDisplayMetrics().density * 2);
            if (strokeWidth % 2 == 1) {
                strokeWidth++;
            }

            step = strokeWidth + strokeWidth / 2;

            logPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            logPaint.setColor(resources.getColor(R.color.sync_log));
            logPaint.setStrokeWidth(strokeWidth);
            logPaint.setStrokeCap(Paint.Cap.ROUND);

            errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            errorPaint.setColor(resources.getColor(R.color.sync_error));
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
                setSyncLogUri(syncLogUri);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            long currentTime = System.currentTimeMillis();

            int width = getWidth();
            int height = getHeight();
            canvas.drawLine(0, height / 2, width, height / 2, logPaint);

            if (logItems != null && scaleFactor > 0) {
                int size = logItems.length;
                for (int index = 0; index < size; index++) {
                    LogItem logItem = logItems[index];
                    int x = width - (int) ((currentTime - logItem.poll) / (float) STEP_TIME * step) - (int) logPaint.getStrokeWidth() / 2;
                    if (logItem.error == null) {
                        float y = (logItem.entriesNew * height / logItem.entriesTotal) / 2 * scaleFactor;
                        canvas.drawLine(x, height / 2 - y, x, height / 2 + y, logPaint);
                    } else {
                        canvas.drawCircle(x, height / 2, step / 2, errorPaint);
                    }
                }
            }
        }

    }

}
