package com.uwdie.stafftools.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Short UI sounds for the mod. Each action and notification kind gets a
 * distinct, pleasant cue built from vanilla sound events, so the mod
 * feels responsive without any custom resource pack.
 *
 * <p>All sounds are quiet and layered at low pitch so they never fight
 * with the game audio. Playback goes through the vanilla sound manager
 * as UI (non-positional) instances.
 */
public final class UiSound {

    private UiSound() {
    }

    /** Soft confirm chime for a successful action / execution. */
    public static void confirm() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.4f, 0.18f);
    }

    /** Bright two-note success for punishments and profile actions. */
    public static void success() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.6f, 0.16f);
    }

    /** Gentle warning blip for the warn / attention state. */
    public static void warn() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.9f, 0.20f);
    }

    /** Low, serious tone for dangerous / destructive actions. */
    public static void danger() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.24f);
    }

    /** Quick pop when a popup / modal opens. */
    public static void open() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f, 0.14f);
    }

    /** Soft close tick when a popup / modal closes. */
    public static void close() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.10f);
    }

    /** Copy-to-clipboard confirmation. */
    public static void copy() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1.8f, 0.16f);
    }

    /** Error / rejected action. */
    public static void error() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6f, 0.22f);
    }

    /** Subtle tick for selection / hover confirmations. */
    public static void tick() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 2.0f, 0.10f);
    }

    private static void play(SoundEvent sound, float pitch, float volume) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.getSoundManager() == null) {
            return;
        }

        client.getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.ui(
                        sound, pitch, volume));
    }
}