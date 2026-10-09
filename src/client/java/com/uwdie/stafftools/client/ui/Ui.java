package com.uwdie.stafftools.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/**
 * Render utilities + the live palette.
 *
 * Color fields are NOT final: {@link Theme#apply(int)} overwrites them
 * when the user picks a theme. Every widget reads these at render time,
 * so a theme switch applies instantly on the next frame.
 */
public final class Ui {

    public static int ACCENT = 0xFF3F8AE0;
    public static int ACCENT_2 = 0xFF7DBCFF;
    public static int ACCENT_SOFT = 0xFF7DBCFF;
    public static int ACCENT_DARK = 0xFF2A5D9E;
    public static int PANEL_BORDER = 0xFF2E5E9E;
    public static int TEXT = 0xFFFFFFFF;
    public static int TEXT_DIM = 0xFFAAAAAA;
    public static int TEXT_MUTED = 0xFF777777;
    public static int DANGER = 0xFFFF5555;
    public static int DANGER_SOFT = 0xFFFF8A8A;
    public static int SUCCESS = 0xFF55DD77;
    public static int WARNING = 0xFFEEBB55;
    public static int PANEL_BG = 0xF0141418;
    public static int PANEL_BG_SOFT = 0x22000000;

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

    /** Smooth sine bump in 0..1, peaks in the middle. */
    public static float wave(float t) {
        return 1f - Math.abs(2f * clamp01(t) - 1f);
    }

    /** Vertical counterpart of {@link #drawGradientH}. */
    public static void drawGradientV(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int colorTop,
            int colorBottom,
            float alpha
    ) {

        int steps = Math.max(1, h / 2);

        for (int i = 0; i < steps; i++) {

            float t = steps == 1
                    ? 0f
                    : i / (float) (steps - 1);

            int segY = y + i * h / steps;
            int segEnd = y + (i + 1) * h / steps;

            context.fill(
                    x,
                    segY,
                    x + w,
                    Math.max(segY + 1, segEnd),
                    argb(mix(colorTop, colorBottom, t), alpha)
            );
        }
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

        // bottom accent bar: gradient when the theme defines one
        if (ACCENT_2 != ACCENT) {

            drawGradientH(
                    context,
                    x + 1,
                    y + height - 1,
                    width - 2,
                    1,
                    ACCENT,
                    ACCENT_2,
                    alpha * 0.8f
            );

        } else {

            context.fill(
                    x + 1,
                    y + height - 1,
                    x + width - 1,
                    y + height,
                    argb(ACCENT, alpha * 0.8f)
            );
        }
    }

    /**
     * Horizontal gradient drawn as thin strips (cheap, no shader).
     */
    public static void drawGradientH(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int colorFrom,
            int colorTo,
            float alpha
    ) {

        int steps = Math.max(1, w / 2);

        for (int i = 0; i < steps; i++) {

            float t = steps == 1
                    ? 0f
                    : i / (float) (steps - 1);

            int segX = x + i * w / steps;
            int segEnd = x + (i + 1) * w / steps;

            context.fill(
                    segX,
                    y,
                    Math.max(segX + 1, segEnd),
                    y + h,
                    argb(mix(colorFrom, colorTo, t), alpha)
            );
        }
    }

    /**
     * Rounded 1px border whose colour flows around the whole perimeter,
     * corners included, so the frame reads as one continuous stroke
     * instead of four flat lines.
     */
    public static void drawFlowingBorder(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int colorFrom,
            int colorTo,
            float alpha
    ) {

        if (width <= 0 || height <= 0) {
            return;
        }

        int r = Math.min(radius, Math.min(width, height) / 2);

        int top = width - 2 * r;
        int side = height - 2 * r;
        int perimeter = 2 * top + 2 * side;

        if (perimeter <= 0) {
            return;
        }

        // corners first, in a mid tone; the edges are drawn over them
        drawRoundBorder(
                context,
                x,
                y,
                width,
                height,
                r,
                argb(mix(colorFrom, colorTo, 0.5f), alpha)
        );

        // 3px segments: the palette shifts by less than 1 colour unit
        // per step, so the flow stays perfectly smooth
        for (int i = 0; i < perimeter; i += 3) {

            float t = wave(i / (float) (perimeter - 1));
            int c = argb(mix(colorFrom, colorTo, t), alpha);

            int j = Math.min(i + 3, perimeter);

            // top edge: left -> right
            fillArc(context, x + r, y, i, j, 0, top, 1, 0, c);

            // right edge: top -> bottom
            fillArc(context, x + width - 1, y + r, i, j, top, side,
                    0, 1, c);

            // bottom edge: right -> left
            fillArc(context, x + width - r - 1, y + height - 1,
                    i, j, top + side, top, -1, 0, c);

            // left edge: bottom -> top
            fillArc(context, x, y + height - r - 1,
                    i, j, top + side + top, side, 0, -1, c);
        }
    }

    /** Paints the {@code [from,to)} slice of one straight border edge. */
    private static void fillArc(
            DrawContext context,
            int originX,
            int originY,
            int i,
            int j,
            int start,
            int length,
            int stepX,
            int stepY,
            int color
    ) {

        int from = Math.max(i, start);
        int to = Math.min(j, start + length);

        if (to <= from) {
            return;
        }

        int x1 = originX + (from - start) * stepX;
        int y1 = originY + (from - start) * stepY;
        int x2 = originX + (to - start) * stepX + Math.max(stepX, 0);
        int y2 = originY + (to - start) * stepY + Math.max(stepY, 0);

        context.fill(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.max(x1, x2),
                Math.max(y1, y2),
                color
        );
    }

    /**
     * Cohesive menu frame: soft drop shadow, glass body with a top sheen,
     * one flowing accent border around the whole perimeter, an inner
     * hairline highlight and matching top/bottom ribbons.
     * Used by the in-chat player popups.
     */
    public static void drawPopupFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        int radius = 4;

        if (width <= 0 || height <= 0) {
            return;
        }

        int shadow = mix(0xFF000000, ACCENT, 0.22f);

        // drop shadow: soft layers expanding outwards
        for (int i = 3; i >= 1; i--) {

            drawRoundBorder(
                    context,
                    x - i, y - i,
                    width + i * 2, height + i * 2,
                    radius + i,
                    argb(shadow, alpha * (0.34f / i))
            );
        }

        // glass body
        drawRoundRect(
                context,
                x,
                y,
                width,
                height,
                radius,
                argb(PANEL_BG, alpha)
        );

        int sheen = mix(TEXT, ACCENT, 0.5f);

        // top sheen: light sliding in from the upper border
        if (width > radius * 2 + 4 && height > 16) {

            context.fillGradient(
                    x + radius, y + 2,
                    x + width - radius, y + 13,
                    argb(sheen, alpha * 0.06f),
                    argb(sheen, 0f)
            );
        }

        // bottom depth: slight darkening above the lower border
        if (width > radius * 2 + 4 && height > 20) {

            context.fillGradient(
                    x + radius, y + height - 16,
                    x + width - radius, y + height - 3,
                    argb(0xFF000000, 0f),
                    argb(0xFF000000, alpha * 0.32f)
            );
        }

        // one continuous border around the entire menu
        drawFlowingBorder(
                context,
                x, y, width, height, radius,
                mix(PANEL_BORDER, ACCENT, 0.5f),
                ACCENT_2,
                alpha
        );

        // inner hairline, right below the border (glass thickness)
        drawRoundBorder(
                context,
                x + 1, y + 1,
                width - 2, height - 2,
                Math.max(1, radius - 1),
                argb(mix(TEXT, ACCENT, 0.6f), alpha * 0.08f)
        );

        // accent ribbons that follow the same palette
        drawGradientH(
                context,
                x + radius, y + 1,
                width - radius * 2, 1,
                ACCENT,
                ACCENT_2,
                alpha * 0.9f
        );

        drawGradientH(
                context,
                x + radius, y + height - 2,
                width - radius * 2, 1,
                ACCENT_2,
                ACCENT,
                alpha * 0.45f
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

    /**
     * Very soft tick played when a list row / widget becomes hovered.
     */
    public static void playHover() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null) {
            return;
        }

        client.getSoundManager().play(
                PositionedSoundInstance.master(
                        SoundEvents.BLOCK_NOTE_BLOCK_HAT
                                .value(),
                        2.0f,
                        0.1f
                )
        );
    }
}
