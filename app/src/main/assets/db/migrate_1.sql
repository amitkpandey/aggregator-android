-- the feeds table
CREATE TABLE feed (
    _id INTEGER PRIMARY KEY,
    url TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    favicon TEXT,
    etag TEXT,
    modified TEXT,
    poll INTEGER NOT NULL DEFAULT 0,
    poll_status INTEGER,
    poll_type TEXT,
    next_poll INTEGER NOT NULL DEFAULT 0
);

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
    flag_read INTEGER NOT NULL DEFAULT 0,
    flag_star INTEGER NOT NULL DEFAULT 0,
    UNIQUE (feed_id, guid),
    FOREIGN KEY (feed_id) REFERENCES feed (_id)
);

-- a trigger that inserts a new entry_user row for each new entry_sync
CREATE TRIGGER create_entry_user
    AFTER INSERT ON entry_sync
    BEGIN
        INSERT INTO entry_user (feed_id, guid) VALUES (NEW.feed_id, NEW.guid);
    END;

-- a trigger that deletes the associated entry_user when an entry_sync is deleted
CREATE TRIGGER delete_entry_user
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
        entry_sync.updated AS updated,
        entry_sync.poll AS poll,
        entry_sync.data AS data,
        entry_user.flag_read AS flag_read,
        entry_user.flag_star AS flag_star
    FROM entry_user, entry_sync
    WHERE
        entry_user.feed_id = entry_sync.feed_id AND
        entry_user.guid = entry_sync.guid;

-- a view that includes 'all' and 'starred' virtual feed
CREATE VIEW feed_view AS
    SELECT
        -1 AS _id,
        NULL AS url,
        'ALL' AS title,
        NULL AS link,
        NULL AS favicon,
        NULL AS etag,
        NULL AS modified,
        0 AS poll,
        NULL AS poll_status,
        NULL AS poll_type,
        0 AS next_poll,
        (SELECT COUNT(1) FROM entry_view WHERE flag_read = 0) AS unread_count
    UNION
        SELECT
            -2 AS _id,
            NULL AS url,
            'STARRED' AS title,
            NULL AS link,
            NULL AS favicon,
            NULL AS etag,
            NULL AS modified,
            0 AS poll,
            NULL AS poll_status,
            NULL AS poll_type,
            0 AS next_poll,
            (SELECT COUNT(1) FROM entry_view WHERE flag_star = 1) AS unread_count
        UNION
            SELECT
                feed._id,
                feed.url,
                feed.title,
                feed.link,
                feed.favicon,
                feed.etag,
                feed.modified,
                feed.poll,
                feed.poll_status,
                feed.poll_type,
                feed.next_poll,
                (SELECT COUNT(1) FROM entry_view WHERE entry_view.feed_id = feed._id AND flag_read = 0) AS unread_count
            FROM feed
            ORDER BY title;

-- add initial feeds
INSERT INTO feed (url, title, link) VALUES ('http://www.tughi.com/feed', 'Tughi''s Blog', 'http://www.tughi.com');

-- add initial feeds
INSERT INTO feed (url, title, link) VALUES ('https://github.com/tughi.atom', 'Tughi''s Github', 'https://github.com/tughi');
