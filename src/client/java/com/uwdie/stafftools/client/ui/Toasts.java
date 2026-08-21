package com.uwdie.stafftools.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;

/**
 * Lightweight toast notification system (top-right corner).
 *
 * Slide-in from the right with ease-out, hold, fade-out with a slight
 * drift. Colored side bar indicates the toast kind. Renders anywhere:
 * on screens (via mixins) and directly on the HUD.
 */
public final class Toasts {

    public static final Toasts INSTANCE = new Toasts();

    private static final int MAX = 4;

    private static final long IN_MS = 200;
    private static final long HOLD_MS = 2600;
    private static final long OUT_MS = 280;

    private static final int H = 18;
    private static final int GAP = 4;

    private record Toast(String text, int accent, long born) {
    }

    private final ArrayDeque<Toast> active =
            new ArrayDeque<>();

    private Toasts() {
    }

    public void info(String text) {
        push(text, Ui.ACCENT_SOFT);
    }

    public void success(String text) {
        push(text, Ui.SUCCESS);
    }

    public void warn(String text) {
        push(text, Ui.WARNING);
    }

    public void danger(String text) {
        push(text, Ui.DANGER);
    }

    private void push(String text, int accent) {

        if (!com.uwdie.stafftools.client.StafftoolsClient
                .getConfig()
                .isToastsEnabled()) {

            return;
        }

        while (active.size() >= MAX) {
            active.pollFirst();
        }

        active.addLast(
                new Toast(
                        text,
                        accent,
                        System.currentTimeMillis()
                )
        );
    }

    public void render(DrawContext context) {

        if (active.isEmpty()) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        var tr = client.textRenderer;

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        long now = System.currentTimeMillis();

        active.removeIf(t ->
                now - t.born() >
                        IN_MS + HOLD_MS + OUT_MS);

        // stacked from the bottom edge upward
        int index = 0;

        for (Toast t : active) {

            long age = now - t.born();

            float in =
                    Ui.clamp01(age / (float) IN_MS);

            float out =
                    Ui.clamp01(
                            (age - IN_MS - HOLD_MS)
                                    / (float) OUT_MS
                    );

            float a = Math.min(in, 1f - out);

            if (a <= 0.01f) {
                continue;
            }

            float slide =
                    (1f - Ui.easeOutCubic(in)) * 20f
                            + out * 10f;

            int w = Math.min(
                    tr.getWidth(t.text()) + 18,
                    screenWidth / 2
            );

            int x = screenWidth - w - 8 + (int) slide;

            int y = screenHeight - 8 - H
                    - index * (H + GAP);

            String text =
                    tr.trimToWidth(t.text(), w - 18);

            Ui.drawRoundRect(
                    context, x, y, w, H, 3,
                    Ui.argb(Ui.PANEL_BG, a)
            );

            Ui.drawRoundBorder(
                    context, x, y, w, H, 3,
                    Ui.argb(0xFF2C3B55, a)
            );

            context.fill(
                    x, y + 2, x + 2, y + H - 2,
                    Ui.argb(t.accent(), a)
            );

            context.drawTextWithShadow(
                    tr,
                    Text.literal(text),
                    x + 8,
                    y + (H - 9) / 2,
                    Ui.argb(Ui.TEXT, a)
            );

            y += H + GAP;

            index++;
        }
    }
}
