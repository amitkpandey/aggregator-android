package com.tughi.aggregator.feeds;

import android.support.annotation.NonNull;

import com.tughi.aggregator.io.StreamHelper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for {@link URLConnection} handling.
 */
public class ConnectionHelper {

    /**
     * Reads the connection response with encoding detection.
     */
    public static String load(URLConnection connection) throws IOException {
        // read the connection response
        byte[] content = StreamHelper.read(connection.getInputStream());

        // detect encoding
        String encoding = getContentTypeCharset(connection.getContentType());
        if (encoding == null) {
            encoding = getXmlEncoding(content);
            if (encoding == null) {
                encoding = getHtmlEncoding(content);
                if (encoding == null) {
                    // default to utf-8
                    encoding = "utf-8";
                }
            }
        }

        return new String(content, encoding);
    }

    private static final Pattern charsetPattern = Pattern.compile("charset=['\"]?([a-z0-9\\-\\+\\.:]+)");

    private static String getContentTypeCharset(String contentType) {
        String charset = null;

        if (contentType != null) {
            // look for charset in the content type
            Matcher matcher = charsetPattern.matcher(contentType.toLowerCase());
            if (matcher.find()) {
                charset = matcher.group(1);

                if ("none".equals(charset)) {
                    charset = null;
                }
            }
        }

        return charset;
    }

    private static final Pattern encodingPattern = Pattern.compile("encoding=['\"]?([a-z0-9\\-\\+\\.:]+)");

    private static String getXmlEncoding(byte[] content) throws IOException {
        String encoding = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content)));
        String firstLine = reader.readLine();
        if (firstLine != null && firstLine.startsWith("<?xml")) {
            // look for the encoding in the prolog
            Matcher matcher = encodingPattern.matcher(firstLine.toLowerCase());
            if (matcher.find()) {
                encoding = matcher.group(1);
            }
        }

        return encoding;
    }

    private static final Pattern metaCharsetPattern = Pattern.compile("<meta[^>]+charset=['\"]?([a-zA-Z0-9\\-\\+\\.:]+)[^>]*>|<body");

    private static String getHtmlEncoding(byte[] content) throws IOException {
        String encoding = null;

        ByteChars byteChars = new ByteChars(content);
        Matcher matcher = metaCharsetPattern.matcher(byteChars);
        if (matcher.find()) {
            encoding = matcher.group(1);
        }

        return encoding;
    }

    private static class ByteChars implements CharSequence {

        private byte[] bytes;
        private int start;
        private int end;

        public ByteChars(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }

        private ByteChars(byte[] bytes, int start, int end) {
            this.bytes = bytes;
            this.start = start;
            this.end = end;
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return (char) bytes[start + index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ByteChars(bytes, this.start + start, this.start + end);
        }

        @NonNull
        @Override
        public String toString() {
            try {
                return new String(bytes, start, end - start, "ISO-8859-1");
            } catch (UnsupportedEncodingException exception) {
                throw new IllegalStateException(exception);
            }
        }

    }

}
