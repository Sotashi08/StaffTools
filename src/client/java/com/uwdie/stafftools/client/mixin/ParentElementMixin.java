package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In 1.21.x the mouse drag/release handlers live in the {@code ParentElement}
 * interface as default methods ({@link Screen} does not override them), so we
 * inject here and only act when the current element is the chat screen.
 */
@Mixin(ParentElement.class)
public interface ParentElementMixin {

    @Inject(
            method = "mouseDragged",
            at = @At("HEAD"),
            cancellable = true
    )
    default void stafftools$onMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof ChatScreen)) {
            return;
        }

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseDragged(
                        mouseX,
                        mouseY,
                        button,
                        deltaX,
                        deltaY
                )) {

            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "mouseReleased",
            at = @At("HEAD"),
            cancellable = true
    )
    default void stafftools$onMouseReleased(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof ChatScreen)) {
            return;
        }

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseReleased(
                        mouseX,
                        mouseY,
                        button
                )) {

            cir.setReturnValue(true);
        }
    }
}
