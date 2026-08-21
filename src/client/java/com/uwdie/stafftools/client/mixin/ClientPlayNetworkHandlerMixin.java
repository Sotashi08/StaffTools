package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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
}
