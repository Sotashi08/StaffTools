package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.chat.ChatTextProcessor;
import com.uwdie.stafftools.client.chat.NickCache;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(
            method = "sendChatCommand",
            at = @At("HEAD")
    )
    private void stafftools$onCommandSent(
            String command,
            CallbackInfo ci
    ) {

        if (command != null) {

            PunishmentHistory.get()
                    .onCommandSent(command);
        }
    }

    /**
     * Keeps the local nick cache in sync with the tab list, so players
     * stay resolvable for a while after they leave it.
     */
    @Inject(
            method = "onPlayerList",
            at = @At("TAIL")
    )
    private void stafftools$cacheTabList(
            PlayerListS2CPacket packet,
            CallbackInfo ci
    ) {

        if (StafftoolsClient.getConfig().isDetectCache()) {

            NickCache.get().refreshFromTabList();

            // EssentialsX keeps the account name in the profile and puts
            // a changed nick into the display name, so it is tracked
            // separately
            com.uwdie.stafftools.client.chat
                    .EssentialsNickTracker.get()
                    .refreshFromTabList();
        }
    }

    /**
     * Safety net for chat plugins.
     *
     * {@code ChatHudMixin} styles messages on their way into the chat
     * widget, but a self-written chat plugin may route its text through
     * packets the widget never sees, or replace the widget's messages
     * right after they were added. Reading the packets directly means
     * the detection runs on the raw server text in every case, and the
     * text is remembered for the click-on-nick lookup.
     */
    @Inject(
            method = "onGameMessage",
            at = @At("TAIL")
    )
    private void stafftools$onGameMessage(
            GameMessageS2CPacket packet,
            CallbackInfo ci
    ) {

        // overlay messages are the action bar, not the chat log
        if (!packet.overlay()) {

            observe(packet.content());
        }
    }

    @Inject(
            method = "onProfilelessChatMessage",
            at = @At("TAIL")
    )
    private void stafftools$onProfilelessChatMessage(
            ProfilelessChatMessageS2CPacket packet,
            CallbackInfo ci
    ) {

        observe(packet.message());
    }

    @Inject(
            method = "onOverlayMessage",
            at = @At("TAIL")
    )
    private void stafftools$onOverlayMessage(
            OverlayMessageS2CPacket packet,
            CallbackInfo ci
    ) {

        observe(packet.text());
    }

    /**
     * Feeds a raw server message into the detection and the punishment
     * history. Failures are swallowed on purpose: a chat plugin must
     * never be able to crash the client through this hook.
     */
    private static void observe(Text message) {

        if (message == null) {
            return;
        }

        try {

            PunishmentHistory.get()
                    .onMessageReceived(message.getString());

            // the styled result teaches the alias cache who is who
            ChatTextProcessor.process(message);

        } catch (Throwable ignored) {

            // a malformed component from a plugin must not be fatal
        }
    }
}
