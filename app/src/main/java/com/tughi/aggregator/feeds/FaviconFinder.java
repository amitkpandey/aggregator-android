package com.tughi.aggregator.feeds;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the favicon of a site based on HTML auto-discovery.
 */
public class FaviconFinder {

    private FaviconFinder() {
    }

    public static Result find(String url) throws IOException {
        Result result = new FaviconFinder().new Result();

        URLConnection connection = new URL(url).openConnection();
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestProperty("Accept", "text/html");

            result.status = httpURLConnection.getResponseCode();

            if (result.status != HttpURLConnection.HTTP_OK) {
                // unexpected response code
                return result;
            }

            result.url = httpURLConnection.getURL().toString();
        }

        String content = ConnectionHelper.load(connection);

        if (!findFavicon(content, result)) {
            findBaseUrlFavicon(url, result);
        }

        return result;
    }

    private static final Pattern baseUrlPattern = Pattern.compile("(https?://[^/]+).*");

    private static void findBaseUrlFavicon(String url, Result result) throws IOException {
        Matcher matcher = baseUrlPattern.matcher(url);
        if (matcher.matches()) {
            String faviconUrl = matcher.group(1) + "/favicon.ico";

            HttpURLConnection connection = (HttpURLConnection) new URL(faviconUrl).openConnection();
            connection.setRequestMethod("HEAD");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // found it
                result.faviconUrl = faviconUrl;
            }
        }
    }

    private static final Pattern linkPattern = Pattern.compile("<(body)|<link([^>]+)>");
    private static final Pattern iconPattern = Pattern.compile("rel\\s*=\\s*['\"]([sS]hortcut [iI]con|icon)['\"]");
    private static final Pattern hrefPattern = Pattern.compile("href\\s*=\\s*['\"]([^'\"]+)['\"]");

    private static boolean findFavicon(String content, Result result) {
        // find <head> <link>s
        Matcher linkMatcher = linkPattern.matcher(content);
        while (linkMatcher.find() && linkMatcher.group(1) == null) {
            String attributes = linkMatcher.group(2);
            // is it an icon link?
            if (iconPattern.matcher(attributes).find()) {
                Matcher hrefMatcher = hrefPattern.matcher(attributes);
                // does the link have the required href?
                if (hrefMatcher.find()) {
                    String href = hrefMatcher.group(1);

                    // TODO: get favicon size

                    // add feed
                    result.faviconUrl = FeedsFinder.absoluteUrl(href, result.url);
                }
            }
        }

        return result.faviconUrl != null;
    }

    public class Result {

        private String url;
        public int status;

        public String faviconUrl;

    }

}
