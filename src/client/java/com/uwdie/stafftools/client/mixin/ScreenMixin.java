package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.chat.ChatTextProcessor;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;
import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
            method = "handleTextClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stafftools$onTextClick(
            Style style,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (style == null) {
            return;
        }

        ClickEvent event =
                style.getClickEvent();

        if (event == null) {
            return;
        }

        String value = event.getValue();

        if (value == null) {
            return;
        }

        // Foreign click (e.g. "/msg Notch " from a chat plugin):
        // resolve the player and open the actions popup instead of
        // letting vanilla paste the command into the input field.
        if (!value.startsWith(
                ChatTextProcessor.CLICK_PREFIX
        )) {

            PlayerContext foreign =
                    ChatTextProcessor.resolveFromForeignClick(
                            value
                    );

            if (foreign != null &&
                    StafftoolsClient.getConfig()
                            .isPlayerActionsEnabled()) {

                PlayerActionOverlay.INSTANCE.open(foreign);

                cir.setReturnValue(true);
            }

            return;
        }

        String rest = value.substring(
                ChatTextProcessor.CLICK_PREFIX.length()
        );

        int separator = rest.lastIndexOf(':');

        String name =
                separator >= 0
                        ? rest.substring(0, separator)
                        : rest;

        String uuidPart =
                separator >= 0
                        ? rest.substring(separator + 1)
                        : "";

        UUID uuid = null;

        if (!uuidPart.isEmpty()) {

            try {
                uuid = UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ignored) {
            }
        }

        PlayerContext player = null;

        if (uuid != null) {
            player = PlayerResolver.resolveByUuid(uuid)
                    .orElse(null);
        }

        if (player == null) {
            player = PlayerResolver.resolve(name)
                    .orElse(null);
        }

        if (player == null && uuid != null) {
            player = new PlayerContext(name, uuid);
        }

        if (player == null) {
            return;
        }

        boolean shift =
                Screen.hasShiftDown();

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (shift &&
                StafftoolsClient.getConfig()
                        .isClickToCopyEnabled()) {

            client.keyboard.setClipboard(
                    player.name()
            );
        }

        if (StafftoolsClient.getConfig()
                .isPlayerActionsEnabled()) {

            PlayerActionOverlay.INSTANCE.open(player);
        }

        cir.setReturnValue(true);
    }

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
