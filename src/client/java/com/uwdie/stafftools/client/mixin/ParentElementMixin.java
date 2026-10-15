package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mouse drag/release handlers live in the {@code ParentElement}
 * interface as default methods, so we inject here and only act when the
 * current element is the chat screen. Since 1.21.9 they take a
 * {@link Click} record instead of raw coordinates.
 */
@Mixin(ParentElement.class)
public interface ParentElementMixin {

    @Inject(
            method = "mouseDragged",
            at = @At("HEAD"),
            cancellable = true
    )
    default void stafftools$onMouseDragged(
            Click click,
            double offsetX,
            double offsetY,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof ChatScreen)) {
            return;
        }

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseDragged(
                        click.x(),
                        click.y(),
                        click.button(),
                        offsetX,
                        offsetY
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
            Click click,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof ChatScreen)) {
            return;
        }

        if (PlayerActionOverlay.INSTANCE.isOpen() &&
                PlayerActionOverlay.INSTANCE.mouseReleased(
                        click.x(),
                        click.y(),
                        click.button()
                )) {

            cir.setReturnValue(true);
        }
    }
}
