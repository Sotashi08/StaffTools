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

            paintEdges(
                    context,
                    x, y, width, height, r,
                    top, side,
                    i, Math.min(i + 3, perimeter),
                    1, c
            );
        }
    }

    /** Smooth 0..1 ramp, flat at both ends. */
    public static float smoothstep(float edge0, float edge1, float t) {

        if (edge1 == edge0) {
            return t < edge0 ? 0f : 1f;
        }

        float x = clamp01((t - edge0) / (edge1 - edge0));

        return x * x * (3f - 2f * x);
    }

    /**
     * Paints the {@code [from, to)} slice of the frame perimeter.
     * The slice is wrapped into {@code [0, perimeter)} first, so it may
     * cross the seam between the left and the top edge.
     */
    private static void strokePerimeter(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int top,
            int side,
            int perimeter,
            int from,
            int to,
            int thickness,
            int color
    ) {

        int len = to - from;

        if (len <= 0 || perimeter <= 0) {
            return;
        }

        int cursor = Math.floorMod(from, perimeter);

        while (len > 0) {

            int end = Math.min(cursor + len, perimeter);

            paintEdges(
                    context,
                    x, y, width, height, radius,
                    top, side,
                    cursor, end,
                    thickness, color
            );

            len -= end - cursor;
            cursor = 0;
        }
    }

    /**
     * Paints one straight border edge of a rounded rectangle as a
     * {@code thickness} px stroke centred on the edge line.
     *
     * {@code lineX}/{@code lineY} is the point at perimeter offset
     * {@code edgeStart}; the edge runs {@code edgeLength} pixels in the
     * {@code (dirX, dirY)} direction. Only the part of the slice that
     * belongs to this edge is painted.
     */
    private static void paintEdges(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int top,
            int side,
            int from,
            int to,
            int thickness,
            int color
    ) {

        // top edge: left -> right
        edgeSpan(context, x + radius, y, from, to,
                0, top, 1, 0, thickness, color);

        // right edge: top -> bottom
        edgeSpan(context, x + width - 1, y + radius, from, to,
                top, side, 0, 1, thickness, color);

        // bottom edge: right -> left
        edgeSpan(context, x + width - radius - 1, y + height - 1, from, to,
                top + side, top, -1, 0, thickness, color);

        // left edge: bottom -> top
        edgeSpan(context, x, y + height - radius - 1, from, to,
                top + side + top, side, 0, -1, thickness, color);
    }

    /** Edge slice painter backing {@link #paintEdges}. */
    private static void edgeSpan(
            DrawContext context,
            int lineX,
            int lineY,
            int from,
            int to,
            int edgeStart,
            int edgeLength,
            int dirX,
            int dirY,
            int thickness,
            int color
    ) {

        int s = Math.max(from, edgeStart);
        int e = Math.min(to, edgeStart + edgeLength);

        if (e <= s) {
            return;
        }

        int half = (thickness - 1) / 2;

        int ax = lineX + (s - edgeStart) * dirX;
        int ay = lineY + (s - edgeStart) * dirY;
        int bx = lineX + (e - edgeStart) * dirX;
        int by = lineY + (e - edgeStart) * dirY;

        int x1;
        int x2;
        int y1;
        int y2;

        if (dirX != 0) {

            x1 = Math.min(ax, bx);
            x2 = Math.max(ax, bx);

            // the stroke grows perpendicular to the direction of travel
            y1 = ay - half;
            y2 = ay + thickness - half;

        } else {

            x1 = ax - half;
            x2 = ax + thickness - half;

            y1 = Math.min(ay, by);
            y2 = Math.max(ay, by);
        }

        context.fill(x1, y1, x2, y2, color);
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

    /**
     * Animated overlay frame for the main menu: four smooth, high-contrast
     * bars orbiting the panel border clockwise.
     *
     * Each bar is a theme gradient that is darkest at both ends and
     * brightest at its centre — {@code ___---====---___} — with feathered
     * edges and a layered glow. Motion is sub-pixel smooth: a bar is not
     * drawn as whole pixels but as half-pixel slices, so nothing steps
     * and both ends read as feathered instead of cut.
     * Colours are read live from the theme, so a theme switch applies on
     * the very next frame.
     */
    public static void drawOrbitFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        int inset = 5;
        int radius = 6;

        int ix = x + inset;
        int iy = y + inset;
        int iw = width - inset * 2;
        int ih = height - inset * 2;

        if (iw <= radius * 2 || ih <= radius * 2
                || alpha <= 0.004f) {
            return;
        }

        // resting hairline, so the frame reads even when idle
        drawRoundBorder(
                context,
                ix, iy, iw, ih, radius,
                argb(mix(PANEL_BORDER, ACCENT, 0.35f), alpha * 0.55f)
        );

        int top = iw - radius * 2;
        int side = ih - radius * 2;
        int perimeter = 2 * top + 2 * side;

        if (perimeter <= 0) {
            return;
        }

        long time = System.currentTimeMillis();

        double period = 9000.0;   // ms per full lap

        // double precision, so a long-running session stays smooth
        // (float loses all precision at epoch-scale timestamps)
        double phase = (time / period) % 1.0;

        float phaseT = (float) (phase * perimeter);

        int bars = 4;

        int barLength = Math.max(
                48,
                Math.min(150, perimeter / 6)
        );

        for (int i = 0; i < bars; i++) {

            // tail of the bar, running clockwise from the top-left
            float tail =
                    (phaseT + i * (perimeter / (float) bars)
                            + perimeter) % perimeter;

            drawOrbitBar(
                    context,
                    ix, iy, iw, ih, radius,
                    top, side, perimeter,
                    tail, barLength,
                    alpha
            );
        }
    }

    /**
     * One bar: an alpha envelope that is darkest at both ends and
     * brightest in the middle, so the bar reads as
     * {@code ___---====---___}.
     *
     * The bar is painted as half-pixel slices — two per screen pixel —
     * so the alpha of every slice is a genuine average over its own
     * width and the travel is sub-pixel smooth.
     */
    private static void drawOrbitBar(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int top,
            int side,
            int perimeter,
            float tail,
            int length,
            float alpha
    ) {

        if (length <= 0 || alpha <= 0.004f) {
            return;
        }

        // tail colour: accent nudged towards white, so the bar stays
        // richer than the panel edge instead of a flat grey line
        int edge = mix(ACCENT, TEXT, 0.10f);

        // centre colour: accent brightened towards white — this is what
        // makes the bar read brighter than the base theme colour
        int core = mix(mix(ACCENT_2, TEXT, 0.60f), ACCENT, 0.20f);

        // glow colour: a little dimmer, so the halo reads as light
        // bleeding out of the bar instead of a second bar
        int glow = mix(edge, core, 0.45f);

        int slices = length * 2;

        for (int i = 0; i < slices; i++) {

            // 0 at the tail, 1 at the head
            float t = (i + 0.5f) / slices;

            // 0 at both ends of the bar, 1 at its centre
            float m = 1f - Math.abs(t * 2f - 1f);

            float a = barEnvelope(m);

            if (a <= 0.004f) {
                continue;
            }

            float pos = tail + t * length;

            // half-pixel slice: [pos - 0.5, pos + 0.5)
            int p0 = Math.round(pos - 0.5f);
            int p1 = p0 + 1;

            int rgb = mix(edge, core, smoothstep(0.10f, 0.85f, m));

            // wide soft halo, drawn under the core
            strokePerimeter(
                    context,
                    x, y, width, height, radius,
                    top, side, perimeter,
                    p0, p1,
                    5,
                    argb(glow, alpha * a * 0.10f)
            );

            // tight halo: most of the glow
            strokePerimeter(
                    context,
                    x, y, width, height, radius,
                    top, side, perimeter,
                    p0, p1,
                    3,
                    argb(rgb, alpha * a * 0.30f)
            );

            // bright core
            strokePerimeter(
                    context,
                    x, y, width, height, radius,
                    top, side, perimeter,
                    p0, p1,
                    1,
                    argb(rgb, alpha * a)
            );
        }
    }

    /**
     * Brightness envelope along one bar, symmetric about its centre:
     * a soft ramp up, a wide plateau and a soft ramp down, so both ends
     * are feathered instead of cut.
     */
    private static float barEnvelope(float m) {

        if (m <= 0f) {
            return 0f;
        }

        // ramp up from the edge ...
        float up = smoothstep(0f, 0.42f, m);

        // ... flatten into a plateau instead of a hard peak
        return up * (1f - 0.12f * smoothstep(0.86f, 1f, m));
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
