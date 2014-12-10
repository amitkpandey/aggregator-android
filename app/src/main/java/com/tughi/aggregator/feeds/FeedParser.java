package com.tughi.aggregator.feeds;

import com.tughi.xml.Document;
import com.tughi.xml.TagElement;
import com.tughi.xml.TextElement;
import com.tughi.xml.TypedTextElement;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Universal parser for RSS and Atom feeds.
 */
public final class FeedParser {

    private Result result = new Result();

    private Result.Feed.Entry currentEntry;

    private DateParser dateParser = DateParser.newInstance();

    private final MessageDigest md5Digester;

    private FeedParser() {
        try {
            md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Parses the feed provided via its <b>url</b>.
     */
    public static Result parse(String url) throws FeedParserException {
        return new FeedParser().parseUrl(url);
    }

    private Result parseUrl(String url) throws FeedParserException {
        try {
            URLConnection connection = new URL(url).openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

                result.status = httpURLConnection.getResponseCode();
                result.url = httpURLConnection.getURL().toString();
                result.headers = httpURLConnection.getHeaderFields();

                if (result.status != HttpURLConnection.HTTP_OK) {
                    // unexpected response code
                    return result;
                }
            }

            String content = ConnectionHelper.load(connection);

            // create SAX parser
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();

            Document document = new Document();
            createRssElements(document);
            createAtomElements(document);
            DefaultHandler contentHandler = document.getContentHandler();

            // parse XML
            saxParser.parse(new InputSource(new StringReader(content)), contentHandler);

        } catch (Exception exception) {
            throw new FeedParserException("parse failed", exception);
        }

        return result;
    }


    private void createRssElements(Document document) {
        final String[] rssNamespaces = {"", "http://purl.org/rss/1.0/"};

        TagElement rssElement = new TagElement("rss");
        document.addChild(rssElement);

        TagElement rdfElement = new TagElement("RDF", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        document.addChild(rdfElement);

        TagElement channelElement = new TagElement("channel", rssNamespaces);
        rssElement.addChild(channelElement);
        rdfElement.addChild(channelElement);

        channelElement.addChild(new TextElement("title", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleFeedTitle(text);
            }
        });

        channelElement.addChild(new TextElement("link", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleFeedLink(text);
            }
        });

        TagElement itemElement = new TagElement("item", rssNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                handleEntryStart();
            }

            @Override
            protected void end(String namespace, String name) throws SAXException {
                handleEntryEnd();
            }
        };
        channelElement.addChild(itemElement);
        rdfElement.addChild(itemElement);

        itemElement.addChild(new TextElement("title", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryTitle(text);
            }
        });

        itemElement.addChild(new TextElement("link", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryLink(text);
            }
        });

        itemElement.addChild(new TextElement("guid", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryId(text);
            }
        });

        itemElement.addChild(new TextElement("description", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntrySummary(text);
            }
        });

        itemElement.addChild(new TextElement("encoded", "http://purl.org/rss/1.0/modules/content/") {
            @Override
            protected void handleText(String text) {
                handleEntryContent(text, null);
            }
        });

        itemElement.addChild(new TextElement("author", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorStart();
                handleEntryAuthorName(text);
            }
        });

        itemElement.addChild(new TextElement("creator", "http://purl.org/dc/elements/1.1/") {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorStart();
                handleEntryAuthorName(text);
            }
        });

        itemElement.addChild(new TextElement("created", "http://purl.org/dc/terms/") {
            @Override
            protected void handleText(String text) {
                handleEntryCreated(text);
            }
        });

        itemElement.addChild(new TextElement("pubDate", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryPublished(text);
            }
        });

        itemElement.addChild(new TextElement("issued", "http://purl.org/dc/terms/") {
            @Override
            protected void handleText(String text) {
                handleEntryPublished(text);
            }
        });

        itemElement.addChild(new TextElement("date", "http://purl.org/dc/elements/1.1/") {
            @Override
            protected void handleText(String text) {
                handleEntryUpdated(text);
            }
        });

        itemElement.addChild(new TextElement("modified", "http://purl.org/dc/terms/") {
            @Override
            protected void handleText(String text) {
                handleEntryUpdated(text);
            }
        });
    }

    private void createAtomElements(Document document) {
        final String[] atomNamespaces = {"http://www.w3.org/2005/Atom", "http://purl.org/atom/ns#"};

        TagElement feedElement = new TagElement("feed", atomNamespaces);
        document.addChild(feedElement);

        feedElement.addChild(new TypedTextElement("title", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                handleFeedTitle(text);
            }
        });

        feedElement.addChild(new AtomLinkElement("link", atomNamespaces) {
            @Override
            public void handleLink(String rel, String href) {
                if ("alternate".equals(rel)) {
                    handleFeedLink(href);
                }
            }
        });

        TagElement entryElement = new TagElement("entry", atomNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                handleEntryStart();
            }

            @Override
            protected void end(String namespace, String name) throws SAXException {
                handleEntryEnd();
            }
        };
        feedElement.addChild(entryElement);

        entryElement.addChild(new TypedTextElement("title", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                handleEntryTitle(text);
            }
        });

        entryElement.addChild(new AtomLinkElement("link", atomNamespaces) {
            @Override
            public void handleLink(String rel, String href) {
                if ("alternate".equals(rel)) {
                    handleEntryLink(href);
                }
            }
        });

        entryElement.addChild(new TypedTextElement("id", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                handleEntryId(text);
            }
        });

        entryElement.addChild(new TypedTextElement("summary", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                handleEntrySummary(text);
            }
        });

        entryElement.addChild(new TypedTextElement("content", atomNamespaces) {
            @Override
            protected void handleText(String text, String type) {
                handleEntryContent(text, type);
            }
        });

        TagElement authorElement = new TagElement("author", atomNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                handleEntryAuthorStart();
            }
        };
        entryElement.addChild(authorElement);

        authorElement.addChild(new TextElement("name", atomNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorName(text);
            }
        });

        authorElement.addChild(new TextElement("url", atomNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorUrl(text);
            }
        });

        authorElement.addChild(new TextElement("uri", atomNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorUrl(text);
            }
        });

        authorElement.addChild(new TextElement("email", atomNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryAuthorEmail(text);
            }
        });

        entryElement.addChild(new TextElement("created", "http://purl.org/atom/ns#") {
            @Override
            protected void handleText(String text) {
                handleEntryCreated(text);
            }
        });

        entryElement.addChild(new TextElement("published", "http://www.w3.org/2005/Atom") {
            @Override
            protected void handleText(String text) {
                handleEntryPublished(text);
            }
        });

        entryElement.addChild(new TextElement("issued", "http://purl.org/atom/ns#") {
            @Override
            protected void handleText(String text) {
                handleEntryPublished(text);
            }
        });

        entryElement.addChild(new TextElement("updated", "http://www.w3.org/2005/Atom") {
            @Override
            protected void handleText(String text) {
                handleEntryUpdated(text);
            }
        });

        entryElement.addChild(new TextElement("modified", "http://purl.org/atom/ns#") {
            @Override
            protected void handleText(String text) {
                handleEntryUpdated(text);
            }
        });
    }

    private void handleFeedTitle(String title) {
        result.feed.title = title;
    }

    private void handleFeedLink(String link) {
        result.feed.link = link;
    }

    private void handleEntryStart() {
        currentEntry = result.feed.new Entry();
    }

    private void handleEntryEnd() {
        // make sure update points to a date
        if (currentEntry.updated == null) {
            currentEntry.updated = currentEntry.published;
            currentEntry.updatedTimestamp = currentEntry.publishedTimestamp;

            if (currentEntry.updated == null) {
                currentEntry.updated = currentEntry.created;
                currentEntry.updatedTimestamp = currentEntry.createdTimestamp;
            }
        }

        // make sure there is an ID
        if (currentEntry.id == null) {
            currentEntry.id = generateEntryId(currentEntry);
        }

        result.feed.entries.add(currentEntry);
    }

    private static final char[] HEX_MAP = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private String generateEntryId(Result.Feed.Entry entry) {
        md5Digester.reset();

        String title = entry.title;
        if (title != null) {
            md5Digester.update(title.getBytes());
        }

        String link = entry.link;
        if (link != null) {
            md5Digester.update(link.getBytes());
        }

        byte[] md5 = md5Digester.digest();

        StringBuilder id = new StringBuilder("AGGREGATOR::".length() + md5.length * 2);
        id.append("AGGREGATOR::");
        for (byte value : md5) {
            id.append(HEX_MAP[(value >> 4) & 0xf]);
            id.append(HEX_MAP[value & 0xf]);
        }

        return id.toString();
    }

    private void handleEntryTitle(String title) {
        currentEntry.title = title;
    }

    private void handleEntryLink(String link) {
        currentEntry.link = link;
    }

    private void handleEntryId(String id) {
        currentEntry.id = id;
    }

    private void handleEntrySummary(String summary) {
        currentEntry.summary = currentEntry.new Content();
        currentEntry.summary.value = summary;
    }

    private void handleEntryContent(String value, String type) {
        Result.Feed.Entry.Content content = currentEntry.new Content();
        content.value = value;
        content.type = type;
        currentEntry.contents.add(content);
    }

    private void handleEntryAuthorStart() {
        currentEntry.author = currentEntry.new Author();
    }

    private void handleEntryAuthorName(String name) {
        // TODO: use regex to search for email address
        currentEntry.author.name = name;
    }

    private void handleEntryAuthorUrl(String url) {
        currentEntry.author.url = url;
    }

    private void handleEntryAuthorEmail(String email) {
        currentEntry.author.email = email;
    }

    private void handleEntryCreated(String created) {
        currentEntry.created = created;
        currentEntry.createdTimestamp = dateParser.parse(created).getTime();
    }

    private void handleEntryPublished(String published) {
        currentEntry.published = published;
        currentEntry.publishedTimestamp = dateParser.parse(published).getTime();
    }

    private void handleEntryUpdated(String updated) {
        currentEntry.updated = updated;
        currentEntry.updatedTimestamp = dateParser.parse(updated).getTime();

    }

    private abstract class AtomLinkElement extends TagElement {

        public AtomLinkElement(String name, String... namespaces) {
            super(name, namespaces);
        }

        @Override
        protected void start(String namespace, String name, Attributes attributes) throws SAXException {
            handleLink(attributes.getValue("rel"), attributes.getValue("href"));
        }

        public abstract void handleLink(String rel, String href);

    }

    public class Result {

        public String url;
        public int status;
        public Map<String, List<String>> headers;
        public Feed feed = new Feed();

        public class Feed {

            public String title;
            public String link;
            public List<Entry> entries = new LinkedList<Entry>();

            public class Entry {

                public String title;
                public String link;
                public String id;
                public Author author;
                public Content summary;
                public List<Content> contents = new LinkedList<Content>();
                public String created;
                public Long createdTimestamp;
                public String published;
                public Long publishedTimestamp;
                public String updated;
                public Long updatedTimestamp;

                public class Author {
                    public String name;
                    public String url;
                    public String email;
                }

                public class Content {
                    public String value;
                    public String type;
                }
            }
        }
    }

}
