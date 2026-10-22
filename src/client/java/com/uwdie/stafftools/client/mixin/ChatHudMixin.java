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

        if (message == null) {
            return null;
        }

        String original = message.getString();

        Text processed = ChatTextProcessor.process(message);

        // a chat plugin may deliver the whole line as one component, in
        // which case the per-node pass finds nothing and the whole-line
        // pass has to take over
        Text result = ChatTextProcessor.processWholeMessage(processed);

        // never let the mod cut chat text: if the whole-line rebuild
        // shrank the string, fall back to the per-node result (which
        // keeps every character but may lack whole-line highlighting)
        if (result == null || result.getString().length() < original.length()) {
            return processed;
        }

        return result;
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
