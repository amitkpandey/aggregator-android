-- the feed_sync table is updated by the sync service
CREATE TABLE feed_sync (
    _id INTEGER PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    etag TEXT,
    modified TEXT,
    entry_count INTEGER NOT NULL DEFAULT 0
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
CREATE TRIGGER create_feed_user
    AFTER INSERT ON feed_sync
    BEGIN
        INSERT INTO feed_user (_id) VALUES (NEW._id);
    END;

-- a trigger that deletes the associated feed_user when a feed_sync is deleted
CREATE TRIGGER delete_feed_user
    AFTER DELETE ON feed_sync
    BEGIN
        DELETE FROM feed_user WHERE _id = OLD._id;
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
    ro_flag_read INTEGER NOT NULL DEFAULT 0,
    UNIQUE (feed_id, guid),
    FOREIGN KEY (feed_id) REFERENCES feed (_id)
);

-- a trigger that inserts a new entry_user row for each new entry_sync
CREATE TRIGGER create_entry_user
    AFTER INSERT ON entry_sync
    BEGIN
        INSERT INTO entry_user (feed_id, guid, poll) VALUES (NEW.feed_id, NEW.guid, NEW.poll);
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
        entry_sync.data AS data,
        entry_user.flag_read AS flag_read,
        entry_user.flag_star AS flag_star,
        entry_user.ro_flag_read AS ro_flag_read,
        feed_sync.title AS feed_title,
        feed_user.favicon AS feed_favicon
    FROM
        entry_user,
        entry_sync,
        feed_user,
        feed_sync
    WHERE
        entry_user.feed_id = entry_sync.feed_id AND
        entry_user.guid = entry_sync.guid AND
        entry_user.feed_id = feed_user._id AND
        feed_user._id = feed_sync._id;

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
        -1 AS update_mode,
        0 AS next_sync,
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
            -1 AS update_mode,
            0 AS next_sync,
            (SELECT COUNT(1) FROM entry_view WHERE flag_star = 1) AS unread_count
        UNION
            SELECT
                feed_sync._id,
                feed_sync.url,
                feed_sync.title,
                feed_sync.link,
                feed_user.favicon,
                feed_sync.etag,
                feed_sync.modified,
                feed_user.update_mode,
                feed_user.next_sync,
                (SELECT COUNT(1) FROM entry_view WHERE entry_view.feed_id = feed_sync._id AND flag_read = 0) AS unread_count
            FROM
                feed_user,
                feed_sync
            WHERE
                feed_user._id = feed_sync._id
            ORDER BY title;

-- a view for the feed updates
CREATE VIEW sync_log AS
    SELECT
        feed_sync._id AS feed_id,
        entry_user.poll AS poll,
        COUNT(1) AS entry_count,
        feed_sync.entry_count AS entry_count_max
    FROM
        feed_sync,
        entry_user
    WHERE
        feed_sync._id = entry_user.feed_id
    GROUP BY entry_user.poll, feed_sync._id
    ORDER BY entry_user.poll;


-- add test feed
INSERT INTO feed_sync (url, title, link) VALUES ('http://www.tughi.com/feed', 'Tughi''s Blog', 'http://www.tughi.com');

-- add test feed
INSERT INTO feed_sync (url, title, link) VALUES ('https://github.com/tughi.atom', 'Tughi''s Github', 'https://github.com/tughi');
