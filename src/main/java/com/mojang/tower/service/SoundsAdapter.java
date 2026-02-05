package com.mojang.tower.service;

import com.mojang.tower.Sound;
import com.mojang.tower.Sounds;

/**
 * Adapter that wraps the existing Sounds singleton.
 *
 * Delegates all calls to the static Sounds methods,
 * providing an AudioService interface for the existing implementation.
 */
public final class SoundsAdapter implements AudioService {

    @Override
    public void play(Sound sound) {
        Sounds.play(sound);
    }

    @Override
    public void setMute(boolean mute) {
        Sounds.setMute(mute);
    }

    @Override
    public boolean isMute() {
        return Sounds.isMute();
    }
}
