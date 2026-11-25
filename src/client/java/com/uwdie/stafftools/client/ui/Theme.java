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
            // Amethyst — deep violet with cool indigo gradient
            new Palette(
                    "theme.amethyst",
                    0xFFAC67ED, 0xFF6366F1, 0xFFC9A0F5, 0xFF7C3AED, 0xFF2A2438,
                    0xFFF5F3FF, 0xFFC4B8E8, 0xFF8E7FB0,
                    0xFF14121C, 0xFFAC67ED
            ),
            // Ocean — blue to teal
            new Palette(
                    "theme.ocean",
                    0xFF3B82F6, 0xFF06B6D4, 0xFF93C5FD, 0xFF2563EB, 0xFF1E2A3A,
                    0xFFF0F9FF, 0xFFB8D4F0, 0xFF7A9AB8,
                    0xFF0E1620, 0xFF60A5FA
            ),
            // Emerald — green to lime
            new Palette(
                    "theme.emerald",
                    0xFF10B981, 0xFF84CC16, 0xFF6EE7B7, 0xFF059669, 0xFF1A2E26,
                    0xFFF0FDF4, 0xFFB8E8C8, 0xFF7AA888,
                    0xFF0C1814, 0xFF34D399
            ),
            // Crimson — red to orange
            new Palette(
                    "theme.crimson",
                    0xFFEF4444, 0xFFF97316, 0xFFFCA5A5, 0xFFDC2626, 0xFF341E1E,
                    0xFFFFF5F5, 0xFFF0B8B8, 0xFFB87A7A,
                    0xFF1A0E0E, 0xFFF87171
            ),
            // Dawn — amber to pink
            new Palette(
                    "theme.dawn",
                    0xFFF59E0B, 0xFFEC4899, 0xFFFCD34D, 0xFFD97706, 0xFF342A1A,
                    0xFFFFFBF0, 0xFFF0D8A8, 0xFFB89A6A,
                    0xFF1A140A, 0xFFFBBF24
            ),
            // Aurora — cyan to violet
            new Palette(
                    "theme.aurora",
                    0xFF06B6D4, 0xFF8B5CF6, 0xFF67E8F9, 0xFF0891B2, 0xFF1A2438,
                    0xFFF0FDFF, 0xFFB8E8F0, 0xFF7AA8B8,
                    0xFF0C1220, 0xFF22D3EE
            ),
            // Neon — hot pink to purple
            new Palette(
                    "theme.neon",
                    0xFFEC4899, 0xFFA855F7, 0xFFF9A8D4, 0xFFDB2777, 0xFF2E1A2E,
                    0xFFFFF0FA, 0xFFF0B8E0, 0xFFB87AA8,
                    0xFF180C18, 0xFFF472B6
            ),
            // Sunset — orange to rose
            new Palette(
                    "theme.sunset",
                    0xFFFF795E, 0xFFE44871, 0xFFFFB49E, 0xFFE44871, 0xFF341E1A,
                    0xFFFFF5F0, 0xFFF0C8B8, 0xFFB88A7A,
                    0xFF1A0E0A, 0xFFFFA06E
            ),
            // Twilight — indigo to magenta
            new Palette(
                    "theme.twilight",
                    0xFF7268F5, 0xFFEC6AA7, 0xFFC1B9FF, 0xFF5146D8, 0xFF241E38,
                    0xFFF5F3FF, 0xFFC8B8F0, 0xFF8E7AB8,
                    0xFF120E1C, 0xFFB48CF5
            ),
            // Sakura — pink to lavender
            new Palette(
                    "theme.sakura",
                    0xFFEC5C9B, 0xFFA78BFA, 0xFFFFC5D8, 0xFFC83876, 0xFF341E2A,
                    0xFFFFF5FA, 0xFFF0C8E0, 0xFFB88AA8,
                    0xFF1A0E14, 0xFFF487B4
            ),
            // Glacier — ice blue to mint
            new Palette(
                    "theme.glacier",
                    0xFF38A8F5, 0xFF45D6CF, 0xFF9DEBFA, 0xFF2475C7, 0xFF1A2A34,
                    0xFFF0FAFF, 0xFFB8D8F0, 0xFF7AA0B8,
                    0xFF0C1620, 0xFF62D6F0
            ),
            // Mint — teal to green
            new Palette(
                    "theme.mint",
                    0xFF20B99B, 0xFF4ADE80, 0xFFA8F0D8, 0xFF138B76, 0xFF1A2E28,
                    0xFFF0FDF8, 0xFFB8E8D0, 0xFF7AA890,
                    0xFF0C1814, 0xFF4ED1A7
            ),
            // Plum — purple to pink
            new Palette(
                    "theme.plum",
                    0xFF9B5DE5, 0xFFF15BB5, 0xFFD8B4FE, 0xFF743CC0, 0xFF2A1E38,
                    0xFFFAF5FF, 0xFFD8B8F0, 0xFF9A7AB8,
                    0xFF140E1C, 0xFFC77DFF
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

        // derive a slightly lighter surface from the panel background
        // so each theme has its own surface tone instead of a shared grey
        Ui.SURFACE_2 = Ui.mix(p.panelBg(), p.accent(), 0.06f);
        Ui.PANEL_BG_SOFT = Ui.mix(p.panelBg(), 0xFFFFFFFF, 0.04f);

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
