package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.ChatResizer;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the chat grow past the vanilla 320×180 maximum.
 *
 * {@code ChatHud#getWidth(D)} / {@code #getHeight(D)} are the single
 * source of the chat layout: message wrapping, the rendered background,
 * the scroll clamp and the visible line count all go through them, so
 * multiplying the result scales the chat everywhere at once — including
 * the values shown in the vanilla "Chat Settings" menu.
 *
 * A boost of 1 (the default) returns vanilla's value untouched.
 */
@Mixin(ChatHud.class)
public class ChatHudResizeMixin {

    @Inject(
            method = "getWidth(D)I",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void stafftools$boostChatWidth(
            double widthOption,
            CallbackInfoReturnable<Integer> cir
    ) {

        int original = cir.getReturnValue();
        int boosted =
                ChatResizer.INSTANCE.boostWidth(original);

        if (boosted != original) {
            cir.setReturnValue(boosted);
        }
    }

    @Inject(
            method = "getHeight(D)I",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void stafftools$boostChatHeight(
            double heightOption,
            CallbackInfoReturnable<Integer> cir
    ) {

        int original = cir.getReturnValue();
        int boosted =
                ChatResizer.INSTANCE.boostHeight(original);

        if (boosted != original) {
            cir.setReturnValue(boosted);
        }
    }
}
