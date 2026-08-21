package com.uwdie.stafftools.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class EmojiPicker {

    public static final String[] EMOJIS = {
            "😀", "😅", "😂", "😊", "😎", "🤔", "😭", "😡",
            "💀", "❤", "🔥", "⚡", "⭐", "🌟", "☀", "🌙",
            "❄", "⚠", "✅", "❌", "🚫", "🔇", "🔊", "👢",
            "👀", "📋", "📌", "💰", "🎯", "🏆", "🚀", "☠",
            "✋", "👍", "👎", "🎉", "🔔", "🔒", "🔑", "👑"
    };

    private static final int COLS = 8;
    private static final int CELL = 18;
    private static final int PAD = 6;

    private boolean open;
    private int x;
    private int y;
    private Consumer<String> onPick;

    private int panelWidth;
    private int panelHeight;

    public boolean isOpen() {
        return open;
    }

    public void open(
            int x,
            int y,
            Consumer<String> onPick
    ) {

        this.x = x;
        this.y = y;
        this.onPick = onPick;
        this.open = true;

        int rows = (int) Math.ceil(
                (double) EMOJIS.length / COLS
        );

        this.panelWidth =
                PAD * 2 + COLS * CELL;

        this.panelHeight =
                PAD * 2 + rows * CELL;

        MinecraftClient client =
                MinecraftClient.getInstance();

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        if (this.x + panelWidth > screenWidth) {
            this.x = screenWidth - panelWidth;
        }

        if (this.y + panelHeight > screenHeight) {
            this.y = screenHeight - panelHeight;
        }
    }

    public void close() {
        this.open = false;
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY
    ) {

        if (!open) {
            return false;
        }

        if (mouseX < x ||
                mouseX > x + panelWidth ||
                mouseY < y ||
                mouseY > y + panelHeight) {

            close();

            return false;
        }

        int col = (int) ((mouseX - x - PAD) / CELL);
        int row = (int) ((mouseY - y - PAD) / CELL);

        int index = row * COLS + col;

        if (index >= 0 &&
                index < EMOJIS.length &&
                onPick != null) {

            onPick.accept(EMOJIS[index]);
        }

        close();

        return true;
    }

    public void render(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {

        if (!open) {
            return;
        }

        float alpha = 1f;

        Ui.drawPanel(
                context,
                x,
                y,
                panelWidth,
                panelHeight,
                alpha
        );

        MinecraftClient client =
                MinecraftClient.getInstance();

        for (int i = 0; i < EMOJIS.length; i++) {

            int col = i % COLS;
            int row = i / COLS;

            int cellX = x + PAD + col * CELL;
            int cellY = y + PAD + row * CELL;

            boolean hovered =
                    mouseX >= cellX &&
                            mouseX < cellX + CELL &&
                            mouseY >= cellY &&
                            mouseY < cellY + CELL;

            if (hovered) {

                context.fill(
                        cellX,
                        cellY,
                        cellX + CELL,
                        cellY + CELL,
                        Ui.argb(0x403F8AE0, alpha)
                );
            }

            int textX = cellX + (CELL - client.textRenderer
                    .getWidth(EMOJIS[i])) / 2;

            int textY = cellY + (CELL - 9) / 2;

            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(EMOJIS[i]),
                    textX,
                    textY,
                    Ui.argb(Ui.TEXT, alpha)
            );
        }
    }
}
