package com.mojang.tower.service;

import com.mojang.tower.Sound;

/**
 * Interface abstracting the sound playback system.
 *
 * This allows swapping implementations for testing (NullAudioService)
 * or different audio backends in the future.
 */
public interface AudioService {
    /**
     * Play a sound effect.
     *
     * @param sound the sound to play
     */
    void play(Sound sound);

    /**
     * Set whether audio output is muted.
     *
     * @param mute true to mute, false to unmute
     */
    void setMute(boolean mute);

    /**
     * Check if audio output is currently muted.
     *
     * @return true if muted, false otherwise
     */
    boolean isMute();
}
