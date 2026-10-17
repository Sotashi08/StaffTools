package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Colour picker for the chat nick highlight.
 *
 * A full RGB picker is overkill here, so the widget shows a fixed palette
 * of swatches plus a "custom" row that cycles through the 16 vanilla
 * colours and their bright variants. Clicking a swatch applies it right
 * away through the given callback.
 */
public class StColorPicker extends ButtonWidget {

    /** Sixteen vanilla colours, in the order the game lists them. */
    private static final int[] COLORS = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    /** A few shades the vanilla set does not cover well. */
    private static final int[] EXTRA = {
            0xFFD24A4A, 0xFF4AD24A, 0xFF4A82D2, 0xFFD2C14A,
            0xFFD24AD2, 0xFF4AD2C1, 0xFF9B6FE3, 0xFF3F8AE0,
            0xFF2FCC8F, 0xFFD9A94E, 0xFFFF8A8A, 0xFF7DBCFF
    };

    private static final int SWATCH = 14;
    private static final int GAP = 3;

    private final java.util.function.IntConsumer onPick;
    private final int startX;
    private final int startY;
    private final int perRow;

    private int selected;

    public StColorPicker(
            int x,
            int y,
            int width,
            int current,
            java.util.function.IntConsumer onPick
    ) {

        // the widget spans every swatch row, not just the first one:
        // its declared height is what the screen uses for hit testing
        super(
                x,
                y,
                width,
                heightOf(width),
                net.minecraft.text.Text.literal(""),
                b -> {
                },
                DEFAULT_NARRATION_SUPPLIER
        );

        this.onPick = onPick;
        this.startX = x;
        this.startY = y;
        this.perRow = Math.max(
                1,
                (width + GAP) / (SWATCH + GAP)
        );

        this.selected = nearestIndex(current);
    }

    public int getSelected() {
        return colorAt(selected);
    }

    @Override
    protected void drawIcon(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        TextRenderer tr =
                MinecraftClient.getInstance().textRenderer;

        int index = 0;
        int row = 0;
        int col = 0;

        for (int color : COLORS) {

            drawSwatch(context, index, color, mouseX, mouseY);

            index++;
            col++;

            if (col >= perRow) {
                col = 0;
                row++;
            }
        }

        for (int color : EXTRA) {

            drawSwatch(context, index, color, mouseX, mouseY);

            index++;
            col++;

            if (col >= perRow) {
                col = 0;
                row++;
            }
        }

        // a preview line so the chosen color is visible in context
        int previewY =
                startY + rows() * (SWATCH + GAP) + 1;

        String sample = "Player";

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(sample),
                startX,
                previewY,
                colorAt(selected)
        );
    }

    private int rows() {

        int total = COLORS.length + EXTRA.length;

        return (total + perRow - 1) / perRow;
    }

    /** How tall the picker needs to be, swatches plus the preview. */
    public static int heightOf(int width) {

        int perRow = Math.max(
                1,
                (width + GAP) / (SWATCH + GAP)
        );

        int total = COLORS.length + EXTRA.length;

        int rows = (total + perRow - 1) / perRow;

        return rows * (SWATCH + GAP) + 14;
    }

    private void drawSwatch(
            DrawContext context,
            int index,
            int color,
            int mouseX,
            int mouseY
    ) {

        int row = index / perRow;
        int col = index % perRow;

        int x = startX + col * (SWATCH + GAP);
        int y = startY + row * (SWATCH + GAP);

        boolean hovered =
                mouseX >= x && mouseX < x + SWATCH &&
                        mouseY >= y && mouseY < y + SWATCH;

        boolean active = index == selected;

        context.fill(
                x,
                y,
                x + SWATCH,
                y + SWATCH,
                color
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                SWATCH,
                SWATCH,
                2,
                active
                        ? Ui.TEXT
                        : hovered ? Ui.ACCENT : 0xFF000000
        );

        if (hovered) {
            context.fill(
                    x,
                    y,
                    x + SWATCH,
                    y + 1,
                    0xFFFFFFFF
            );
        }
    }

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        if (click.button() != 0) {
            return false;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        int total = COLORS.length + EXTRA.length;

        for (int i = 0; i < total; i++) {

            int row = i / perRow;
            int col = i % perRow;

            int x = startX + col * (SWATCH + GAP);
            int y = startY + row * (SWATCH + GAP);

            if (mouseX >= x && mouseX < x + SWATCH &&
                    mouseY >= y && mouseY < y + SWATCH) {

                selected = i;
                onPick.accept(colorAt(i));

                Ui.playHover();

                return true;
            }
        }

        return false;
    }

    private static int colorAt(int index) {

        return index < COLORS.length
                ? COLORS[index]
                : EXTRA[index - COLORS.length];
    }

    private static int nearestIndex(int color) {

        int best = 0;
        long bestDistance = Long.MAX_VALUE;

        int total = COLORS.length + EXTRA.length;

        for (int i = 0; i < total; i++) {

            int candidate = colorAt(i);

            long dr =
                    ((candidate >> 16) & 0xFF) - ((color >> 16) & 0xFF);
            long dg =
                    ((candidate >> 8) & 0xFF) - ((color >> 8) & 0xFF);
            long db =
                    (candidate & 0xFF) - (color & 0xFF);

            long distance =
                    dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {

                bestDistance = distance;
                best = i;
            }
        }

        return best;
    }
}