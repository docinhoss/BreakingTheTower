package com.mojang.tower.service;

import com.mojang.tower.Sound;

/**
 * No-op implementation of AudioService for testing.
 *
 * All methods do nothing, which allows tests to run
 * without actual audio hardware or side effects.
 */
public final class NullAudioService implements AudioService {

    @Override
    public void play(Sound sound) {
        // No-op: silent during testing
    }

    @Override
    public void setMute(boolean mute) {
        // No-op
    }

    @Override
    public boolean isMute() {
        return true; // Always muted in test environment
    }
}
