package com.uwdie.stafftools.client.ui;

/**
 * Color theme system. Each preset is a full palette; applying one
 * copies its colors into the public static fields of {@link Ui},
 * which every widget reads at render time — no other changes needed.
 *
 * A preset may define {@code accent2}: then ACCENT_2 differs from the
 * primary accent and all accent bars are rendered as a horizontal
 * gradient (ACCENT -> ACCENT_2).
 */
public final class Theme {

    public record Palette(
            String nameKey,
            int accent,
            int accent2,
            int accentSoft,
            int accentDark,
            int border,
            int text,
            int textDim,
            int textMuted,
            int panelBg
    ) {
    }

    private static final Palette[] PRESETS = {
            new Palette(
                    "theme.ocean",
                    0xFF3F8AE0, 0, 0xFF7DBCFF, 0xFF2A5D9E, 0xFF2E5E9E,
                    0xFFFFFFFF, 0xFFAAAAAA, 0xFF777777,
                    0xF0141418
            ),
            new Palette(
                    "theme.emerald",
                    0xFF2FCC8F, 0, 0xFF6FE7B8, 0xFF1D8A61, 0xFF1F7A57,
                    0xFFFFFFFF, 0xFFA9C5B8, 0xFF6F8579,
                    0xF0101714
            ),
            new Palette(
                    "theme.amethyst",
                    0xFF9B6FE3, 0xFFE36FC4, 0xFFC3A3FF, 0xFF6B44B8, 0xFF6C4FB3,
                    0xFFFFFFFF, 0xFFBCB0CE, 0xFF7C7290,
                    0xF0161219
            ),
            new Palette(
                    "theme.crimson",
                    0xFFCB4848, 0, 0xFFE89B9B, 0xFF8F3232, 0xFF8F3A3A,
                    0xFFFFFFFF, 0xFFC9AEAE, 0xFF856969,
                    0xF0191010
            ),
            new Palette(
                    "theme.dawn",
                    0xFFD9A94E, 0xFFF2D49B, 0xFFF0D49A, 0xFF9C742F, 0xFF9C7434,
                    0xFFFFFFFF, 0xFFD8CBA8, 0xFF8A7E64,
                    0xF01A1710
            ),
            new Palette(
                    "theme.aurora",
                    0xFF35D0BA, 0xFF4FA8FF, 0xFF8FE8DB, 0xFF1F8F80, 0xFF1F7A93,
                    0xFFFFFFFF, 0xFFAAC9CF, 0xFF68858B,
                    0xF00E1717
            ),
            new Palette(
                    "theme.neon",
                    0xFFFF3D8F, 0xFF38C6FF, 0xFFFFA1CC, 0xFFB21D63, 0xFF2E7FB8,
                    0xFFFFFFFF, 0xFFC0B7CE, 0xFF7E7595,
                    0xF0121018
            )
    };

    private static int index = 0;

    private Theme() {
    }

    public static void apply(int themeIndex) {

        index = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        Palette p = PRESETS[index];

        Ui.ACCENT = p.accent();
        Ui.ACCENT_2 = p.accent2() != 0
                ? p.accent2()
                : p.accent();
        Ui.ACCENT_SOFT = p.accentSoft();
        Ui.ACCENT_DARK = p.accentDark();
        Ui.PANEL_BORDER = p.border();
        Ui.TEXT = p.text();
        Ui.TEXT_DIM = p.textDim();
        Ui.TEXT_MUTED = p.textMuted();

        // semantic colors stay constant across themes
        Ui.DANGER = 0xFFFF5555;
        Ui.DANGER_SOFT = 0xFFFF8A8A;
        Ui.SUCCESS = 0xFF55DD77;
        Ui.WARNING = 0xFFEEBB55;

        Ui.PANEL_BG = p.panelBg();
        Ui.PANEL_BG_SOFT = 0x22000000;
    }

    public static int getIndex() {
        return index;
    }

    public static int count() {
        return PRESETS.length;
    }

    /** Lang key of the theme display name. */
    public static String nameKey(int themeIndex) {

        int i = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        return PRESETS[i].nameKey();
    }

    /** True when the preset defines a gradient (accent != accent2). */
    public static boolean isGradient(int themeIndex) {

        int i = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        return PRESETS[i].accent2() != 0
                && PRESETS[i].accent2() != PRESETS[i].accent();
    }

    /** Primary accent of a preset (independent of the live palette). */
    public static int accentOf(int themeIndex) {

        int i = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        return PRESETS[i].accent();
    }

    /** Secondary accent of a preset. */
    public static int accent2Of(int themeIndex) {

        int i = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        Palette p = PRESETS[i];

        return p.accent2() != 0
                ? p.accent2()
                : p.accentSoft();
    }
}
