-- create_views.sql
-- Creates the UserArtistStats view and helpful indexes.
-- Safe to commit (contains only DDL, no credentials).

USE railway;

DROP VIEW IF EXISTS UserArtistStats;

CREATE VIEW UserArtistStats AS
SELECT
    p.UserID,
    sc.artist,
    COUNT(*) AS song_count
FROM PlaylistSongs ps
JOIN Playlists p ON ps.PlaylistID = p.PlaylistID
JOIN SongCache sc ON ps.SongID = sc.SongID
GROUP BY p.UserID, sc.artist;

-- Indexes for performance (idempotent if your server supports IF NOT EXISTS)
-- MySQL 8+ supports CREATE INDEX IF NOT EXISTS; if your server doesn't, these will error if index exists.
CREATE INDEX idx_song_artist ON SongCache(artist);
CREATE INDEX idx_playlist_user ON Playlists(UserID);

