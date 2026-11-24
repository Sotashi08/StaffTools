package com.uwdie.stafftools.client.ui;

/**
 * Design tokens: the single source of truth for spacing, corner radii,
 * component heights, typography sizes, animation timings and easing.
 *
 * <p>Every screen and widget reads these instead of scattering magic
 * numbers, so the whole mod shares one visual rhythm. Colours live in
 * {@link Ui} (they change with the theme); geometry and motion live here
 * (they do not).
 */
public final class Design {

    private Design() {
    }

    // ---------------------------------------------------------------
    // spacing scale (4px base)
    // ---------------------------------------------------------------

    public static final int SPACE_1 = 2;
    public static final int SPACE_2 = 4;
    public static final int SPACE_3 = 6;
    public static final int SPACE_4 = 8;
    public static final int SPACE_5 = 10;
    public static final int SPACE_6 = 12;
    public static final int SPACE_7 = 16;
    public static final int SPACE_8 = 20;
    public static final int SPACE_9 = 24;

    // ---------------------------------------------------------------
    // corner radii
    // ---------------------------------------------------------------

    public static final int RADIUS_SM = 2;
    public static final int RADIUS_MD = 3;
    public static final int RADIUS_LG = 4;
    public static final int RADIUS_XL = 6;

    // ---------------------------------------------------------------
    // component heights
    // ---------------------------------------------------------------

    public static final int H_BUTTON = 20;
    public static final int H_BUTTON_LG = 22;
    public static final int H_FIELD = 20;
    public static final int H_ROW = 16;
    public static final int H_ROW_LG = 22;
    public static final int H_TAB = 18;
    public static final int H_HEADER = 28;
    public static final int H_CARD_HEADER = 16;

    // ---------------------------------------------------------------
    // typography (Minecraft font is 9px tall; these are line steps)
    // ---------------------------------------------------------------

    public static final int FONT_SM = 9;
    public static final int FONT_MD = 10;
    public static final int FONT_LG = 12;

    // ---------------------------------------------------------------
    // animation durations (milliseconds)
    // ---------------------------------------------------------------

    public static final long DUR_FAST = 120;
    public static final long DUR_NORMAL = 180;
    public static final long DUR_SLOW = 260;
    public static final long DUR_ENTRANCE = 260;

    // ---------------------------------------------------------------
    // easing
    // ---------------------------------------------------------------

    /** Fast start, gentle stop. The default for entrances. */
    public static float easeOutCubic(float t) {

        float u = 1f - Ui.clamp01(t);

        return 1f - u * u * u;
    }

    /** Even faster start, very gentle stop. For large movements. */
    public static float easeOutQuint(float t) {

        float u = 1f - Ui.clamp01(t);

        return 1f - u * u * u * u * u;
    }

    /** Symmetric ease for state changes that must not overshoot. */
    public static float easeInOutCubic(float t) {

        float x = Ui.clamp01(t);

        return x < 0.5f
                ? 4f * x * x * x
                : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    /** Slight overshoot, for pop-in of small elements. */
    public static float easeOutBack(float t) {

        float x = Ui.clamp01(t);

        float c1 = 1.70158f;
        float c3 = c1 + 1f;

        return 1f + c3 * (float) Math.pow(x - 1f, 3)
                + c1 * (float) Math.pow(x - 1f, 2);
    }

    /** Frame-rate independent lerp factor for a given duration. */
    public static float lerpFactor(float deltaSeconds, long durationMs) {

        if (durationMs <= 0) {
            return 1f;
        }

        return Math.min(
                1f,
                deltaSeconds * 1000f / durationMs
        );
    }
}
