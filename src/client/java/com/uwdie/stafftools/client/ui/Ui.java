package com.uwdie.stafftools.client.ui;

import net.minecraft.client.gui.DrawContext;

public final class Ui {

    public static final int ACCENT = 0xFF3F8AE0;
    public static final int ACCENT_SOFT = 0xFF7DBCFF;
    public static final int ACCENT_DARK = 0xFF2A5D9E;
    public static final int PANEL_BORDER = 0xFF2E5E9E;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFAAAAAA;
    public static final int TEXT_MUTED = 0xFF777777;
    public static final int DANGER = 0xFFFF5555;
    public static final int DANGER_SOFT = 0xFFFF8A8A;
    public static final int SUCCESS = 0xFF55DD77;
    public static final int WARNING = 0xFFEEBB55;
    public static final int PANEL_BG = 0xF0141418;
    public static final int PANEL_BG_SOFT = 0x22000000;

    private Ui() {
    }

    public static int argb(int color, float alpha) {
        int a = (int) ((color >>> 24) * alpha);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    public static float easeOutCubic(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    /** Opaque RGB mix between two colors. */
    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    /** Simple rounded rectangle drawn with two overlapping fills. */
    public static void drawRoundRect(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {

        if (width <= 0 || height <= 0) {
            return;
        }

        int r = Math.min(radius, Math.min(width, height) / 2);

        context.fill(
                x + r, y,
                x + width - r, y + height,
                color
        );

        context.fill(
                x, y + r,
                x + width, y + height - r,
                color
        );

        if (r >= 2) {
            context.fill(
                    x + 1, y + 1,
                    x + 2, y + 2,
                    color
            );
            context.fill(
                    x + width - 2, y + 1,
                    x + width - 1, y + 2,
                    color
            );
            context.fill(
                    x + 1, y + height - 2,
                    x + 2, y + height - 1,
                    color
            );
            context.fill(
                    x + width - 2, y + height - 2,
                    x + width - 1, y + height - 1,
                    color
            );
        }
    }

    /** Rounded 1px border around a rounded rectangle. */
    public static void drawRoundBorder(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {

        int r = Math.min(radius, Math.min(width, height) / 2);

        context.fill(
                x + r, y,
                x + width - r, y + 1,
                color
        );

        context.fill(
                x + r, y + height - 1,
                x + width - r, y + height,
                color
        );

        context.fill(
                x, y + r,
                x + 1, y + height - r,
                color
        );

        context.fill(
                x + width - 1, y + r,
                x + width, y + height - r,
                color
        );

        if (r >= 2) {
            context.fill(
                    x + 1, y + 1,
                    x + 2, y + 2,
                    color
            );
            context.fill(
                    x + width - 2, y + 1,
                    x + width - 1, y + 2,
                    color
            );
            context.fill(
                    x + 1, y + height - 2,
                    x + 2, y + height - 1,
                    color
            );
            context.fill(
                    x + width - 2, y + height - 2,
                    x + width - 1, y + height - 1,
                    color
            );
        }
    }

    public static void drawPanel(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        drawRoundRect(
                context,
                x,
                y,
                width,
                height,
                4,
                argb(PANEL_BG, alpha)
        );

        drawRoundBorder(
                context,
                x,
                y,
                width,
                height,
                4,
                argb(PANEL_BORDER, alpha)
        );

        context.fill(
                x + 1,
                y + height - 1,
                x + width - 1,
                y + height,
                argb(ACCENT, alpha * 0.8f)
        );
    }

    public static void drawHeader(
            DrawContext context,
            int x,
            int y,
            int width,
            float alpha
    ) {

        context.fill(
                x,
                y,
                x + width,
                y + 2,
                argb(ACCENT, alpha)
        );
    }
}
