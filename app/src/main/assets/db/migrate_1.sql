-- the feeds table
CREATE TABLE feed (
    _id INTEGER PRIMARY KEY,
    url TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    favicon TEXT,
    etag TEXT,
    modified TEXT,
    poll INTEGER NOT NULL,
    poll_status INTEGER,
    poll_type TEXT,
    next_poll INTEGER NOT NULL DEFAULT 0
);

-- the entry_sync table is updated by the sync service
CREATE TABLE entry_sync (
    feed_id INTEGER NOT NULL,
    guid TEXT NOT NULL,
    poll INTEGER NOT NULL,
    updated INTEGER,
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
CREATE VIEW entry AS
    SELECT
        entry_user._id AS _id,
        entry_user.feed_id AS feed_id,
        entry_user.guid AS guid,
        entry_sync.poll AS poll,
        entry_sync.updated AS updated,
        entry_sync.data AS data,
        entry_user.flag_read AS flag_read,
        entry_user.flag_star AS flag_star
    FROM entry_user, entry_sync
    WHERE
        entry_user.feed_id = entry_sync.feed_id AND
        entry_user.guid = entry_sync.guid;