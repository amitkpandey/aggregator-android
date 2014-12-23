package com.tughi.aggregator.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.R;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.Uris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A {@link Fragment} that shows an entry in a {@link WebView}.
 */
public class ReaderEntryFragment extends Fragment {

    public static final String ARG_CURSOR_POSITION = "cursor_position";

    public static final String STATE_KEEP_UNREAD = "keep_unread";

    private static String entryHtml;

    private DataSetObserver dataSetObserver;

    private long entryId;
    private String entryTitle;
    private long entryUpdated;
    private boolean entryFlagRead;
    private boolean entryFlagStar;
    private String entryData;

    private boolean keepUnread;

    private WebView descriptionWebView;
    private ProgressBar progressBar;

    private MenuItem markReadMenuItem;
    private MenuItem markUnreadMenuItem;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
    }

    private Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int progress = message.arg1;
            if (progress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    public ReaderEntryFragment() {
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if (!menuVisible && descriptionWebView != null) {
            // reset the scroll state of the #aggregator-content element
            descriptionWebView.reload();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load template
        if (entryHtml == null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.entry)));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                entryHtml = builder.toString();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load entry template", exception);
            }
        }

        setHasOptionsMenu(true);

        final ReaderActivity activity = (ReaderActivity) getActivity();
        Log.i(getClass().getName(), "activity: " + activity);
        activity.adapter.registerDataSetObserver(dataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                Cursor cursor = activity.cursor;
                if (cursor.moveToPosition(getArguments().getInt(ARG_CURSOR_POSITION))) {
                    entryId = cursor.getLong(ReaderActivity.ENTRY_ID);
                    String newEntryTitle = cursor.getString(ReaderActivity.ENTRY_TITLE);
                    long newEntryUpdated = cursor.getLong(ReaderActivity.ENTRY_UPDATED);
                    boolean newEntryFlagRead = cursor.getInt(ReaderActivity.ENTRY_FLAG_READ) != 0;
                    boolean newEntryFlagStar = cursor.getInt(ReaderActivity.ENTRY_FLAG_STAR) != 0;
                    String newEntryData = cursor.getString(ReaderActivity.ENTRY_DATA);

                    if (newEntryUpdated != entryUpdated
                            || (newEntryTitle != null ? !newEntryTitle.equals(entryTitle) : entryTitle != null)
                            || (newEntryData != null ? !newEntryData.equals(entryData) : entryData != null)) {
                        entryTitle = newEntryTitle;
                        entryUpdated = newEntryUpdated;
                        entryData = newEntryData;
                        // TODO: parse JSON data

                        // show new content
                        String content = prepareContent();
                        String base64 = Base64.encodeToString(content.getBytes(), Base64.DEFAULT);
                        descriptionWebView.loadData(base64, "text/html; charset=utf-8", "base64");
                    }

                    if (newEntryFlagRead != entryFlagRead || newEntryFlagStar != entryFlagStar) {
                        entryFlagRead = newEntryFlagRead;
                        entryFlagStar = newEntryFlagStar;

                        activity.supportInvalidateOptionsMenu();
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            keepUnread = savedInstanceState.getBoolean(STATE_KEEP_UNREAD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.reader_entry_fragment, container, false);

        progressBar = (ProgressBar) fragmentView.findViewById(R.id.progress);

        WebView descriptionWebView = this.descriptionWebView = (WebView) fragmentView.findViewById(R.id.description);
        descriptionWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressHandler.sendMessage(progressHandler.obtainMessage(0, newProgress, 0));
            }
        });
        descriptionWebView.getSettings().setJavaScriptEnabled(true);
        descriptionWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        descriptionWebView.setBackgroundColor(getResources().getColor(R.color.reader_background));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                descriptionWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }

        registerForContextMenu(descriptionWebView);

        // force load
        ReaderActivity activity = (ReaderActivity) getActivity();
        if (activity.cursor != null) {
            dataSetObserver.onChanged();
        }

        return fragmentView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEEP_UNREAD, keepUnread);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ReaderActivity activity = (ReaderActivity) getActivity();
        activity.adapter.unregisterDataSetObserver(dataSetObserver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.reader_entry_fragment, menu);

        markReadMenuItem = menu.findItem(R.id.mark_read);
        markUnreadMenuItem = menu.findItem(R.id.mark_unread);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        markReadMenuItem.setVisible(!entryFlagRead);
        markUnreadMenuItem.setVisible(entryFlagRead);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemId = item.getItemId();
        switch (menuItemId) {
            case R.id.share:
                // TODO
                return true;
            case R.id.mark_read:
            case R.id.mark_unread:
                new UpdateFlagReadTask(getActivity()).execute(entryId, !(keepUnread = (menuItemId == R.id.mark_unread)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static final int DATE_FORMAT = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR;

    private String prepareContent() {
        return entryHtml
                .replace("{{ reader.theme }}", "dark")
                .replace("{{ entry.link }}", nullSafe("", "#")) // TODO: data['link']
                .replace("{{ entry.date }}", DateUtils.formatDateTime(getActivity(), entryUpdated, DATE_FORMAT))
                .replace("{{ entry.title }}", nullSafe(entryTitle, ""))
                .replace("{{ entry.content }}", nullSafe(entryData, "")); // TODO: data['content']
    }

    private String nullSafe(String value, String nullValue) {
        return value != null ? value : nullValue;
    }

    public void markRead(Context context) {
        if (entryId != 0 && !keepUnread && !entryFlagRead) {
            new UpdateFlagReadTask(context).execute(entryId, true);
        }
    }

    private static class UpdateFlagReadTask extends AsyncTask<Object, Void, Void> {

        private Context context;

        private UpdateFlagReadTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Object... params) {
            Long entryId = (Long) params[0];
            Boolean read = (Boolean) params[1];

            ContentResolver contentResolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(EntryColumns.FLAG_READ, read ? 1 : 0);
            contentResolver.update(Uris.newUserEntryUri(entryId), values, EntryColumns.FLAG_READ + " = " + (read ? 0 : 1), null);

            return null;
        }

    }

}
