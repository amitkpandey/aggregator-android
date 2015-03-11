-- the feed_sync table is updated by the sync service
CREATE TABLE feed_sync (
    _id INTEGER PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT,
    link TEXT,
    etag TEXT,
    modified TEXT
);

-- the feed_user table is updated by the user
CREATE TABLE feed_user (
    _id INTEGER UNIQUE NOT NULL,
    title TEXT,
    favicon TEXT,
    update_mode INTEGER NOT NULL DEFAULT 0,
    next_sync INTEGER NOT NULL DEFAULT 0
);

-- a trigger that inserts a new feed_user row for each new feed_sync
CREATE TRIGGER after_insert_feed_sync
    AFTER INSERT ON feed_sync
    BEGIN
        INSERT INTO feed_user (_id) VALUES (NEW._id);
    END;

-- a trigger that deletes the feed data when a feed_sync is deleted
CREATE TRIGGER after_delete_feed_sync
    AFTER DELETE ON feed_sync
    BEGIN
        DELETE FROM feed_user WHERE _id = OLD._id;
        DELETE FROM entry_sync WHERE feed_id = OLD._id;
        DELETE FROM sync_log WHERE feed_id = OLD._id;
    END;

-- the entry_sync table is updated by the sync service
CREATE TABLE entry_sync (
    feed_id INTEGER NOT NULL,
    guid TEXT NOT NULL,
    title TEXT,
    updated INTEGER,
    poll INTEGER NOT NULL,
    data TEXT NOT NULL,
    UNIQUE (feed_id, guid),
    FOREIGN KEY (feed_id) REFERENCES feed (_id)
);

-- the entry_user table is updated by the user
CREATE TABLE entry_user (
    _id INTEGER PRIMARY KEY,
    feed_id INTEGER NOT NULL,
    guid TEXT NOT NULL,
    poll INTEGER NOT NULL,
    flag_read INTEGER NOT NULL DEFAULT 0,
    flag_star INTEGER NOT NULL DEFAULT 0,
    flag_junk INTEGER NOT NULL DEFAULT 0,
    ro_flag_read INTEGER NOT NULL DEFAULT 0,
    UNIQUE (feed_id, guid),
    FOREIGN KEY (feed_id) REFERENCES feed (_id)
);

-- a trigger that inserts a new entry_user row for each new entry_sync
CREATE TRIGGER after_insert_entry_sync
    AFTER INSERT ON entry_sync
    BEGIN
        INSERT INTO entry_user (feed_id, guid, poll) VALUES (NEW.feed_id, NEW.guid, NEW.poll);
    END;

-- a trigger that deletes the associated entry_user when an entry_sync is deleted
CREATE TRIGGER after_delete_entry_sync
    AFTER DELETE ON entry_sync
    BEGIN
        DELETE FROM entry_user WHERE feed_id = OLD.feed_id AND guid = OLD.guid;
    END;

-- a view that merges entry_sync and entry_user
CREATE VIEW entry_view AS
    SELECT
        entry_user._id AS _id,
        entry_user.feed_id AS feed_id,
        entry_user.guid AS guid,
        entry_sync.title AS title,
        COALESCE(entry_sync.updated, entry_user.poll) AS updated,
        entry_sync.data AS data,
        entry_user.flag_read AS flag_read,
        entry_user.flag_star AS flag_star,
        entry_user.ro_flag_read AS ro_flag_read,
        COALESCE(feed_user.title, feed_sync.title) AS feed_title,
        feed_user.favicon AS feed_favicon
    FROM
        entry_user,
        entry_sync,
        feed_user,
        feed_sync
    WHERE
        entry_user.flag_junk = 0 AND
        entry_user.feed_id = entry_sync.feed_id AND
        entry_user.guid = entry_sync.guid AND
        entry_user.feed_id = feed_user._id AND
        feed_user._id = feed_sync._id;

-- a view that includes 'all' and 'starred' virtual feed
CREATE VIEW feed_view AS
    SELECT
        -1 AS _id,
        1 AS _type,
        NULL AS url,
        'ALL' AS title,
        NULL AS link,
        NULL AS favicon,
        NULL AS etag,
        NULL AS modified,
        NULL AS update_mode,
        NULL AS next_sync,
        (SELECT COUNT(1) FROM entry_view WHERE flag_read = 0) AS unread_count
    UNION
        SELECT
            -2 AS _id,
            2 AS _type,
            NULL AS url,
            'STARRED' AS title,
            NULL AS link,
            NULL AS favicon,
            NULL AS etag,
            NULL AS modified,
            NULL AS update_mode,
            NULL AS next_sync,
            (SELECT COUNT(1) FROM entry_view WHERE flag_star = 1) AS unread_count
        UNION
            SELECT
                feed_sync._id AS _id,
                3 AS _type,
                feed_sync.url AS url,
                COALESCE(feed_user.title, feed_sync.title) AS title,
                feed_sync.link AS link,
                feed_user.favicon AS favicon,
                feed_sync.etag AS etag,
                feed_sync.modified AS modified,
                feed_user.update_mode AS update_mode,
                feed_user.next_sync AS next_sync,
                (SELECT COUNT(1) FROM entry_view WHERE entry_view.feed_id = feed_sync._id AND flag_read = 0) AS unread_count
            FROM
                feed_user,
                feed_sync
            WHERE
                feed_user._id = feed_sync._id
            ORDER BY _type, title;

-- a table that tracks all feed syncs
CREATE TABLE sync_log (
    feed_id INTEGER NOT NULL,
    poll INTEGER NOT NULL,
    poll_delta INTEGER,
    error TEXT,
    entries_total INTEGER,
    entries_new INTEGER,
    UNIQUE (feed_id, poll)
);

-- a trigger that updates the entries_new column for each new sync_log
CREATE TRIGGER after_insert_sync_log
    AFTER INSERT ON sync_log
    BEGIN
        UPDATE sync_log
            SET
                entries_new = (SELECT COUNT(1) FROM entry_user es WHERE es.feed_id = NEW.feed_id AND es.poll = NEW.poll),
                poll_delta = NEW.poll - (SELECT sl.poll FROM sync_log sl WHERE sl.feed_id = NEW.feed_id AND sl.poll < NEW.poll AND sl.error IS NULL ORDER BY sl.poll DESC LIMIT 1)
            WHERE feed_id = NEW.feed_id AND poll = NEW.poll;
    END;

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://android-developers.blogspot.com/feeds/posts/default');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://feeds.arstechnica.com/arstechnica/index/');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://www.autoblog.com/rss.xml');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://www.bonjourmadame.fr/rss');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://daringfireball.net/feeds/main');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('https://dribbble.com/tags/android.rss');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('https://news.ycombinator.com/rss');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://www.questionablecontent.net/QCRSS.xml');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://rss.slashdot.org/Slashdot/slashdot');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://www.theverge.com/rss/index.xml');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('http://www.tughi.com/feed');

-- add test feed
INSERT INTO feed_sync (url) VALUES ('https://github.com/tughi.atom');
