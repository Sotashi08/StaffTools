package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the actions popup and the toasts on every screen except the
 * chat one (that one is covered by {@link ChatScreenMixin}, which has
 * to draw them on top).
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void stafftools$renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {

        if (MinecraftClient.getInstance().currentScreen
                instanceof ChatScreen) {

            return;
        }

        PlayerActionOverlay.INSTANCE.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        Toasts.INSTANCE.render(context);
    }
}
