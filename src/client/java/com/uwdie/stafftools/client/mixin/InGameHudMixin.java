package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.ModeratorHud;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void stafftools$renderHudOverlays(
            net.minecraft.client.gui.DrawContext context,
            net.minecraft.client.render.RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {

        Toasts.INSTANCE.render(context);

        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof ChatScreen)) {
            // Continue the HUD's close animation after ChatScreen stops
            // rendering. The chat screen itself draws it above its content.
            ModeratorHud.INSTANCE.render(context, 0, 0);
        }
    }
}
