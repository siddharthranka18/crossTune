# CrossTune - Database Architecture & SQL Documentation

This repository contains the source code for **CrossTune**, a music streaming and discovery application. This document details the database design principles, SQL concepts, and specific query implementations used to power the app's features.

---

## 1. Database Design & Normalization

### Normalization (1NF, 2NF, 3NF)
Normalization is the process of organizing data to minimize redundancy and prevent anomalies (Insert, Update, Delete).

*   **1NF (First Normal Form):** Data is in a table format with atomic values. In CrossTune, we never store multiple Song IDs in a single text field; instead, each `SongID` gets its own row in the `PlaylistSongs` table.
*   **2NF (Second Normal Form):** Meets 1NF and ensures all non-key attributes depend on the *entire* primary key. By splitting User details and Playlist details into separate tables, we ensure that user-specific data (like `Email`) isn't repeated for every playlist created.
*   **3NF (Third Normal Form):** Meets 2NF and ensures non-key attributes do not depend on other non-key attributes. 
    *   *Project Application:* We store song metadata (Artist, Title, Duration) in `SongCache`. The `PlaylistSongs` table only stores the `SongID`. This prevents us from having to update the artist's name in 100 different playlists if it were to change; we only update it once in the cache.

---

## 2. Relational Keys

*   **Primary Key (PK):** A unique identifier for every record. 
    *   *Examples:* `UserID` in the `Users` table, `SongID` in `SongCache`.
*   **Foreign Key (FK):** A column that establishes a relationship between two tables. 
    *   *Example:* `PlaylistID` in `PlaylistSongs` is an FK that references the PK in the `Playlists` table. This maintains **Referential Integrity**.
*   **Candidate Key:** Any column or set of columns that *could* uniquely identify a record. For instance, a User's `Email` or `Username` are candidate keys before one is chosen as the Primary Key.

---

## 3. SQL Command Categories

*   **DDL (Data Definition Language):** Used to define the database structure.
    *   *Used in:* `CREATE PROCEDURE`, `CREATE TABLE`, `ALTER TABLE`.
*   **DML (Data Manipulation Language):** Used to manage data within the structure.
    *   *Used in:* `INSERT` (adding songs), `UPDATE` (editing playlists), `DELETE` (removing users).
*   **DQL (Data Query Language):** Used to fetch data.
    *   *Used in:* All `SELECT` statements used to populate the UI.

---

## 4. Advanced SQL Concepts

### Joins
Joins allow us to query data from multiple tables simultaneously.
*   **Inner Join:** Returns records that have matching values in both tables.
    *   *Use Case:* Linking `PlaylistSongs` with `SongCache` to show the actual song names in a user's playlist.

### Group By & Having
*   **Group By:** Collapses multiple rows into summary rows based on a specific column.
*   **Having:** Acts as a `WHERE` clause for groups.
    *   *Project Application:* Grouping songs by `Artist` to calculate which artist a user listens to the most.

### Subqueries
A query nested inside another query. 
*   *Example:* Finding all users who have more than 5 playlists by using a `SELECT` inside a `WHERE` clause.

### Functions (Aggregate & Scalar)
*   **Aggregate Functions:** Operate on a set of values to return a single result. (`COUNT`, `SUM`, `AVG`).
    *   *Project Application:* `SUM(durationSec)` to get the total length of a playlist.
*   **Scalar Functions:** Operate on a single value. (`IFNULL`, `UPPER`, `ROUND`).
    *   *Project Application:* `IFNULL(..., 0)` to ensure a playlist with zero songs returns `0` instead of `null`.

### Views
A virtual table based on the result-set of an SQL statement.
*   *Project Application:* Creating a `TrendingView` that pre-joins popular songs and their play counts for faster UI rendering.

### Stored Procedures
A prepared SQL code that you can save and reuse.
*   *Project Application:* `GetPlaylistSummary` is a procedure stored on the server that performs complex calculations (Count and Sum) and returns the result to the Android app.

---

## 5. Transactions (ACID Properties)

Transactions ensure that a series of database operations either all succeed or all fail together.
*   **Atomicity:** The "All or Nothing" rule.
*   **Consistency:** The database remains in a valid state after the transaction.
*   **Isolation:** Transactions do not interfere with each other.
*   **Durability:** Once committed, changes are permanent.

**Implementation in CrossTune:** 
When deleting a user, we use `START TRANSACTION`, `COMMIT`, and `ROLLBACK`. This ensures we don't delete a User record while leaving their Playlists behind due to a crash.

---

## 6. Indexing (Basics)
Indexes are used to retrieve data from the database more quickly.
*   We index `SongID` and `PlaylistID` columns to ensure that as the library grows to thousands of songs, the "Search" and "Open Playlist" features remain lightning-fast.

---

## 7. Project Query Logic Explained

### I. Playlist Summary Procedure
**Location:** `PlaylistFragment.java` logic
```sql
SELECT 
    COUNT(PS.SongID) AS song_count, 
    IFNULL(SUM(SC.durationSec), 0) AS total_seconds
FROM PlaylistSongs PS
JOIN SongCache SC ON PS.SongID = SC.SongID
WHERE PS.PlaylistID = target_playlist_id;
```
*   **Logic:** Performs an **Inner Join** between the mapping table and the metadata table. It uses **Aggregate Functions** to calculate the summary of a playlist in one trip to the database.

### II. Top Artist Discovery
**Location:** `db.java`
```sql
SELECT sc.artist, COUNT(*) AS play_count
FROM PlaylistSongs ps
JOIN Playlists p ON ps.PlaylistID = p.PlaylistID
JOIN SongCache sc ON ps.SongID = sc.SongID
WHERE p.UserID = ?
GROUP BY sc.artist
ORDER BY play_count DESC
LIMIT 5;
```
*   **Logic:** This is a complex **Triple Join**. It traverses from the User to their Playlists, then to the Songs within, and finally to the Metadata. It **Groups** by artist to identify the user's top 5 favorites.

### III. Safe User Deletion
**Location:** `db.java`
```sql
START TRANSACTION;
-- 1. Delete mapping
DELETE ps FROM PlaylistSongs ps ... 
-- 2. Delete playlists
DELETE FROM Playlists WHERE UserID = ?;
-- 3. Delete user
DELETE FROM Users WHERE UserID = ?;
COMMIT;
```
*   **Logic:** Demonstrates **Transactional Integrity**. It deletes records in the reverse order of their dependencies (Child records first, Parent records last) to avoid Foreign Key constraint violations.
