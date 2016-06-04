package com.tughi.aggregator.feeds;

import com.tughi.xml.Document;
import com.tughi.xml.TagElement;
import com.tughi.xml.TextElement;
import com.tughi.xml.TypedTextElement;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Finds feeds based on HTML auto-discovery and XML parsing (RSS, Atom, OPML).
 */
public class FeedsFinder {

    private FeedsFinder() {
    }

    private static final Pattern sourcePattern = Pattern.compile("<(html|feed|rss|rdf:RDF|opml)");

    public static Result find(URLConnection connection) throws IOException {
        Result result = new FeedsFinder().new Result();

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            connection.setRequestProperty("User-Agent", "Aggregator Core");

            result.status = httpURLConnection.getResponseCode();
            result.url = httpURLConnection.getURL().toString();
            result.headers = httpURLConnection.getHeaderFields();

            if (result.status != HttpURLConnection.HTTP_OK) {
                // unexpected response code
                return result;
            }
        }

        String content = ConnectionHelper.load(connection);

        // detect source
        Matcher matcher = sourcePattern.matcher(content);
        if (matcher.find()) {
            result.source = matcher.group(1).toLowerCase();
        }

        if ("html".equals(result.source)) {
            findHtmlFeeds(content, result);
        } else if ("feed".equals(result.source) || "rss".equals(result.source)) {
            result.source = "xml";
            findXmlFeed(content, result);
        } else if ("opml".equals(result.source)) {
            findOpmlFeeds(content, result);
        }

        return result;
    }

    private static final Pattern linkPattern = Pattern.compile("<(body)|<link([^>]+)>");
    private static final Pattern alternatePattern = Pattern.compile("rel\\s*=\\s*['\"]alternate['\"]");
    private static final Pattern typePattern = Pattern.compile("type\\s*=\\s*['\"]application/(atom|rss)\\+xml['\"]");
    private static final Pattern hrefPattern = Pattern.compile("href\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern titlePattern = Pattern.compile("title\\s*=\\s*['\"]([^'\"]+)['\"]");

    private static void findHtmlFeeds(String content, Result result) {
        // find <head> <link>s
        Matcher linkMatcher = linkPattern.matcher(content);
        while (linkMatcher.find() && linkMatcher.group(1) == null) {
            String attributes = linkMatcher.group(2);
            // is it an alternate link?
            if (alternatePattern.matcher(attributes).find()) {
                Matcher typeMatcher = typePattern.matcher(attributes);
                // is an feed link?
                if (typeMatcher.find()) {
                    String type = typeMatcher.group(1);

                    Matcher hrefMatcher = hrefPattern.matcher(attributes);
                    // does the link have the required href?
                    if (hrefMatcher.find()) {
                        String href = hrefMatcher.group(1);

                        // get optional feed title
                        Matcher titleMatcher = titlePattern.matcher(attributes);
                        String title = titleMatcher.find() ? titleMatcher.group(1) : null;

                        // add feed
                        Result.Feed feed = result.new Feed();
                        feed.href = absoluteUrl(href, result.url);
                        feed.title = title;
                        feed.type = type;
                        result.feeds.add(feed);
                    }
                }
            }
        }
    }

    private static final Pattern baseUrlPattern = Pattern.compile("((https?|feed)://[^/]+).*");

    /**
     * Transforms the provided <b>href</b> into an absolute URL using the provided <b>url</b>.
     */
    static String absoluteUrl(String href, String url) {
        if (href.startsWith("/")) {
            if (href.startsWith("//")) {
                // protocol is missing

                return url.substring(0, url.indexOf("://") + 1) + href;
            }

            // full path
            Matcher baseUrlMatcher = baseUrlPattern.matcher(url);
            if (baseUrlMatcher.matches()) {
                return baseUrlMatcher.group(1) + href;
            }
        } else if (href.startsWith("http://") || href.startsWith("https://")) {
            // already absolute
            return href;
        } else if (href.startsWith("feed://")) {
            // also absolute but with the wrong protocol
            return href.replaceFirst("feed://", "http://");
        }

        // relative to url
        return url + (url.endsWith("/") ? "" : "/") + href;
    }

    private static void findXmlFeed(String content, final Result result) throws IOException {
        final Result.Feed feed = result.new Feed();

        Document document = new Document();
        final String[] rssNamespaces = {"", "http://purl.org/rss/1.0/"};

        // create RSS parser
        TagElement rssElement = new TagElement("rss") {
            @Override
            protected void start(String namespace, String name, Attributes attributes) {
                feed.type = "rss";
                feed.href = result.url;
            }
        };
        document.addChild(rssElement);

        TagElement rdfElement = new TagElement("RDF", "http://www.w3.org/1999/02/22-rdf-syntax-ns#") {
            @Override
            protected void start(String namespace, String name, Attributes attributes) {
                feed.type = "rss";
                feed.href = result.url;
            }
        };
        document.addChild(rdfElement);

        TagElement channelElement = new TagElement("channel", rssNamespaces);
        rssElement.addChild(channelElement);
        rdfElement.addChild(channelElement);

        channelElement.addChild(new TextElement("title", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                feed.title = text;
            }
        });

        TagElement itemElement = new TagElement("item", rssNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                throw new FinishedException();
            }
        };
        channelElement.addChild(itemElement);
        rdfElement.addChild(itemElement);

        // create Atom parser
        final String[] atomNamespaces = {"http://www.w3.org/2005/Atom", "http://purl.org/atom/ns#"};

        TagElement feedElement = new TagElement("feed", atomNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) {
                feed.type = "atom";
                feed.href = result.url;
            }
        };
        document.addChild(feedElement);

        feedElement.addChild(new TypedTextElement("title", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                feed.title = text;
            }
        });

        TagElement entryElement = new TagElement("entry", atomNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                throw new FinishedException();
            }
        };
        feedElement.addChild(entryElement);

        // parse XML
        DefaultHandler contentHandler = document.getContentHandler();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(new InputSource(new StringReader(content)), contentHandler);
        } catch (FinishedException exception) {
            result.feeds.add(feed);
        } catch (SAXException | ParserConfigurationException exception) {
            throw new IOException("parse failed", exception);
        }
    }

    private static void findOpmlFeeds(String content, final Result result) throws IOException {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(new InputSource(new StringReader(content)), new DefaultHandler() {
                private Stack<String> categories = new Stack<>();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if ("outline".equals(localName)) {
                        String title = attributes.getValue("title");

                        if ("rss".equals(attributes.getValue("type"))) {
                            String href = attributes.getValue("xmlUrl");
                            if (href != null) {
                                // add feed
                                Result.Feed feed = result.new Feed();
                                feed.title = title;
                                feed.href = href;
                                if (!categories.empty()) {
                                    feed.type = categories.peek();
                                }
                                result.feeds.add(feed);
                            }
                        }

                        categories.push(title);
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if ("outline".equals(localName)) {
                        categories.pop();
                    }
                }
            });
        } catch (SAXException | ParserConfigurationException exception) {
            throw new IOException("parse failed", exception);
        }
    }

    public class Result {

        public String url;
        public int status;
        public Map<String, List<String>> headers;
        public String source;
        public List<Feed> feeds = new LinkedList<>();

        @Override
        public String toString() {
            return "{url: '" + url + "', source: '" + source + "', feeds: " + feeds + "}";
        }

        public class Feed {
            public String href;
            public String title;
            public String type;

            @Override
            public String toString() {
                return "href: '" + href + "', type: '" + type + "', title: '" + title + "'";
            }
        }

    }

    private static class FinishedException extends SAXException {
    }

}
