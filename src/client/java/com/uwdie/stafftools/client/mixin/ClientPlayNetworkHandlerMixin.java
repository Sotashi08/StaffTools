package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.chat.NickCache;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
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
        }
    }
}
