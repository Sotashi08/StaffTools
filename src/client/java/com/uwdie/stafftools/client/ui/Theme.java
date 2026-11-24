package com.uwdie.stafftools.client.ui;

/**
 * Color theme system. Each preset is a full palette; applying one
 * copies its colors into the public static fields of {@link Ui},
 * which every widget reads at render time — no other changes needed.
 *
 * <p>Every preset keeps the same dark graphite surfaces and only swaps
 * the accent, so the interface always reads as one product.
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
            int panelBg,
            int nick
    ) {
    }

    private static final Palette[] PRESETS = {
            new Palette(
                    "theme.amethyst",
                    0xFFAC67ED, 0xFF8B5CF6, 0xFFC9A0F5, 0xFF7C3AED, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFAC67ED
            ),
            new Palette(
                    "theme.ocean",
                    0xFF3B82F6, 0xFF60A5FA, 0xFF93C5FD, 0xFF2563EB, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFF60A5FA
            ),
            new Palette(
                    "theme.emerald",
                    0xFF10B981, 0xFF34D399, 0xFF6EE7B7, 0xFF059669, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFF34D399
            ),
            new Palette(
                    "theme.crimson",
                    0xFFEF4444, 0xFFF87171, 0xFFFCA5A5, 0xFFDC2626, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFF87171
            ),
            new Palette(
                    "theme.dawn",
                    0xFFF59E0B, 0xFFFBBF24, 0xFFFCD34D, 0xFFD97706, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFFBBF24
            ),
            new Palette(
                    "theme.aurora",
                    0xFF06B6D4, 0xFF22D3EE, 0xFF67E8F9, 0xFF0891B2, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFF22D3EE
            ),
            new Palette(
                    "theme.neon",
                    0xFFEC4899, 0xFFF472B6, 0xFFF9A8D4, 0xFFDB2777, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFF472B6
            ),
            new Palette(
                    "theme.sunset",
                    0xFFFF795E, 0xFFFFC46B, 0xFFFFB49E, 0xFFE44871, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFFFA06E
            ),
            new Palette(
                    "theme.twilight",
                    0xFF7268F5, 0xFFEC6AA7, 0xFFC1B9FF, 0xFF5146D8, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFB48CF5
            ),
            new Palette(
                    "theme.sakura",
                    0xFFEC5C9B, 0xFFFFB4CF, 0xFFFFC5D8, 0xFFC83876, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFF487B4
            ),
            new Palette(
                    "theme.glacier",
                    0xFF38A8F5, 0xFF45D6CF, 0xFF9DEBFA, 0xFF2475C7, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFF62D6F0
            ),
            new Palette(
                    "theme.mint",
                    0xFF20B99B, 0xFF78D98B, 0xFFA8F0D8, 0xFF138B76, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFF4ED1A7
            ),
            new Palette(
                    "theme.plum",
                    0xFF9B5DE5, 0xFFF15BB5, 0xFFD8B4FE, 0xFF743CC0, 0xFF26262F,
                    0xFFFFFFFF, 0xFFB4B4C0, 0xFF8A8A96,
                    0xFF121218, 0xFFC77DFF
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
        Ui.BORDER = p.border();
        Ui.TEXT = p.text();
        Ui.TEXT_DIM = p.textDim();
        Ui.TEXT_MUTED = p.textMuted();

        // semantic colors stay constant across themes
        Ui.DANGER = 0xFFEF4444;
        Ui.DANGER_SOFT = 0xFFF87171;
        Ui.SUCCESS = 0xFF4ADE80;
        Ui.WARNING = 0xFFF59E0B;

        Ui.PANEL_BG = p.panelBg();
        Ui.SURFACE = p.panelBg();
        Ui.SURFACE_2 = 0xFF17171F;
        Ui.PANEL_BG_SOFT = 0xFF17171F;

        // a manual pick wins over the theme default
        int nick =
                com.uwdie.stafftools.client.StafftoolsClient
                        .getConfig()
                        .getNickHighlight();

        Ui.NICK_HIGHLIGHT = nick != 0
                ? nick
                : p.nick();
    }

    /** Nick highlight a preset ships with. */
    public static int nickOf(int themeIndex) {

        int i = Math.clamp(themeIndex, 0, PRESETS.length - 1);

        return PRESETS[i].nick();
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
