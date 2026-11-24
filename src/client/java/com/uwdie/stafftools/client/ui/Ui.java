package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
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

    // ---------------------------------------------------------------
    // palette (dark graphite + purple accent, matching the launcher UI)
    // ---------------------------------------------------------------

    /** Window background: the darkest layer. */
    public static int BG = 0xFF0B0B10;

    /** Primary surface: sidebar, cards, inputs. */
    public static int SURFACE = 0xFF121218;

    /** Secondary surface: hovered cards, nested blocks. */
    public static int SURFACE_2 = 0xFF17171F;

    /** Hairline border used on every surface. */
    public static int BORDER = 0xFF26262F;

    public static int ACCENT = 0xFFAC67ED;
    public static int ACCENT_2 = 0xFF8B5CF6;
    public static int ACCENT_SOFT = 0xFFC9A0F5;
    public static int ACCENT_DARK = 0xFF7C3AED;
    public static int PANEL_BORDER = 0xFF26262F;
    public static int TEXT = 0xFFFFFFFF;
    public static int TEXT_DIM = 0xFFB4B4C0;
    public static int TEXT_MUTED = 0xFF8A8A96;
    public static int DANGER = 0xFFEF4444;
    public static int DANGER_SOFT = 0xFFF87171;
    public static int SUCCESS = 0xFF4ADE80;
    public static int WARNING = 0xFFF59E0B;
    public static int PANEL_BG = 0xFF121218;
    public static int PANEL_BG_SOFT = 0xFF17171F;

    /**
     * Color of a detected nick in chat. Kept out of the theme palette on
     * purpose: it is the one color a moderator tunes by hand, so it is
     * stored in the config and only seeded from the theme on first use.
     */
    public static int NICK_HIGHLIGHT = 0xFFAC67ED;

    private Ui() {
    }

    public static int argb(int color, float alpha) {
        int a = (int) ((color >>> 24) * alpha);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    /** Master switch for every optional motion effect in the UI. */
    public static boolean animationsEnabled() {

        return StafftoolsClient.getConfig().isMenuAnimation();
    }

    /** Configured animation speed, from 0.5x to 2x. */
    public static float animationSpeed() {

        StaffToolsConfig config = StafftoolsClient.getConfig();

        return config.getAnimationSpeed() / 100f;
    }

    /** Configured animated accent-glow strength, from 0 to 1. */
    public static float glowIntensity() {

        StaffToolsConfig config = StafftoolsClient.getConfig();

        return config.getGlowIntensity() / 100f;
    }

    /** Progress for a timed transition, respecting the global switch and speed. */
    public static float animationProgress(
            long startAt,
            long durationMs
    ) {

        if (!animationsEnabled() || durationMs <= 0L) {
            return 1f;
        }

        float elapsed =
                Math.max(0L, System.currentTimeMillis() - startAt)
                        * animationSpeed();

        return clamp01(elapsed / durationMs);
    }

    /** Eased progress for a staggered component entrance. */
    public static float entranceProgress(
            long startAt,
            long delayMs,
            long durationMs
    ) {

        return easeOutCubic(
                animationProgress(startAt + Math.max(0L, delayMs), durationMs)
        );
    }

    /**
     * Frame-rate-independent transition toward a target value.
     * Screen render deltas are partial ticks, so convert them to seconds.
     */
    public static float animateTowards(
            float current,
            float target,
            float delta,
            float rate
    ) {

        return animateTowardsSeconds(
                current,
                target,
                Math.max(0f, delta) * 0.05f,
                rate
        );
    }

    /** Exponential, frame-rate-independent interpolation using elapsed seconds. */
    public static float animateTowardsSeconds(
            float current,
            float target,
            float deltaSeconds,
            float rate
    ) {

        if (!animationsEnabled()) {
            return target;
        }

        double elapsed = Math.max(0f, deltaSeconds) * animationSpeed();
        float factor = (float) (1.0 - Math.exp(-Math.max(0f, rate) * elapsed));

        return current + (target - current) * factor;
    }

    public static float easeOutCubic(float t) {
        float u = 1 - clamp01(t);
        return 1 - u * u * u;
    }

    public static float easeInCubic(float t) {
        float x = clamp01(t);
        return x * x * x;
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

    /** Label, percentage and a draggable horizontal slider. */
    public static void drawSlider(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int width,
            String label,
            int value,
            int min,
            int max,
            boolean hovered,
            float alpha
    ) {

        drawSlider(
                context, tr, x, y, width, label, value, min, max,
                hovered ? 1f : 0f, alpha
        );
    }

    /** Slider with an interpolated hover amount for its knob glow. */
    public static void drawSlider(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int width,
            String label,
            int value,
            int min,
            int max,
            float hover,
            float alpha
    ) {

        float hoverAmount = clamp01(hover);
        String percentage = value + "%";
        int valueWidth = tr.getWidth(percentage);

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(
                        tr.trimToWidth(
                                label,
                                Math.max(1, width - valueWidth - 8)
                        )
                ),
                x,
                y,
                argb(TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(percentage),
                x + width - valueWidth,
                y,
                argb(TEXT, alpha)
        );

        float progress = max <= min
                ? 0f
                : (value - min) / (float) (max - min);
        int trackX = x + 3;
        int trackW = Math.max(1, width - 8);
        int trackY = y + 17;
        int knobX = trackX + Math.round(trackW * clamp01(progress));

        drawRoundRect(
                context,
                trackX,
                trackY,
                trackW,
                4,
                2,
                argb(mix(SURFACE_2, TEXT, 0.06f), alpha)
        );

        int fillW = Math.round(trackW * clamp01(progress));

        if (fillW > 0) {
            drawRoundRect(
                    context,
                    trackX,
                    trackY,
                    fillW,
                    4,
                    2,
                    argb(mix(ACCENT, ACCENT_2, 0.45f), alpha)
            );
        }

        drawHoverGlow(
                context,
                knobX - 4,
                trackY - 3,
                8,
                10,
                4,
                ACCENT,
                hoverAmount,
                alpha
        );

        drawRoundRect(
                context,
                knobX - 3,
                trackY - 2,
                6,
                8,
                3,
                argb(mix(ACCENT_SOFT, TEXT, hoverAmount), alpha)
        );
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

    /**
     * Horizontal inset of a rounded corner at row {@code dy}.
     *
     * <p>The corner is a quarter circle of radius {@code r}; at a given
     * row the fill has to start where the circle crosses that row. This
     * is what makes the corners actually round instead of clipped.
     */
    private static int cornerInset(int r, int dy) {

        double d = r - dy - 0.5;

        double v = (double) r * r - d * d;

        if (v <= 0) {
            return r;
        }

        return (int) Math.round(r - Math.sqrt(v));
    }

    /**
     * Paints the pixels that fall OUTSIDE a rounded rectangle with the
     * given colour, so a square child surface (rail, top bar) can be
     * clipped to the rounded window shape.
     */
    public static void maskOutsideCorners(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {

        int r = Math.min(radius, Math.min(width, height) / 2);

        if (r <= 0) {
            return;
        }

        for (int dy = 0; dy < r; dy++) {

            int inset = cornerInset(r, dy);

            if (inset <= 0) {
                continue;
            }

            // top-left / top-right
            context.fill(x, y + dy, x + inset, y + dy + 1, color);
            context.fill(
                    x + width - inset, y + dy,
                    x + width, y + dy + 1,
                    color
            );

            // bottom-left / bottom-right
            context.fill(
                    x, y + height - dy - 1,
                    x + inset, y + height - dy,
                    color
            );

            context.fill(
                    x + width - inset, y + height - dy - 1,
                    x + width, y + height - dy,
                    color
            );
        }
    }

    /** Rounded rectangle with true per-row corner geometry. */
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

        if (r <= 0) {

            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // straight middle block
        context.fill(
                x, y + r,
                x + width, y + height - r,
                color
        );

        // top and bottom corner rows, inset by the circle
        for (int dy = 0; dy < r; dy++) {

            int inset = cornerInset(r, dy);

            context.fill(
                    x + inset, y + dy,
                    x + width - inset, y + dy + 1,
                    color
            );

            context.fill(
                    x + inset, y + height - dy - 1,
                    x + width - inset, y + height - dy,
                    color
            );
        }
    }

    /** Rounded 1px border with true per-row corner geometry. */
    public static void drawRoundBorder(
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

        if (r <= 0) {

            context.fill(x, y, x + width, y + 1, color);
            context.fill(x, y + height - 1, x + width, y + height, color);
            context.fill(x, y, x + 1, y + height, color);
            context.fill(x + width - 1, y, x + width, y + height, color);
            return;
        }

        // straight edges between the corners
        context.fill(x + r, y, x + width - r, y + 1, color);
        context.fill(x + r, y + height - 1, x + width - r, y + height, color);
        context.fill(x, y + r, x + 1, y + height - r, color);
        context.fill(x + width - 1, y + r, x + width, y + height - r, color);

        // corner arcs: one pixel per row on each side
        for (int dy = 0; dy < r; dy++) {

            int inset = cornerInset(r, dy);

            context.fill(
                    x + inset, y + dy,
                    x + inset + 1, y + dy + 1,
                    color
            );

            context.fill(
                    x + width - inset - 1, y + dy,
                    x + width - inset, y + dy + 1,
                    color
            );

            context.fill(
                    x + inset, y + height - dy - 1,
                    x + inset + 1, y + height - dy,
                    color
            );

            context.fill(
                    x + width - inset - 1, y + height - dy - 1,
                    x + width - inset, y + height - dy,
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

        // soft shadow so the panel reads as a raised surface
        drawShadow(
                context, x, y, width, height,
                Design.RADIUS_LG, 3, 0.16f, alpha
        );

        drawRoundRect(
                context,
                x,
                y,
                width,
                height,
                Design.RADIUS_LG,
                argb(PANEL_BG, alpha)
        );

        drawRoundBorder(
                context,
                x,
                y,
                width,
                height,
                Design.RADIUS_LG,
                argb(PANEL_BORDER, alpha)
        );

        // bottom accent bar: gradient when the theme defines one
        if (ACCENT_2 != ACCENT) {

            drawGradientH(
                    context,
                    x + Design.RADIUS_LG,
                    y + height - 1,
                    width - Design.RADIUS_LG * 2,
                    1,
                    ACCENT,
                    ACCENT_2,
                    alpha * 0.8f
            );

        } else {

            context.fill(
                    x + Design.RADIUS_LG,
                    y + height - 1,
                    x + width - Design.RADIUS_LG,
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
    /**
     * Frame for the in-chat popups: a rounded glass body with a hairline
     * border and a top accent ribbon.
     *
     * <p>No drop shadow: these panels float over the chat, where a shadow
     * only smears the text behind them.
     */
    public static void drawPopupFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        int radius = 8;

        if (width <= 0 || height <= 0) {
            return;
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

        // soft top sheen separates the header from the panel body
        if (width > radius * 2 + 4 && height > 16) {

            context.fillGradient(
                    x + radius, y + 2,
                    x + width - radius, y + 14,
                    argb(sheen, alpha * 0.05f),
                    argb(sheen, 0f)
            );
        }

        // bottom depth: slight darkening above the lower border
        if (width > radius * 2 + 4 && height > 20) {

            context.fillGradient(
                    x + radius, y + height - 18,
                    x + width - radius, y + height - 3,
                    argb(0xFF000000, 0f),
                    argb(0xFF000000, alpha * 0.26f)
            );
        }

        // one continuous rounded border around the entire menu
        drawRoundBorder(
                context,
                x, y, width, height, radius,
                argb(BORDER, alpha)
        );

        // inner hairline, right below the border (glass thickness)
        drawRoundBorder(
                context,
                x + 1, y + 1,
                width - 2, height - 2,
                Math.max(1, radius - 1),
                argb(mix(TEXT, ACCENT, 0.6f), alpha * 0.06f)
        );

        // the same travelling glow as the menus, in its thin preset
        drawOrbitFrameThin(
                context,
                x, y, width, height,
                alpha
        );

        // accent ribbon along the top edge, inset so it follows the
        // rounded corners instead of cutting across them
        drawGradientH(
                context,
                x + radius, y + 1,
                width - radius * 2, 1,
                ACCENT,
                ACCENT_2,
                alpha * 0.9f
        );
    }

    /** Subtle multi-ring accent glow for hovered controls. */
    public static void drawHoverGlow(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int accent,
            float hover,
            float alpha
    ) {

        float strength = animationsEnabled()
                ? clamp01(hover) * glowIntensity()
                : 0f;

        if (strength <= 0.01f || width <= 0 || height <= 0) {
            return;
        }

        for (int layer = 3; layer >= 1; layer--) {

            drawRoundBorder(
                    context,
                    x - layer,
                    y - layer,
                    width + layer * 2,
                    height + layer * 2,
                    radius + layer,
                    argb(accent, alpha * strength * 0.10f / layer)
            );
        }
    }

    /** Legacy style values retained for callers of the frame overload. */
    public record OrbitStyle(
            int inset,
            int radius,
            int bars,
            int barMin,
            int barMax,
            int barDivisor,
            int haloWide,
            int haloTight,
            float wideAlpha,
            float tightAlpha
    ) {

        /** Standard shell frame. */
        public static final OrbitStyle FULL =
                new OrbitStyle(
                        5, 6, 4,
                        48, 150, 6,
                        5, 3,
                        0.10f, 0.30f
                );

        /** Floating popup frame. */
        public static final OrbitStyle THIN =
                new OrbitStyle(
                        3, 4, 2,
                        18, 64, 10,
                        3, 1,
                        0.09f, 0.24f
                );

        /**
         * Chat frame: the lightest preset for a frequently visible
         * surface.
         */
        public static final OrbitStyle CHAT =
                new OrbitStyle(
                        2, 3, 2,
                        16, 48, 14,
                        2, 1,
                        0.08f, 0.22f
                );
    }

    /** Breathes a low-contrast accent around a panel without a moving stripe. */
    public static void drawOrbitFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        drawAmbientFrame(context, x, y, width, height, alpha, 1f);
    }

    /** Narrower ambient frame used by floating chat panels. */
    public static void drawOrbitFrameThin(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {

        drawAmbientFrame(context, x, y, width, height, alpha, 0.65f);
    }

    private static void drawAmbientFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha,
            float strengthScale
    ) {

        float glow = glowIntensity();
        if (!animationsEnabled() || glow <= 0.01f ||
                width <= 0 || height <= 0 || alpha <= 0.004f) {
            return;
        }

        double phase = System.currentTimeMillis()
                * (double) animationSpeed() / 1800.0;
        float breath = (float) (0.5 + 0.5 * Math.sin(phase));
        float intensity = glow * strengthScale * (0.45f + breath * 0.55f);
        int accent = mix(ACCENT, ACCENT_SOFT, 0.12f + breath * 0.18f);
        int radius = Math.min(Design.RADIUS_XL, Math.min(width, height) / 2);

        drawRoundBorder(
                context,
                x - 1,
                y - 1,
                width + 2,
                height + 2,
                radius + 1,
                argb(accent, alpha * intensity * 0.13f)
        );

        drawRoundBorder(
                context,
                x,
                y,
                width,
                height,
                radius,
                argb(mix(accent, ACCENT_2, 0.25f), alpha * intensity * 0.10f)
        );
    }

    /** Compatibility overload that keeps all frame styles consistent. */
    public static void drawOrbitFrame(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float alpha,
            OrbitStyle style
    ) {

        float strengthScale = style == null
                ? 1f
                : clamp01(
                        style.tightAlpha() /
                                OrbitStyle.FULL.tightAlpha()
                );

        drawAmbientFrame(
                context,
                x,
                y,
                width,
                height,
                alpha,
                strengthScale
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

    // ---------------------------------------------------------------
    // shared design-system primitives
    // ---------------------------------------------------------------

    /**
     * A card: a slightly lighter surface than the panel it sits on, with
     * a hairline border. Used for stat cells, list rows and grouped
     * content so the interface reads as layered surfaces.
     */
    public static void drawCard(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            float alpha
    ) {

        drawShadow(
                context, x, y, w, h,
                Design.RADIUS_MD, 2, 0.10f, alpha
        );

        drawRoundRect(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(mix(PANEL_BG, TEXT, 0.045f), alpha)
        );

        drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(mix(PANEL_BORDER, TEXT, 0.10f), alpha * 0.75f)
        );
    }

    /** Card with an accent left edge, for selected / highlighted rows. */
    public static void drawCardAccent(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int accent,
            float alpha
    ) {

        drawCard(context, x, y, w, h, alpha);

        context.fill(
                x + 1,
                y + 3,
                x + 3,
                y + h - 3,
                argb(accent, alpha)
        );
    }

    /**
     * Section header: a small uppercase caption with a hairline that
     * runs to the right edge. Groups related controls without a heavy
     * divider.
     */
    public static void drawSectionHeader(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int width,
            String caption,
            float alpha
    ) {

        int textW = tr.getWidth(caption);

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(caption),
                x,
                y,
                argb(TEXT_MUTED, alpha)
        );

        int lineX = x + textW + Design.SPACE_3;

        if (lineX < x + width) {

            context.fill(
                    lineX,
                    y + 4,
                    x + width,
                    y + 5,
                    argb(mix(PANEL_BORDER, TEXT, 0.06f), alpha * 0.8f)
            );
        }
    }

    /** A 1px divider that fades out towards both ends. */
    public static void drawDivider(
            DrawContext context,
            int x,
            int y,
            int width,
            float alpha
    ) {

        drawGradientH(
                context,
                x, y, width, 1,
                mix(PANEL_BORDER, TEXT, 0.02f),
                mix(PANEL_BORDER, TEXT, 0.10f),
                alpha * 0.7f
        );
    }

    /**
     * A pill / badge: a compact rounded label used for statuses and
     * counts. The text is centred inside the pill.
     */
    public static void drawPill(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            String label,
            int accent,
            float alpha
    ) {

        int w = tr.getWidth(label) + Design.SPACE_3 * 2;
        int h = 11;

        drawRoundRect(
                context, x, y, w, h, Design.RADIUS_SM,
                argb(mix(PANEL_BG, accent, 0.22f), alpha)
        );

        drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_SM,
                argb(accent, alpha * 0.7f)
        );

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(label),
                x + Design.SPACE_3,
                y + 1,
                argb(accent, alpha)
        );
    }

    /** Width a pill would occupy, for layout maths. */
    public static int pillWidth(
            net.minecraft.client.font.TextRenderer tr,
            String label
    ) {

        return tr.getWidth(label) + Design.SPACE_3 * 2;
    }

    /**
     * Slim scrollbar thumb. {@code progress} is 0..1, {@code visible} is
     * the fraction of the content currently on screen.
     */
    public static void drawScrollbar(
            DrawContext context,
            int x,
            int trackTop,
            int trackH,
            float progress,
            float visible,
            float alpha
    ) {

        if (trackH <= 0) {
            return;
        }

        int thumbH = Math.max(
                14,
                (int) (trackH * clamp01(visible))
        );

        int travel = Math.max(1, trackH - thumbH);

        int y = trackTop + (int) (travel * clamp01(progress));

        drawRoundRect(
                context,
                x,
                y,
                3,
                thumbH,
                Design.RADIUS_SM,
                argb(mix(ACCENT_2, TEXT, 0.12f), alpha * 0.7f)
        );
    }

    /**
     * Soft fade at the top and bottom of a scroll viewport, so content
     * dissolves into the panel instead of being cut off.
     */
    public static void drawScrollFade(
            DrawContext context,
            int x,
            int top,
            int width,
            int bottom,
            float topAmount,
            float bottomAmount,
            float alpha
    ) {

        int fadeH = 12;

        if (topAmount > 0.01f) {

            context.fillGradient(
                    x, top, x + width, top + fadeH,
                    argb(PANEL_BG, alpha * clamp01(topAmount)),
                    argb(PANEL_BG, 0f)
            );
        }

        if (bottomAmount > 0.01f) {

            context.fillGradient(
                    x, bottom - fadeH, x + width, bottom,
                    argb(PANEL_BG, 0f),
                    argb(PANEL_BG, alpha * clamp01(bottomAmount))
            );
        }
    }

    // ---------------------------------------------------------------
    // launcher-style components
    // ---------------------------------------------------------------

    /**
     * Soft drop shadow under a rounded rectangle: a few expanding
     * hairline layers that fade out. Cheap enough for buttons and cards
     * and it is what makes the surfaces read as raised.
     */
    public static void drawShadow(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int layers,
            float strength,
            float alpha
    ) {

        for (int i = layers; i >= 1; i--) {

            drawRoundBorder(
                    context,
                    x - i, y - i + 1,
                    w + i * 2, h + i * 2,
                    radius + i,
                    argb(0xFF000000, alpha * strength / i)
            );
        }
    }

    /**
     * A select / dropdown field: dark surface, hairline border, label on
     * the left and a caret on the right. Matches the reference design.
     */
    public static void drawSelect(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int w,
            int h,
            String label,
            boolean open,
            boolean hovered,
            float alpha
    ) {

        drawSelect(
                context, tr, x, y, w, h, label, open,
                hovered ? 1f : 0f, alpha
        );
    }

    /** Select field with an interpolated hover treatment. */
    public static void drawSelect(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int w,
            int h,
            String label,
            boolean open,
            float hover,
            float alpha
    ) {

        float hoverAmount = clamp01(hover);

        if (hoverAmount > 0.01f) {
            drawShadow(
                    context,
                    x, y, w, h,
                    Design.RADIUS_MD,
                    2,
                    0.10f * hoverAmount,
                    alpha
            );
        }

        drawRoundRect(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(
                        open
                                ? mix(SURFACE, ACCENT, 0.14f)
                                : mix(SURFACE, SURFACE_2, hoverAmount),
                        alpha
                )
        );

        drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(
                        mix(BORDER, ACCENT, Math.max(
                                open ? 1f : 0f,
                                hoverAmount
                        )),
                        alpha
                )
        );

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(
                        tr.trimToWidth(label, w - 30)
                ),
                x + 10,
                y + (h - 9) / 2,
                argb(TEXT, alpha)
        );

        // caret
        int cx = x + w - 14;
        int cy = y + h / 2 - 1;

        for (int i = 0; i < 3; i++) {

            context.fill(
                    cx - i,
                    cy + i,
                    cx + i + 1,
                    cy + i + 1,
                    argb(open ? ACCENT : TEXT_MUTED, alpha)
            );
        }
    }

    /**
     * A colour swatch: a rounded square filled with the colour, with an
     * accent ring when selected.
     */
    public static void drawSwatch(
            DrawContext context,
            int x,
            int y,
            int size,
            int color,
            boolean selected,
            boolean hovered,
            float alpha
    ) {

        drawSwatch(
                context, x, y, size, color, selected,
                hovered ? 1f : 0f, alpha
        );
    }

    /** Color swatch with a smoothly interpolated hover ring. */
    public static void drawSwatch(
            DrawContext context,
            int x,
            int y,
            int size,
            int color,
            boolean selected,
            float hover,
            float alpha
    ) {

        float hoverAmount = clamp01(hover);

        drawRoundRect(
                context, x, y, size, size, Design.RADIUS_MD,
                argb(color, alpha)
        );

        if (selected) {

            drawRoundBorder(
                    context, x - 2, y - 2, size + 4, size + 4,
                    Design.RADIUS_LG,
                    argb(TEXT, alpha)
            );

        } else if (hoverAmount > 0.01f) {

            drawRoundBorder(
                    context, x, y, size, size,
                    Design.RADIUS_MD,
                    argb(mix(color, TEXT, 0.5f * hoverAmount), alpha)
            );

            drawHoverGlow(
                    context, x, y, size, size,
                    Design.RADIUS_MD,
                    color, hoverAmount, alpha
            );
        }
    }

    /**
     * A pill toggle: a rounded track with a sliding knob, used in the
     * settings rows of the reference design.
     */
    public static void drawPillToggle(
            DrawContext context,
            int x,
            int y,
            float on,
            float alpha
    ) {

        int w = 26;
        int h = 12;

        int track = mix(
                mix(SURFACE_2, TEXT, 0.10f),
                ACCENT,
                on
        );

        drawRoundRect(
                context, x, y, w, h, Design.RADIUS_LG,
                argb(track, alpha)
        );

        drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_LG,
                argb(mix(BORDER, ACCENT, on), alpha)
        );

        int knob = 8;
        int travel = w - knob - 4;

        int kx = x + 2 + (int) (travel * clamp01(on));
        int ky = y + (h - knob) / 2;

        drawRoundRect(
                context, kx, ky, knob, knob,
                Design.RADIUS_MD,
                argb(0xFFF5F5FA, alpha)
        );
    }

    /**
     * A settings row: a full-width card with a label on the left and a
     * pill toggle on the right.
     */
    public static void drawToggleRow(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int w,
            int h,
            String label,
            float on,
            boolean hovered,
            float alpha
    ) {

        drawToggleRow(
                context, tr, x, y, w, h, label, on,
                hovered ? 1f : 0f, alpha
        );
    }

    /** Settings row with an interpolated hover treatment. */
    public static void drawToggleRow(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            int w,
            int h,
            String label,
            float on,
            float hover,
            float alpha
    ) {

        float hoverAmount = clamp01(hover);

        if (hoverAmount > 0.01f) {
            drawShadow(
                    context,
                    x, y, w, h,
                    Design.RADIUS_MD,
                    2,
                    0.12f * hoverAmount,
                    alpha
            );
            drawHoverGlow(
                    context,
                    x, y, w, h,
                    Design.RADIUS_MD,
                    ACCENT,
                    hoverAmount,
                    alpha
            );
        }

        drawRoundRect(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(mix(SURFACE, SURFACE_2, hoverAmount), alpha)
        );

        drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_MD,
                argb(
                        mix(BORDER, mix(BORDER, TEXT, 0.18f), hoverAmount),
                        alpha
                )
        );

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(
                        tr.trimToWidth(label, w - 48)
                ),
                x + 12,
                y + (h - 9) / 2,
                argb(TEXT, alpha)
        );

        drawPillToggle(
                context,
                x + w - 38,
                y + (h - 12) / 2,
                on,
                alpha
        );
    }

    /**
     * A section header of the reference design: a small glyph, an
     * uppercase title and a muted description line underneath.
     */
    public static void drawSectionTitle(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            int x,
            int y,
            String glyph,
            String title,
            String description,
            float alpha
    ) {

        int tx = x;

        if (glyph != null && !glyph.isBlank()) {

            context.drawTextWithShadow(
                    tr,
                    net.minecraft.text.Text.literal(glyph),
                    x,
                    y,
                    argb(TEXT, alpha)
            );

            tx = x + tr.getWidth(glyph) + 8;
        }

        context.drawTextWithShadow(
                tr,
                net.minecraft.text.Text.literal(title.toUpperCase()),
                tx,
                y,
                argb(TEXT, alpha)
        );

        if (description != null && !description.isBlank()) {

            context.drawTextWithShadow(
                    tr,
                    net.minecraft.text.Text.literal(description),
                    x,
                    y + 14,
                    argb(TEXT_DIM, alpha)
            );
        }
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
                PositionedSoundInstance.ui(
                        SoundEvents.BLOCK_NOTE_BLOCK_HAT
                                .value(),
                        2.0f,
                        0.1f
                )
        );
    }
}
