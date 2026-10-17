package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Finds the word the cursor sits on inside a rendered chat line.
 *
 * Vanilla itself resolves the click position to a {@code Style} with
 * exactly this math ({@code ChatHud#getTextStyleAt}); 1.21.11 no longer
 * exposes the line lookup, so the index is computed here from the same
 * values the renderer uses — chat scale, chat width, line height, the
 * visible line count and the current scroll offset.
 *
 * Chat plugins are the reason the lookup is layered: a plugin may add its
 * own chat lines, shift the chat box, or send text with no click data at
 * all. So the exact vanilla hit-test is tried first, and when it finds
 * nothing the search widens to the whole chat area and returns the word
 * of the nearest line. That keeps the click-to-popup usable even where
 * the strict maths would miss.
 */
public final class ChatLinePicker {

    /** How far a click may sit from a chat line, in scaled pixels. */
    private static final double LINE_TOLERANCE = 3.0;

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
        double chatScale = accessor.stafftools$getChatScale();

        double lineX = mouseX / chatScale - 4.0;

        double lineY =
                (client.getWindow().getScaledHeight()
                        - mouseY - 40.0)
                        / (chatScale
                        * accessor.stafftools$getLineHeight());

        // chat position resolution only happens with the chat focused
        // and visible
        if (!hud.isChatFocused()) {
            return null;
        }

        // the message background spans exactly [-4, width / scale]
        double maxX =
                MathHelper.floor(
                        accessor.stafftools$getWidth()
                                / chatScale
                );

        if (lineX < -4.0 || lineX > maxX) {
            return null;
        }

        List<ChatHudLine.Visible> visible =
                accessor.stafftools$getVisibleMessages();

        if (visible.isEmpty()) {
            return null;
        }

        // only as many lines as are actually on screen can be hit
        int visibleCount = Math.min(
                hud.getVisibleLineCount(),
                visible.size()
        );

        if (visibleCount <= 0) {
            return null;
        }

        // scrolled history shifts the line the cursor points at
        double shifted =
                lineY + accessor.stafftools$getScrolledLines();

        int index = MathHelper.floor(shifted);

        String word = null;

        // 1. exact hit, exactly like vanilla
        if (index >= 0 && index < visibleCount) {

            word = wordInLine(
                    visible.get(index),
                    lineX
            );
        }

        // 2. the click sits between two lines: take the closest one, a
        // chat plugin may shift the box by a couple of pixels
        if (word == null) {

            int nearest = Math.round((float) shifted);

            if (nearest >= 0 && nearest < visibleCount) {

                word = wordInLine(
                        visible.get(nearest),
                        lineX
                );
            }
        }

        return word;
    }

    /**
     * The name-shaped word at {@code lineX} inside one rendered line, or
     * {@code null} when that x is outside of it.
     */
    private static String wordInLine(
            ChatHudLine.Visible line,
            double lineX
    ) {

        if (line == null) {
            return null;
        }

        // the char index is the floored line X, exactly like vanilla
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
