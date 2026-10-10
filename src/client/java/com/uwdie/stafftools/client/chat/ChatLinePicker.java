package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;

/**
 * Finds the word the cursor sits on inside a rendered chat line.
 *
 * Vanilla itself resolves the click position to a {@code Style} with
 * exactly this math ({@code ChatHud#getTextStyleAt}); we repeat it to
 * get the whole word back, which makes it possible to react to a click
 * on a nick even when the server did not attach any click event to it.
 */
public final class ChatLinePicker {

    private ChatLinePicker() {
    }

    /**
     * @return the name-shaped word under the cursor in the chat, or
     *         {@code null} when the cursor is not on a chat line.
     */
    public static String wordAt(double mouseX, double mouseY) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return null;
        }

        ChatHud hud = client.inGameHud.getChatHud();

        if (hud == null) {
            return null;
        }

        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        // screen coords -> chat line coords (identical to vanilla)
        double lineX = mouseX / hud.getChatScale() - 4.0;

        double lineY =
                (client.getWindow().getScaledHeight()
                        - mouseY - 40.0)
                        / (hud.getChatScale()
                        * accessor.stafftools$getLineHeight());

        int index = accessor.stafftools$getMessageLineIndex(
                lineX,
                lineY
        );

        if (index < 0) {
            return null;
        }

        var visible = accessor.stafftools$getVisibleMessages();

        if (index >= visible.size()) {
            return null;
        }

        ChatHudLine.Visible line = visible.get(index);

        if (line == null) {
            return null;
        }

        int charAt = MathHelper.floor(lineX);

        if (charAt < 0) {
            return null;
        }

        String text = textOf(line.content());

        if (text.isEmpty() || charAt >= text.length()) {
            return null;
        }

        // word boundaries around the clicked character
        int start = charAt;
        int end = charAt;

        while (start > 0 &&
                TextNormalizer.isNameChar(text.charAt(start - 1))) {

            start--;
        }

        while (end < text.length() &&
                TextNormalizer.isNameChar(text.charAt(end))) {

            end++;
        }

        String word = text.substring(start, end);

        return word.isEmpty() ? null : word;
    }

    /** Renders an ordered text back into a plain string. */
    private static String textOf(OrderedText ordered) {

        if (ordered == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        ordered.accept(
                (index, style, codePoint) -> {
                    builder.appendCodePoint(codePoint);
                    return true;
                }
        );

        return builder.toString();
    }
}
