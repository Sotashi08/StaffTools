package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.gui.hud.InGameHud;
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
    private void stafftools$renderToasts(
            net.minecraft.client.gui.DrawContext context,
            net.minecraft.client.render.RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {

        Toasts.INSTANCE.render(context);
    }
}
