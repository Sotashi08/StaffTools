package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Finds the word the cursor sits on inside a rendered chat line.
 *
 * The line lookup uses the chat scale, width, line height, visible line
 * count and scroll offset used by the renderer. Within a line, cursor X
 * is matched against rendered glyph advances rather than treated as a
 * character index.
 *
 * The line under the cursor is checked first. If it contains no word,
 * the nearest visible line is checked to tolerate small vertical
 * rounding differences.
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

        String text = textOf(line.content());

        if (text.isEmpty()) {
            return null;
        }

        int charAt = characterAt(line.content(), text, lineX);

        if (charAt < 0 || charAt >= text.length()) {
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

    /**
     * Maps a rendered X position to the UTF-16 offset of its glyph.
     * Styled glyph widths are measured separately so bold/custom-font
     * text uses the same advances as the displayed line.
     */
    private static int characterAt(
            OrderedText ordered,
            String text,
            double lineX
    ) {

        if (lineX < 0.0) {
            return -1;
        }

        TextRenderer renderer =
                MinecraftClient.getInstance().textRenderer;

        int[] glyphWidths = new int[
                text.codePointCount(0, text.length())
        ];
        int[] glyphCount = {0};

        ordered.accept((index, style, codePoint) -> {

            if (glyphCount[0] >= glyphWidths.length) {
                return false;
            }

            String glyph = new String(Character.toChars(codePoint));
            glyphWidths[glyphCount[0]++] = renderer.getWidth(
                    Text.literal(glyph).setStyle(style)
            );
            return true;
        });

        return characterAt(text, lineX, glyphWidths, glyphCount[0]);
    }

    static int characterAt(
            String text,
            double lineX,
            int[] glyphWidths,
            int glyphCount
    ) {

        if (lineX < 0.0 || text == null || text.isEmpty()) {
            return -1;
        }

        double cursorX = 0.0;
        int availableGlyphs = Math.min(
                glyphCount,
                Math.min(
                        glyphWidths.length,
                        text.codePointCount(0, text.length())
                )
        );

        for (int i = 0; i < availableGlyphs; i++) {

            int width = glyphWidths[i];
            double glyphRight = cursorX + width;

            if (width > 0 &&
                    lineX >= cursorX &&
                    lineX < glyphRight) {

                return text.offsetByCodePoints(0, i);
            }

            cursorX = glyphRight;
        }

        return -1;
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

    /**
     * The full text of the chat line under the cursor, or {@code null}
     * when the cursor is not on a chat line. Used for Alt+click copy.
     */
    public static String lineTextAt(double mouseX, double mouseY) {

        ChatHudLine.Visible line = pickLine(mouseX, mouseY);

        if (line == null) {
            return null;
        }

        String text = textOf(line.content());

        return text.isEmpty() ? null : text;
    }

    /** The chat line the cursor currently points at, or null. */
    private static ChatHudLine.Visible pickLine(double mouseX, double mouseY) {

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

        double chatScale = accessor.stafftools$getChatScale();

        double lineX = mouseX / chatScale - 4.0;

        double lineY =
                (client.getWindow().getScaledHeight()
                        - mouseY - 40.0)
                        / (chatScale
                        * accessor.stafftools$getLineHeight());

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

        int visibleCount = Math.min(
                hud.getVisibleLineCount(),
                visible.size()
        );

        if (visibleCount <= 0) {
            return null;
        }

        double shifted = lineY + accessor.stafftools$getScrolledLines();

        int index = MathHelper.floor(shifted);

        if (index >= 0 && index < visibleCount) {

            return visible.get(index);
        }

        int nearest = Math.round((float) shifted);

        if (nearest >= 0 && nearest < visibleCount) {

            return visible.get(nearest);
        }

        return null;
    }
}
