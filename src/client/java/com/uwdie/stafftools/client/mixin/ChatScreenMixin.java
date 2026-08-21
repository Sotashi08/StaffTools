package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

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

        PlayerActionOverlay.INSTANCE.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        Toasts.INSTANCE.render(context);
    }

    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stafftools$onMouseClicked(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                )) {

            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "mouseScrolled",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stafftools$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseScrolled(
                        mouseX,
                        mouseY,
                        horizontalAmount,
                        verticalAmount
                )) {

            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stafftools$onKeyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {

            PlayerActionOverlay.INSTANCE.close();
            cir.setReturnValue(true);
        }
    }
}
