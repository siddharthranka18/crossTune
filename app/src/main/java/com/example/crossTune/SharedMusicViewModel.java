package com.example.crossTune;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SharedMusicViewModel extends AndroidViewModel {

    // ================= CORE PLAYBACK STATE =================
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final MutableLiveData<String> accentColor = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> playlists = new MutableLiveData<>(new ArrayList<>());

    // ================= THE QUEUE CONTEXT ENGINE =================
    private final MutableLiveData<String> playingContext = new MutableLiveData<>("Unknown Context");
    private final List<Song> currentQueue = new ArrayList<>();
    private int currentQueueIndex = -1;

    // ================= THE TELEMETRY BRAIN (NEW) =================
    private long currentSongStartTime = 0;
    private int rapidSkipCount = 0;
    private final MutableLiveData<Boolean> moodPivotEvent = new MutableLiveData<>(false);

    // ================= JAMMING STATE =================
    private Socket mSocket;
    private final MutableLiveData<String> jamRoomCode = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isJamHost = new MutableLiveData<>(false);
    private final MutableLiveData<JamState> jamSyncEvent = new MutableLiveData<>();

    // ================= PREFERENCES & NETWORK =================
    private final SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MusicAppPrefs";
    private static final String KEY_PLAYLISTS = "PlaylistsJson";
    private static final String KEY_ACCENT_COLOR = "AppAccentColor";

    // Telemetry Keys
    private static final String KEY_TELEMETRY_AFFINITY = "TelemetryAffinityDB";
    private static final String KEY_TELEMETRY_HISTORY = "TelemetryHistoryDB";

    private final OkHttpClient httpClient = new OkHttpClient();

    public SharedMusicViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadAccentColor();
        loadPlaylists();
        initSocket();
    }

    // ==============================================================
    // THE TELEMETRY & MOOD PIVOT ENGINE (The Secret Sauce)
    // ==============================================================

    public LiveData<Boolean> getMoodPivotEvent() { return moodPivotEvent; }
    public void resetMoodPivot() { moodPivotEvent.setValue(false); rapidSkipCount = 0; }

    private void recordTelemetryForCurrentSong() {
        Song prevSong = currentSong.getValue();
        if (prevSong == null || currentSongStartTime == 0) return;

        long timeListenedMs = System.currentTimeMillis() - currentSongStartTime;
        long totalDurationMs = prevSong.getDuration() > 0 ? (prevSong.getDuration() * 1000) : 180000;

        double completionRatio = (double) timeListenedMs / totalDurationMs;

        if (timeListenedMs < 15000) {
            rapidSkipCount++;
            updateArtistAffinity(prevSong.getArtist(), -5);
        } else {
            if (timeListenedMs > 30000) rapidSkipCount = 0;

            if (completionRatio > 0.8) {
                updateArtistAffinity(prevSong.getArtist(), 10);
                addToHeavyRotation(prevSong);
            } else if (completionRatio > 0.4) {
                updateArtistAffinity(prevSong.getArtist(), 3);
            }
        }
    }

    private void updateArtistAffinity(String artist, int scoreDelta) {
        if (artist == null || artist.equals("Unknown Artist") || artist.contains("Various")) return;
        try {
            String jsonStr = sharedPreferences.getString(KEY_TELEMETRY_AFFINITY, "{}");
            JSONObject affinityDB = new JSONObject(jsonStr);

            int currentScore = affinityDB.optInt(artist, 0);
            int newScore = Math.max(0, currentScore + scoreDelta); 

            affinityDB.put(artist, newScore);
            sharedPreferences.edit().putString(KEY_TELEMETRY_AFFINITY, affinityDB.toString()).apply();
        } catch (Exception e) { Log.e("TelemetryEngine", "Affinity update failed", e); }
    }

    private void addToHeavyRotation(Song song) {
        try {
            String jsonStr = sharedPreferences.getString(KEY_TELEMETRY_HISTORY, "[]");
            JSONArray historyDB = new JSONArray(jsonStr);

            JSONObject trackData = new JSONObject();
            trackData.put("id", song.getId());
            trackData.put("title", song.getTitle());
            trackData.put("artist", song.getArtist());
            trackData.put("thumbnailUrl", song.getThumbnailUrl());
            trackData.put("timestamp", System.currentTimeMillis());

            JSONArray updatedHistory = new JSONArray();
            updatedHistory.put(trackData);

            int limit = Math.min(historyDB.length(), 99);
            for (int i = 0; i < limit; i++) {
                updatedHistory.put(historyDB.getJSONObject(i));
            }

            sharedPreferences.edit().putString(KEY_TELEMETRY_HISTORY, updatedHistory.toString()).apply();
        } catch (Exception e) { Log.e("TelemetryEngine", "History update failed", e); }
    }

    public static String getCurrentTimeBucket() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return "Morning";
        if (hour >= 11 && hour < 17) return "Afternoon";
        if (hour >= 17 && hour < 22) return "Evening";
        return "Night";
    }

    // ==============================================================
    // THE QUEUE CONTEXT ENGINE
    // ==============================================================

    public LiveData<String> getPlayingContext() { return playingContext; }

    public void playSongWithContext(Song song, List<Song> queue, String contextName) {
        this.currentQueue.clear();
        if (queue != null && !queue.isEmpty()) this.currentQueue.addAll(queue);
        else this.currentQueue.add(song);

        this.playingContext.setValue(contextName);
        this.currentQueueIndex = -1;
        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).getId().equals(song.getId())) {
                this.currentQueueIndex = i;
                break;
            }
        }
        if (this.currentQueueIndex == -1) this.currentQueueIndex = 0;
        setSong(song);
    }

    public void skipToNext() {
        if (currentQueue.isEmpty()) return;
        currentQueueIndex++;
        if (currentQueueIndex >= currentQueue.size()) currentQueueIndex = 0; 
        setSong(currentQueue.get(currentQueueIndex));
    }

    public void skipToPrevious() {
        if (currentQueue.isEmpty()) return;
        currentQueueIndex--;
        if (currentQueueIndex < 0) currentQueueIndex = currentQueue.size() - 1; 
        setSong(currentQueue.get(currentQueueIndex));
    }

    // ================= CORE PLAYBACK ENGINE =================

    public void setSong(Song song) {
        recordTelemetryForCurrentSong();
        currentSongStartTime = System.currentTimeMillis();
        currentSong.setValue(song);
        isPlaying.setValue(true);
    }

    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public void togglePlayPause() { if (isPlaying.getValue() != null) isPlaying.setValue(!isPlaying.getValue()); }
    public void setPlaying(boolean playing) { isPlaying.postValue(playing); }


    // ================= JAMMING ENGINE (SOCKET.IO) =================
    private void initSocket() {
        try {
            mSocket = IO.socket("https://music-jam-relay.onrender.com/");
            mSocket.on("sync_update", args -> {
                if (args[0] != null) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        JamState state = new JamState();
                        state.videoId = data.optString("videoId", data.optString("id"));
                        state.title = data.getString("title");
                        state.artist = data.getString("artist");
                        state.thumbnailUrl = data.getString("thumbnailUrl");
                        state.isPlaying = data.getBoolean("isPlaying");
                        state.seekPosition = data.getLong("seekPosition");
                        state.hostTime = data.getLong("hostTime");
                        state.streamUrl = data.optString("streamUrl", null);
                        jamSyncEvent.postValue(state);
                    } catch (Exception e) { Log.e("JamEngine", "Parse Error", e); }
                }
            });
            mSocket.connect();
        } catch (Exception e) { Log.e("JamEngine", "Socket init failed", e); }
    }

    public LiveData<String> getJamRoomCode() { return jamRoomCode; }
    public LiveData<Boolean> getIsJamHost() { return isJamHost; }
    public LiveData<JamState> getJamSyncEvent() { return jamSyncEvent; }

    public void createJamRoom() {
        String code = String.format("%06d", new Random().nextInt(999999));
        isJamHost.setValue(true);
        jamRoomCode.setValue(code);
        if (mSocket != null) mSocket.emit("join_room", code);
    }

    public void joinJamRoom(String code) {
        isJamHost.setValue(false);
        jamRoomCode.setValue(code);
        if (mSocket != null) mSocket.emit("join_room", code);
    }

    public void leaveJamRoom() {
        isJamHost.setValue(false);
        jamRoomCode.setValue(null);
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.connect();
        }
    }

    public void sendHostUpdate(Song song, boolean isPlaying, long seekPosition) {
        if (!Boolean.TRUE.equals(isJamHost.getValue()) || jamRoomCode.getValue() == null || song == null || mSocket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", jamRoomCode.getValue());
            data.put("videoId", song.getId()); 
            data.put("title", song.getTitle());
            data.put("artist", song.getArtist());
            data.put("thumbnailUrl", song.getThumbnailUrl());
            data.put("isPlaying", isPlaying);
            data.put("seekPosition", seekPosition);
            data.put("hostTime", System.currentTimeMillis());
            if (song.getStreamUrl() != null) data.put("streamUrl", song.getStreamUrl());
            mSocket.emit("host_update", data);
        } catch (Exception e) { Log.e("JamEngine", "Broadcast Error", e); }
    }

    public static class JamState {
        public String videoId, title, artist, thumbnailUrl, streamUrl;
        public boolean isPlaying;
        public long seekPosition, hostTime;
    }

    // ================= USER PREFERENCES & PLAYLISTS =================

    public LiveData<String> getAccentColor() { return accentColor; }
    public void setAccentColor(String colorHex) { accentColor.setValue(colorHex); sharedPreferences.edit().putString(KEY_ACCENT_COLOR, colorHex).apply(); }
    private void loadAccentColor() { accentColor.setValue(sharedPreferences.getString(KEY_ACCENT_COLOR, "#FFFFFF")); }

    public LiveData<List<Playlist>> getPlaylists() { return playlists; }

    private String escape(String data) {
        if (data == null) return "";
        return data.replace("'", "''"); 
    }
    public void createPlaylist(String name) {
        String playlistId = UUID.randomUUID().toString();
        String userId = currentUser.getUid(); 

        List<Playlist> current = playlists.getValue();
        if (current == null) return;
        current.add(new Playlist(playlistId, name, true));
        playlists.setValue(current);
        savePlaylists(current);

        String sql = String.format(
                "INSERT INTO Playlists (PlaylistID, UserID, name, createdAt) " +
                        "VALUES ('%s', '%s', '%s', NOW());",
                playlistId, userId, escape(name)
        );
        DB.execute(sql);
    }

    private String sqlSafe(String input) {
        if (input == null) return "NULL";
        return "'" + input.replace("'", "''") + "'";
    }

    public void addSongToPlaylist(String playlistId, Song song) {
        List<Playlist> current = playlists.getValue();
        if (current == null) return;
        for (Playlist p : current) {
            if (p.getId().equals(playlistId)) {
                boolean exists = false;
                for (Song s : p.getSongs()) {
                    if (s.getId().equals(song.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    p.getSongs().add(0, song);
                    playlists.setValue(current);
                    savePlaylists(current);
                    new Thread(() -> {
                        try {
                            String songSql = String.format(
                                    "INSERT IGNORE INTO SongCache (SongID, title, artist, album, durationSec, artworkUrl) " +
                                            "VALUES (%s, %s, %s, %s, %d, %s);",
                                    sqlSafe(song.getId()),
                                    sqlSafe(song.getTitle()),
                                    sqlSafe(song.getArtist()),
                                    sqlSafe(song.getAlbum()),
                                    song.getDuration(),
                                    sqlSafe(song.getThumbnailUrl())
                            );
                            DB.execute(songSql);
                            Thread.sleep(2000);
                            String linkSql = String.format(
                                    "INSERT INTO PlaylistSongs (PlaylistID, SongID, addedAt) " +
                                            "VALUES (%s, %s, NOW()) " +
                                            "ON DUPLICATE KEY UPDATE addedAt = NOW();",
                                    sqlSafe(playlistId),
                                    sqlSafe(song.getId())
                            );
                            DB.execute(linkSql);
                        } catch (InterruptedException e) { e.printStackTrace(); }
                    }).start();
                }
                break;
            }
        }
    }

    public void toggleLike(Song song) {
        List<Playlist> current = playlists.getValue();
        if (current == null) return;
        boolean removed = false;
        for (Playlist p : current) {
            if (p.getId().equals("liked")) {
                for (int i = 0; i < p.getSongs().size(); i++) {
                    if (p.getSongs().get(i).getId().equals(song.getId())) {
                        p.getSongs().remove(i); removed = true; break;
                    }
                }
                if (!removed) p.getSongs().add(0, song);
                break;
            }
        }
        playlists.setValue(current); savePlaylists(current);
        if (removed) removeLikedSongFromDb(song);
    }

    public void removeSongFromLiked(Song song) {
        if (song == null) return;
        List<Playlist> current = playlists.getValue();
        if (current == null) return;
        boolean removed = false;
        for (Playlist p : current) {
            if (p.getId().equals("liked")) {
                for (int i = 0; i < p.getSongs().size(); i++) {
                    if (p.getSongs().get(i).getId().equals(song.getId())) {
                        p.getSongs().remove(i); removed = true; break;
                    }
                }
                break;
            }
        }
        if (removed) {
            playlists.setValue(current);
            savePlaylists(current);
            removeLikedSongFromDb(song);
        }
    }

    private void removeLikedSongFromDb(Song song) {
        if (song == null || song.getId() == null) return;
        String deleteSql = String.format(
                "DELETE FROM PlaylistSongs WHERE PlaylistID=%s AND SongID=%s;",
                sqlSafe("liked"),
                sqlSafe(song.getId())
        );
        DB.execute(deleteSql);
    }

    public boolean isSongLiked(String id) {
        List<Playlist> current = playlists.getValue();
        if (current == null) return false;
        for (Playlist p : current) {
            if (p.getId().equals("liked")) {
                for (Song s : p.getSongs()) { if (s.getId().equals(id)) return true; }
            }
        }
        return false;
    }

    public void downloadSong(Context context, Song song, String streamUrl) {
        Toast.makeText(context, "Starting Download...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request request = new Request.Builder().url(streamUrl).build();
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    File dir = new File(context.getFilesDir(), "offline_music");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, song.getId() + ".mp3");
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(response.body().bytes());
                    fos.close();
                    song.setLocalPath(file.getAbsolutePath());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        addSongToPlaylist("downloads", song);
                        Toast.makeText(context, "Saved to Downloads: " + song.getTitle(), Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void savePlaylists(List<Playlist> lists) {
        try {
            JSONArray arr = new JSONArray();
            for (Playlist p : lists) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("name", p.getName());
                obj.put("isDeletable", p.isDeletable());
                JSONArray songArr = new JSONArray();
                for (Song s : p.getSongs()) {
                    JSONObject sObj = new JSONObject();
                    sObj.put("id", s.getId() != null ? s.getId() : s.getVideoId());
                    sObj.put("title", s.getTitle());
                    sObj.put("artist", s.getArtist());
                    sObj.put("album", s.getAlbum());
                    sObj.put("thumbnailUrl", s.getThumbnailUrl());
                    sObj.put("duration", s.getDuration());
                    if (s.getStreamUrl() != null) sObj.put("streamUrl", s.getStreamUrl());
                    if (s.getLocalPath() != null) sObj.put("localPath", s.getLocalPath());
                    songArr.put(sObj);
                }
                obj.put("songs", songArr);
                arr.put(obj);
            }
            sharedPreferences.edit().putString(KEY_PLAYLISTS, arr.toString()).apply();
        } catch (Exception e) { Log.e("ViewModel", "Save Failed", e); }
    }

    private void loadPlaylists() {
        String jsonStr = sharedPreferences.getString(KEY_PLAYLISTS, null);
        List<Playlist> loaded = new ArrayList<>();
        if (jsonStr == null) {
            loaded.add(new Playlist("liked", "Liked Songs", false));
            loaded.add(new Playlist("downloads", "Downloads", false));
            playlists.setValue(loaded); savePlaylists(loaded);
            return;
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Playlist p = new Playlist(obj.getString("id"), obj.getString("name"), obj.getBoolean("isDeletable"));
                JSONArray songArr = obj.getJSONArray("songs");
                for (int j = 0; j < songArr.length(); j++) {
                    JSONObject sObj = songArr.getJSONObject(j);
                    String id = sObj.has("id") ? sObj.getString("id") : sObj.getString("videoId");
                    String album = sObj.has("album") ? sObj.getString("album") : "Unknown Album";
                    long duration = sObj.has("duration") ? sObj.getLong("duration") : 0;
                    Song s = new Song(id, sObj.getString("title"), sObj.getString("artist"), album, sObj.getString("thumbnailUrl"), duration);
                    if (sObj.has("streamUrl")) s.setStreamUrl(sObj.getString("streamUrl"));
                    if (sObj.has("localPath")) s.setLocalPath(sObj.getString("localPath"));
                    p.getSongs().add(s);
                }
                loaded.add(p);
            }
            playlists.setValue(loaded);
        } catch (Exception e) { Log.e("ViewModel", "Load Failed", e); }
    }

    public interface DeletionCallback { void onComplete(boolean success); }

    public void deleteCurrentUserData(DeletionCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { if (callback != null) callback.onComplete(false); return; }
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = DB.deleteUserDataTransactional(user.getUid());
            if (success) {
                sharedPreferences.edit().remove(KEY_PLAYLISTS).remove(KEY_TELEMETRY_AFFINITY).remove(KEY_TELEMETRY_HISTORY).apply();
                List<Playlist> reset = new ArrayList<>();
                reset.add(new Playlist("liked", "Liked Songs", false));
                reset.add(new Playlist("downloads", "Downloads", false));
                playlists.postValue(reset);
            }
            new Handler(Looper.getMainLooper()).post(() -> { if (callback != null) callback.onComplete(success); });
        });
    }
}
