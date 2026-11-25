package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.ui.ChatResizer;
import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mouse click/drag/release handlers live in the {@code ParentElement}
 * interface as default methods, so we inject here and only act when the
 * current element is the chat screen. Since 1.21.9 they take a
 * {@link Click} record instead of raw coordinates.
 *
 * <p>{@code Screen} does not declare {@code mouseClicked} itself — it
 * inherits the default method — so the toast click has to be handled
 * here as well, not in {@link ScreenMixin}.
 */
@Mixin(ParentElement.class)
public interface ParentElementMixin {

    /**
     * A click on a toast expands or collapses it. Toasts float above the
     * screen content, so they get the click first.
     */
    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    default void stafftools$onToastClick(
            Click click,
            boolean doubled,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof Screen)) {
            return;
        }

        if (Toasts.INSTANCE.mouseClicked(
                click.x(),
                click.y(),
                click.button()
        )) {

            cir.setReturnValue(true);
        }
    }

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

        // a chat resize drag outranks everything and has to survive
        // even once the chat screen itself is gone
        if (ChatResizer.INSTANCE.isResizing()) {

            if (ChatResizer.INSTANCE.mouseDragged(
                    click.x(),
                    click.y(),
                    click.button()
            )) {

                cir.setReturnValue(true);
            }

            return;
        }

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

        if (ChatResizer.INSTANCE.isResizing()) {

            if (ChatResizer.INSTANCE.mouseReleased(
                    click.x(),
                    click.y(),
                    click.button()
            )) {

                cir.setReturnValue(true);
            }

            return;
        }

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

    /**
     * While the reason modal is open, typed characters must go to its
     * text field instead of the chat input. {@code charTyped} is a
     * default method here, so this catches every screen.
     */
    @Inject(
            method = "charTyped",
            at = @At("HEAD"),
            cancellable = true
    )
    default void stafftools$onCharTyped(
            CharInput input,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!((Object) this instanceof ChatScreen)) {
            return;
        }

        if (PlayerActionOverlay.INSTANCE.charTyped(input)) {
            cir.setReturnValue(true);
        }
    }
}
