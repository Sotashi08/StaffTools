package com.uwdie.stafftools.client.mixin;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;
import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Toasts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

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

        // chat-input nick detection has priority over everything:
        // click on a hand-typed online nick -> actions popup
        if (tryChatInputDetect(mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }

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
                keyCode == GLFW.GLFW_KEY_ESCAPE) {

            // closes the alias modal first, then the popup itself
            cir.setReturnValue(
                    PlayerActionOverlay.INSTANCE.handleEscape()
            );
        }
    }

    /**
     * Click-to-detect inside the chat input: when the player typed an
     * online player's name by hand and clicks on it, the actions popup
     * opens for that player. Disabled via the settings toggle.
     */
    private boolean tryChatInputDetect(
            double mouseX,
            double mouseY
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (!StafftoolsClient.getConfig()
                .isChatInputDetectEnabled()) {

            return false;
        }

        if (!StafftoolsClient.getConfig()
                .isPlayerActionsEnabled()) {

            return false;
        }

        if (chatField == null) {
            return false;
        }

        String text = chatField.getText();

        if (text.isBlank()) {
            return false;
        }

        // click must land on the input line itself
        if (mouseY < chatField.getY() ||
                mouseY >= chatField.getY() + chatField.getHeight()) {

            return false;
        }

        var tr = client.textRenderer;

        int startX = chatField.getX() + 4;

        // find the caret position (char index) under the cursor
        int caret = text.length();

        for (int i = 0; i <= text.length(); i++) {

            int charX = startX + tr.getWidth(
                    text.substring(0, i)
            );

            if (charX > mouseX) {

                caret = Math.max(0, i - 1);
                break;
            }
        }

        if (caret >= text.length()) {
            caret = text.length() - 1;
        }

        if (!isNameChar(text.charAt(caret))) {
            return false;
        }

        // expand to word boundaries
        int start = caret;

        while (start > 0 &&
                isNameChar(text.charAt(start - 1))) {

            start--;
        }

        int end = caret + 1;

        while (end < text.length() &&
                isNameChar(text.charAt(end))) {

            end++;
        }

        String token = text.substring(start, end);

        if (token.length() < 3) {
            return false;
        }

        PlayerContext resolved =
                PlayerResolver.resolve(token)
                        .orElse(null);

        if (resolved == null) {

            // partial match only counts when unambiguous
            var candidates =
                    PlayerResolver.findPlayers(token);

            if (candidates.size() == 1) {
                resolved = candidates.get(0);
            }
        }

        if (resolved == null) {
            return false;
        }

        PlayerActionOverlay.INSTANCE.open(resolved);

        return true;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_';
    }
}
