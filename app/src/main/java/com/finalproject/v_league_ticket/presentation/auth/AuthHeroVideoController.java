package com.finalproject.v_league_ticket.presentation.auth;

import android.content.Context;
import android.net.Uri;

import com.finalproject.v_league_ticket.R;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

final class AuthHeroVideoController {
    private static ExoPlayer activePlayer;

    private final PlayerView playerView;
    private ExoPlayer player;

    AuthHeroVideoController(PlayerView playerView) {
        this.playerView = playerView;
    }

    void prepare(Context context) {
        stopOtherAuthVideo();
        Uri videoUri = RawResourceDataSource.buildRawResourceUri(R.raw.auth_intro);
        player = new ExoPlayer.Builder(context).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(0f);
        player.setMediaItem(MediaItem.fromUri(videoUri));
        player.prepare();
        player.setPlayWhenReady(true);
        playerView.setPlayer(player);
        activePlayer = player;
    }

    void resume() {
        if (player != null) {
            stopOtherAuthVideo();
            activePlayer = player;
            player.setPlayWhenReady(true);
        }
    }

    void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    void release() {
        if (activePlayer == player) {
            activePlayer = null;
        }
        playerView.setPlayer(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void stopOtherAuthVideo() {
        if (activePlayer != null && activePlayer != player) {
            activePlayer.setPlayWhenReady(false);
        }
    }
}
