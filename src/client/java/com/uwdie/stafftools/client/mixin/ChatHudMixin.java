package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.chat.ChatTextProcessor;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Text stafftools$styleMessage(Text message) {

        return ChatTextProcessor.process(message);
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD")
    )
    private void stafftools$captureMessage(
            Text message,
            MessageSignatureData signature,
            MessageIndicator indicator,
            CallbackInfo ci
    ) {

        if (message != null) {

            PunishmentHistory.get()
                    .onMessageReceived(
                            message.getString()
                    );
        }
    }
}
