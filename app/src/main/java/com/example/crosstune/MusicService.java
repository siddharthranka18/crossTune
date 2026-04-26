package com.example.crosstune.;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * THE BACKGROUND ENGINE
 * This service runs independently of your UI. It keeps the music playing
 * and the notification alive even if the user minimizes the app or locks the phone.
 */
public class MusicService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize the global background player
        player = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true) // Pauses automatically if headphones disconnect
                .build();

        // 2. Set strict audio attributes so Android knows this is High-Priority Media
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        player.setAudioAttributes(audioAttributes, true);

        // 3. Create the MediaSession that Android will use to build the beautiful notification
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    // This method allows your PlayerFragment to "connect" to this background service
    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    // Handles what happens when the user swipes the app away from the "Recents" menu
    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player player = mediaSession.getPlayer();
        if (!player.getPlayWhenReady() || player.getMediaItemCount() == 0) {
            // If the music is paused, allow the service to die so we don't drain battery
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}