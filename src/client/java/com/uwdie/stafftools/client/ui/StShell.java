package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Launcher-style application shell: a compact window with a narrow icon
 * rail, a top bar and a tab row.
 *
 * <p>The window is centred and capped in size, so the interface reads as
 * a desktop application panel instead of a full-screen menu. Every
 * screen of the mod is drawn inside it, which keeps the whole product
 * visually consistent.
 *
 * <p>Rail items and tabs carry a tooltip: hovering them shows what the
 * entry opens, so the icons never have to be guessed.
 */
public final class StShell {

    /** Width of the left icon rail. */
    public static final int RAIL_W = 38;

    /** Height of the top bar. */
    public static final int TOP_H = 38;

    /** Height of the tab row. */
    public static final int TABS_H = 32;

    /** Inner padding of the content area. */
    public static final int CONTENT_PAD = 14;

    private static final int WINDOW_CONTROL_COUNT = 3;
    private static final int TOP_ICON_COUNT = 2;

    /** Preferred window size; the window shrinks on small screens. */
    private static final int PREF_W = 640;
    private static final int PREF_H = 396;

    /** Minimum window size before the layout starts to compress. */
    private static final int MIN_W = 420;
    private static final int MIN_H = 300;

    /** One entry of the left rail. */
    public record RailItem(
            String glyph,
            String labelKey,
            String hintKey,
            Runnable action
    ) {
    }

    /** One tab of the tab row. */
    public record Tab(String labelKey, String hintKey, Runnable action) {
    }

    private final List<RailItem> rail = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();

    private int activeRail = -1;
    private int activeTab = -1;

    /** Screen-space bounds of the rail items, for hit testing. */
    private final List<int[]> railBoxes = new ArrayList<>();

    /** Screen-space bounds of the tabs, for hit testing. */
    private final List<int[]> tabBoxes = new ArrayList<>();

    /** Bounds of the top-bar buttons, for hit testing. */
    private final List<int[]> topBoxes = new ArrayList<>();

    /** Actions of the top-bar buttons, parallel to {@link #topBoxes}. */
    private final List<Runnable> topActions = new ArrayList<>();
    private float[] railHoverAmounts = new float[0];
    private float[] tabHoverAmounts = new float[0];
    private final float[] windowControlHoverAmounts =
            new float[WINDOW_CONTROL_COUNT];
    private final float[] topIconHoverAmounts =
            new float[TOP_ICON_COUNT];
    private float titleHoverAmount;
    private float auditBellHoverAmount;
    private float profileHoverAmount;
    private float statusHoverAmount;

    private int screenW;
    private int screenH;

    /** Window rectangle in screen space. */
    private int winX;
    private int winY;
    private int winW;
    private int winH;

    /** Tooltip to draw this frame: {x, y, text}. */
    private String tipText;
    private int tipX;
    private int tipY;

    public StShell rail(RailItem... items) {

        rail.clear();

        for (RailItem item : items) {
            rail.add(item);
        }
        railHoverAmounts = new float[rail.size()];

        return this;
    }

    public StShell tabs(Tab... items) {

        tabs.clear();

        for (Tab item : items) {
            tabs.add(item);
        }
        tabHoverAmounts = new float[tabs.size()];

        return this;
    }

    public StShell activeRail(int index) {
        this.activeRail = index;
        return this;
    }

    public StShell activeTab(int index) {
        this.activeTab = index;
        return this;
    }

    // ------------------------------------------------------------------
    // geometry
    // ------------------------------------------------------------------

    /**
     * Resolves the window rectangle for the current screen size.
     *
     * <p>This MUST run before any screen builds its widgets: screens
     * position their widgets in {@code init()}, and the content area is
     * derived from the window rectangle. Computing it only during render
     * left every widget at the origin on the first frame.
     */
    public void layout() {

        MinecraftClient client = MinecraftClient.getInstance();

        screenW = client.getWindow().getScaledWidth();
        screenH = client.getWindow().getScaledHeight();

        winW = Math.min(PREF_W, screenW - 24);
        winH = Math.min(PREF_H, screenH - 24);

        winW = Math.max(Math.min(MIN_W, screenW - 8), winW);
        winH = Math.max(Math.min(MIN_H, screenH - 8), winH);

        winX = (screenW - winW) / 2;
        winY = (screenH - winH) / 2;
    }

    /** Left edge of the content area. */
    public int contentLeft() {
        return winX + RAIL_W + CONTENT_PAD;
    }

    /** Top edge of the content area (below the tab row). */
    public int contentTop() {
        return winY + TOP_H + (tabs.isEmpty() ? 0 : TABS_H) + 6;
    }

    /** Width available to the content. */
    public int contentWidth() {
        return winW - RAIL_W - CONTENT_PAD * 2;
    }

    /** Bottom edge of the content area. */
    public int contentBottom() {
        return winY + winH - CONTENT_PAD;
    }

    /** Window bounds, for screens that want to draw their own backdrop. */
    public int windowX() {
        return winX;
    }

    public int windowY() {
        return winY;
    }

    public int windowWidth() {
        return winW;
    }

    public int windowHeight() {
        return winH;
    }

    private static float animateHover(
            float current,
            boolean hovered,
            float delta
    ) {
        return Ui.animateTowards(
                current,
                hovered ? 1f : 0f,
                delta,
                14f
        );
    }

    /**
     * Draws the whole chrome and records hit boxes.
     *
     * @param title    product name shown in the top bar
     * @param subtitle small accent line under the title (version, server)
     * @param status   right-hand status pill text, may be null
     */
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float alpha,
            String title,
            String subtitle,
            String status,
            int titleAnimation,
            float delta
    ) {

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // the rectangle is normally resolved in init(); recompute it so
        // a window resize is picked up on the next frame
        layout();

        railBoxes.clear();
        tabBoxes.clear();
        topBoxes.clear();
        topActions.clear();

        tipText = null;

        // dimmed backdrop behind the window
        context.fill(
                0, 0, screenW, screenH,
                Ui.argb(0xFF05050A, alpha * 0.72f)
        );

        // window shadow: soft layers expanding outwards
        for (int i = 6; i >= 1; i--) {

            Ui.drawRoundBorder(
                    context,
                    winX - i, winY - i,
                    winW + i * 2, winH + i * 2,
                    10 + i,
                    Ui.argb(0xFF000000, alpha * (0.30f / i))
            );
        }

        // window body
        Ui.drawRoundRect(
                context, winX, winY, winW, winH,
                10,
                Ui.argb(Ui.BG, alpha)
        );

        Ui.drawRoundBorder(
                context, winX, winY, winW, winH,
                10,
                Ui.argb(Ui.BORDER, alpha)
        );

        drawRail(context, tr, mouseX, mouseY, alpha, delta);
        drawTopBar(
                context,
                tr,
                mouseX,
                mouseY,
                alpha,
                title,
                subtitle,
                status,
                titleAnimation,
                delta
        );
        drawTabs(context, tr, mouseX, mouseY, alpha, delta);

        // the rail and the top bar are square surfaces: mask the pixels
        // that fall outside the rounded window so every corner stays
        // round, exactly like the moderator HUD
        Ui.maskOutsideCorners(
                context,
                winX, winY, winW, winH,
                10,
                Ui.argb(0xFF05050A, alpha * 0.72f)
        );

        // and re-stroke the border on top of the mask
        Ui.drawRoundBorder(
                context, winX, winY, winW, winH,
                10,
                Ui.argb(Ui.BORDER, alpha)
        );
    }

    /** Draws the hover tooltip; call after the content so it floats. */
    public void renderTooltip(
            DrawContext context,
            float alpha
    ) {

        if (tipText == null) {
            return;
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        int w = tr.getWidth(tipText) + 14;
        int h = 16;

        int x = Math.clamp(tipX, 4, screenW - w - 4);
        int y = Math.clamp(tipY, 4, screenH - h - 4);

        Ui.drawRoundRect(
                context, x, y, w, h, Design.RADIUS_MD,
                Ui.argb(Ui.SURFACE_2, alpha)
        );

        Ui.drawRoundBorder(
                context, x, y, w, h, Design.RADIUS_MD,
                Ui.argb(Ui.BORDER, alpha)
        );

        context.drawTextWithShadow(
                tr,
                Text.literal(tipText),
                x + 7,
                y + 4,
                Ui.argb(Ui.TEXT, alpha)
        );
    }

    // ------------------------------------------------------------------
    // left rail
    // ------------------------------------------------------------------

    private void drawRail(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha,
            float delta
    ) {

        int railX = winX;
        int railY = winY + TOP_H;

        // rail surface, one step lighter than the window
        context.fill(
                railX, railY,
                railX + RAIL_W, winY + winH,
                Ui.argb(Ui.SURFACE, alpha)
        );

        // hairline separating the rail from the content
        context.fill(
                railX + RAIL_W - 1, railY,
                railX + RAIL_W, winY + winH,
                Ui.argb(Ui.BORDER, alpha)
        );

        int itemSize = 28;
        int gap = 5;
        int y = railY + 10;

        for (int i = 0; i < rail.size(); i++) {

            RailItem item = rail.get(i);

            int x = railX + (RAIL_W - itemSize) / 2;

            boolean active = i == activeRail;

            boolean hovered =
                    mouseX >= x && mouseX <= x + itemSize &&
                            mouseY >= y && mouseY <= y + itemSize;
            float hover = animateHover(
                    railHoverAmounts[i],
                    hovered,
                    delta
            );
            railHoverAmounts[i] = hover;

            if (active) {

                Ui.drawRoundRect(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG,
                        Ui.argb(Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.20f), alpha)
                );

                Ui.drawRoundBorder(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG,
                        Ui.argb(Ui.ACCENT, alpha)
                );

            } else if (hover > 0.01f) {

                Ui.drawShadow(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG, 2, 0.10f * hover, alpha
                );
                Ui.drawHoverGlow(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG, Ui.ACCENT, hover, alpha
                );
                Ui.drawRoundRect(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG,
                        Ui.argb(
                                Ui.mix(Ui.SURFACE, Ui.SURFACE_2, hover),
                                alpha
                        )
                );
                Ui.drawRoundBorder(
                        context, x, y, itemSize, itemSize,
                        Design.RADIUS_LG,
                        Ui.argb(
                                Ui.mix(Ui.BORDER, Ui.ACCENT, hover * 0.4f),
                                alpha
                        )
                );
            }

            context.drawCenteredTextWithShadow(
                    tr,
                    Text.literal(item.glyph()),
                    x + itemSize / 2,
                    y + (itemSize - 9) / 2,
                    Ui.argb(
                            active
                                    ? Ui.ACCENT
                                    : Ui.mix(Ui.TEXT_MUTED, Ui.TEXT, hover),
                            alpha
                    )
            );

            railBoxes.add(new int[]{x, y, itemSize, itemSize});

            if (hovered) {
                tipText = Lang.t(item.hintKey());
                tipX = x + itemSize + 8;
                tipY = y + 8;
            }

            y += itemSize + gap;
        }
    }

    // ------------------------------------------------------------------
    // top bar
    // ------------------------------------------------------------------

    private void drawProductTitle(
            DrawContext context,
            TextRenderer tr,
            String title,
            int x,
            int y,
            float hover,
            int animation,
            float alpha
    ) {

        if (!Ui.animationsEnabled()) {
            animation = 0;
        }

        if (animation <= 0) {
            context.drawTextWithShadow(
                    tr,
                    Text.literal(title),
                    x,
                    y,
                    Ui.argb(
                            Ui.mix(Ui.TEXT, Ui.ACCENT, hover),
                            alpha
                    )
            );
            return;
        }

        long now = System.currentTimeMillis();
        int length = title.length();
        double phase = (now * (double) Ui.animationSpeed() % 2600.0)
                / 2600.0;
        float pulse = (float) (
                0.5 + 0.5 * Math.sin(phase * Math.PI * 2)
        );
        float glowStrength = Ui.glowIntensity();
        int textWidth = tr.getWidth(title);
        float scale = animation == 2
                ? 0.90f + pulse * 0.20f
                : 1f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                x + textWidth / 2f,
                y + 4.5f
        );
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(
                -(x + textWidth / 2f),
                -(y + 4.5f)
        );

        int drawX = x;

        for (int i = 0; i < length; i++) {

            String glyph = title.substring(i, i + 1);
            float wave = (float) (
                    0.5 + 0.5 * Math.sin(
                            i * 0.68 - phase * Math.PI * 2
                    )
            );

            float highlight;
            int color;

            if (animation == 1) {
                highlight = 0.24f + wave * 0.66f;
                color = Ui.mix(
                        Ui.ACCENT_2,
                        Ui.ACCENT_SOFT,
                        0.08f + wave * 0.88f
                );
            } else if (animation == 2) {
                highlight = 0.28f + pulse * 0.62f;
                color = Ui.mix(
                        Ui.TEXT,
                        Ui.ACCENT_SOFT,
                        0.08f + pulse * 0.88f
                );
            } else {
                float head = (float) (phase * (length + 4) - 2);
                float distance = Math.abs(i - head);
                highlight = 1f - Ui.smoothstep(0f, 4.2f, distance);
                color = Ui.mix(
                        Ui.TEXT,
                        Ui.ACCENT_SOFT,
                        0.04f + highlight * 0.96f
                );
            }

            color = Ui.mix(color, Ui.ACCENT, hover * 0.45f);
            int glyphY = y;

            if (animation == 1) {
                glyphY += Math.round(
                        (float) Math.sin(i * 0.68 - phase * Math.PI * 2)
                                * 1.2f
                );
            } else if (animation == 3 && highlight > 0.25f) {
                glyphY -= Math.round(highlight);
            }

            if (highlight > 0f && glowStrength > 0f) {
                int glowColor = Ui.mix(Ui.ACCENT, Ui.ACCENT_SOFT, highlight);
                context.drawTextWithShadow(
                        tr,
                        Text.literal(glyph),
                        drawX,
                        glyphY + 1,
                        Ui.argb(
                                glowColor,
                                alpha * highlight * glowStrength * 0.72f
                        )
                );
            }

            context.drawTextWithShadow(
                    tr,
                    Text.literal(glyph),
                    drawX,
                    glyphY,
                    Ui.argb(color, alpha)
            );

            drawX += tr.getWidth(glyph);
        }

        context.getMatrices().popMatrix();
    }

    private void drawTopBar(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha,
            String title,
            String subtitle,
            String status,
            int titleAnimation,
            float delta
    ) {

        int left = winX + RAIL_W;

        context.fill(
                left, winY, winX + winW, winY + TOP_H,
                Ui.argb(Ui.BG, alpha)
        );

        context.fill(
                left, winY + TOP_H - 1, winX + winW, winY + TOP_H,
                Ui.argb(Ui.BORDER, alpha)
        );

        // product name, uppercase; the whole block is a button that
        // returns to the main menu
        int titleX = left + 14;

        boolean titleHovered =
                mouseX >= titleX - 4 && mouseX <= titleX + 150 &&
                        mouseY >= winY + 6 && mouseY <= winY + TOP_H - 4;
        titleHoverAmount = animateHover(
                titleHoverAmount,
                titleHovered,
                delta
        );

        drawProductTitle(
                context,
                tr,
                title.toUpperCase(Locale.ROOT),
                titleX,
                winY + 10,
                titleHoverAmount,
                titleAnimation,
                alpha
        );

        if (subtitle != null && !subtitle.isBlank()) {

            context.drawTextWithShadow(
                    tr,
                    Text.literal(subtitle),
                    titleX,
                    winY + 22,
                    Ui.argb(
                            Ui.mix(
                                    Ui.ACCENT,
                                    Ui.ACCENT_SOFT,
                                    titleHoverAmount
                            ),
                            alpha
                    )
            );
        }

        topBoxes.add(new int[]{titleX - 4, winY + 6, 150, TOP_H - 10});
        topActions.add(() -> MinecraftClient.getInstance()
                .setScreen(new StafftoolsScreen()));

        if (titleHovered) {
            tipText = Lang.t(Lang.Key.APP_TITLE);
            tipX = titleX;
            tipY = winY + TOP_H + 2;
        }

        // right-hand cluster, laid out from the right edge
        int right = winX + winW - 12;

        // window controls: minimise / maximise / close
        String[] glyphs = {"\u2013", "\u25A1", "\u2715"};

        Runnable[] actions = {
                () -> MinecraftClient.getInstance().setScreen(null),
                () -> MinecraftClient.getInstance().setScreen(null),
                () -> MinecraftClient.getInstance().setScreen(null)
        };

        for (int i = glyphs.length - 1; i >= 0; i--) {

            int bx = right - 18;

            boolean hovered =
                    mouseX >= bx - 4 && mouseX <= bx + 12 &&
                            mouseY >= winY + 14 && mouseY <= winY + 30;
            float hover = animateHover(
                    windowControlHoverAmounts[i],
                    hovered,
                    delta
            );
            windowControlHoverAmounts[i] = hover;

            context.drawTextWithShadow(
                    tr,
                    Text.literal(glyphs[i]),
                    bx,
                    winY + 18,
                    Ui.argb(
                            Ui.mix(
                                    Ui.TEXT_MUTED,
                                    i == 2 ? Ui.DANGER : Ui.TEXT,
                                    hover
                            ),
                            alpha
                    )
            );

            topBoxes.add(new int[]{bx - 4, winY + 14, 16, 16});
            topActions.add(actions[i]);

            right -= 20;
        }

        right -= 8;

        // two square icon buttons: settings and themes
        Runnable[] iconActions = {
                () -> MinecraftClient.getInstance()
                        .setScreen(new SettingsScreen(null)),
                () -> MinecraftClient.getInstance()
                        .setScreen(new ThemesScreen())
        };

        String[] iconGlyphs = {"\u2699", "\uD83C\uDFA8"};
        String[] iconHints = {
                Lang.t(Lang.Key.BTN_SETTINGS),
                Lang.t(Lang.Key.BTN_THEMES)
        };

        for (int i = 0; i < 2; i++) {

            int bx = right - 28;

            boolean hovered =
                    mouseX >= bx && mouseX <= bx + 26 &&
                            mouseY >= winY + 13 && mouseY <= winY + 39;
            float hover = animateHover(
                    topIconHoverAmounts[i],
                    hovered,
                    delta
            );
            topIconHoverAmounts[i] = hover;

            if (hover > 0.01f) {
                Ui.drawShadow(
                        context, bx, winY + 13, 26, 26,
                        Design.RADIUS_MD, 3, 0.08f * hover, alpha
                );
                Ui.drawHoverGlow(
                        context, bx, winY + 13, 26, 26,
                        Design.RADIUS_MD, Ui.ACCENT, hover * 0.65f, alpha
                );
            }

            Ui.drawRoundRect(
                    context, bx, winY + 13, 26, 26,
                    Design.RADIUS_MD,
                    Ui.argb(
                            Ui.mix(Ui.SURFACE, Ui.SURFACE_2, hover),
                            alpha
                    )
            );

            Ui.drawRoundBorder(
                    context, bx, winY + 13, 26, 26,
                    Design.RADIUS_MD,
                    Ui.argb(
                            Ui.mix(Ui.BORDER, Ui.ACCENT, hover),
                            alpha
                    )
            );

            context.drawCenteredTextWithShadow(
                    tr,
                    Text.literal(iconGlyphs[i]),
                    bx + 13,
                    winY + 22,
                    Ui.argb(
                            Ui.mix(Ui.TEXT_DIM, Ui.ACCENT, hover),
                            alpha
                    )
            );

            topBoxes.add(new int[]{bx, winY + 13, 26, 26});
            topActions.add(iconActions[i]);

            if (hovered) {
                tipText = iconHints[i];
                tipX = bx;
                tipY = winY + 42;
            }

            right -= 30;
        }

        right -= 6;

        // notification bell: opens the audit journal
        int bellX = right - 22;

        boolean bellHovered =
                mouseX >= bellX - 4 && mouseX <= bellX + 18 &&
                        mouseY >= winY + 13 && mouseY <= winY + 39;
        auditBellHoverAmount = animateHover(
                auditBellHoverAmount,
                bellHovered,
                delta
        );

        context.drawTextWithShadow(
                tr,
                Text.literal("\uD83D\uDD14"),
                bellX,
                winY + 22,
                Ui.argb(
                        Ui.mix(
                                Ui.TEXT_MUTED,
                                Ui.ACCENT,
                                auditBellHoverAmount
                        ),
                        alpha
                )
        );

        // unread dot when the journal has entries
        int journalSize = com.uwdie.stafftools.client.audit.AuditLog
                .get().size();

        if (journalSize > 0) {

            context.fill(
                    bellX + 9,
                    winY + 18,
                    bellX + 12,
                    winY + 21,
                    Ui.argb(Ui.ACCENT, alpha)
            );
        }

        topBoxes.add(new int[]{bellX - 4, winY + 13, 22, 26});
        topActions.add(() -> MinecraftClient.getInstance()
                .setScreen(new AuditScreen(null)));

        if (bellHovered) {
            tipText = Lang.t(Lang.Key.BTN_AUDIT);
            tipX = bellX - 4;
            tipY = winY + 42;
        }

        right = bellX - 8;

        // profile dropdown: avatar square + name + caret
        int profileW = 128;
        int px = right - profileW;

        boolean profileHovered =
                mouseX >= px && mouseX <= px + profileW &&
                        mouseY >= winY + 13 && mouseY <= winY + 39;
        profileHoverAmount = animateHover(
                profileHoverAmount,
                profileHovered,
                delta
        );

        Ui.drawRoundRect(
                context, px, winY + 13, profileW, 26,
                Design.RADIUS_MD,
                Ui.argb(
                        Ui.mix(
                                Ui.SURFACE,
                                Ui.SURFACE_2,
                                profileHoverAmount
                        ),
                        alpha
                )
        );

        Ui.drawRoundBorder(
                context, px, winY + 13, profileW, 26,
                Design.RADIUS_MD,
                Ui.argb(
                        Ui.mix(Ui.BORDER, Ui.ACCENT, profileHoverAmount),
                        alpha
                )
        );

        Ui.drawRoundRect(
                context, px + 4, winY + 17, 18, 18,
                Design.RADIUS_SM,
                Ui.argb(Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.35f), alpha)
        );

        String profileName = com.uwdie.stafftools.client.StafftoolsClient
                .getConfig()
                .getActiveProfile()
                .getName();

        context.drawTextWithShadow(
                tr,
                Text.literal(
                        tr.trimToWidth(profileName, profileW - 46)
                ),
                px + 27,
                winY + 22,
                Ui.argb(Ui.TEXT, alpha)
        );

        context.drawTextWithShadow(
                tr,
                Text.literal("\u25BE"),
                px + profileW - 14,
                winY + 22,
                Ui.argb(
                        Ui.mix(
                                Ui.TEXT_MUTED,
                                Ui.ACCENT,
                                profileHoverAmount
                        ),
                        alpha
                )
        );

        topBoxes.add(new int[]{px, winY + 13, profileW, 26});
        topActions.add(() -> MinecraftClient.getInstance()
                .setScreen(new ProfilesScreen(null)));

        if (profileHovered) {
            tipText = Lang.t(Lang.Key.BTN_PROFILES);
            tipX = px;
            tipY = winY + 42;
        }

        right = px - 8;

        // status pill: moderator head + nick, or the server address
        if (status != null && !status.isBlank()) {

            int pillW = Math.min(
                    tr.getWidth(status) + 40,
                    right - left - 120
            );

            if (pillW > 40) {

                int sx = right - pillW;
                boolean statusHovered =
                        mouseX >= sx && mouseX <= sx + pillW &&
                                mouseY >= winY + 13 &&
                                mouseY <= winY + 39;
                statusHoverAmount = animateHover(
                        statusHoverAmount,
                        statusHovered,
                        delta
                );

                if (statusHoverAmount > 0.01f) {
                    Ui.drawShadow(
                            context, sx, winY + 13, pillW, 26,
                            Design.RADIUS_MD, 3,
                            0.07f * statusHoverAmount, alpha
                    );
                    Ui.drawHoverGlow(
                            context, sx, winY + 13, pillW, 26,
                            Design.RADIUS_MD, Ui.ACCENT,
                            statusHoverAmount * 0.55f, alpha
                    );
                }

                Ui.drawRoundRect(
                        context, sx, winY + 13, pillW, 26,
                        Design.RADIUS_MD,
                        Ui.argb(
                                Ui.mix(
                                        Ui.SURFACE,
                                        Ui.SURFACE_2,
                                        statusHoverAmount
                                ),
                                alpha
                        )
                );

                Ui.drawRoundBorder(
                        context, sx, winY + 13, pillW, 26,
                        Design.RADIUS_MD,
                        Ui.argb(
                                Ui.mix(
                                        Ui.BORDER,
                                        Ui.ACCENT,
                                        statusHoverAmount
                                ),
                                alpha
                        )
                );

                // the moderator's own head, so the pill always shows who
                // is on duty even when no server is joined
                drawOwnHead(context, sx + 4, winY + 17, alpha);

                context.drawTextWithShadow(
                        tr,
                        Text.literal(
                                tr.trimToWidth(status, pillW - 30)
                        ),
                        sx + 24,
                        winY + 22,
                        Ui.argb(
                                Ui.mix(
                                        Ui.TEXT_DIM,
                                        Ui.TEXT,
                                        statusHoverAmount
                                ),
                                alpha
                        )
                );

                topBoxes.add(new int[]{sx, winY + 13, pillW, 26});
                topActions.add(() -> MinecraftClient.getInstance()
                        .setScreen(new ActionHistoryScreen(null)));
            }
        }
    }

    /**
     * Draws the local player's head from their own skin, or a neutral
     * placeholder square when no world is loaded.
     */
    private void drawOwnHead(
            DrawContext context,
            int x,
            int y,
            float alpha
    ) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {

            Ui.drawRoundRect(
                    context, x, y, 16, 16,
                    Design.RADIUS_SM,
                    Ui.argb(Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.30f), alpha)
            );

            return;
        }

        var skin = client.player.getSkin().body().texturePath();

        int color = Ui.argb(0xFFFFFFFF, alpha);

        // front face of the head, with the hat layer on top
        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin,
                x, y,
                8.0F, 8.0F,
                16, 16,
                64, 64,
                color
        );

        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin,
                x, y,
                40.0F, 8.0F,
                16, 16,
                64, 64,
                color
        );
    }

    // ------------------------------------------------------------------
    // tabs
    // ------------------------------------------------------------------

    private void drawTabs(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha,
            float delta
    ) {

        // screens without tabs get no tab strip at all, so the content
        // starts right under the top bar
        if (tabs.isEmpty()) {
            return;
        }

        int left = winX + RAIL_W;

        context.fill(
                left, winY + TOP_H, winX + winW, winY + TOP_H + TABS_H,
                Ui.argb(Ui.BG, alpha)
        );

        int x = left + CONTENT_PAD;
        int y = winY + TOP_H + 8;
        int h = 24;

        for (int i = 0; i < tabs.size(); i++) {

            Tab tab = tabs.get(i);

            String label = Lang.t(tab.labelKey()).toUpperCase();

            int w = tr.getWidth(label) + 22;

            boolean active = i == activeTab;

            boolean hovered =
                    mouseX >= x && mouseX <= x + w &&
                            mouseY >= y && mouseY <= y + h;
            float hover = animateHover(
                    tabHoverAmounts[i],
                    hovered,
                    delta
            );
            tabHoverAmounts[i] = hover;

            if (!active && hover > 0.01f) {
                Ui.drawShadow(
                        context, x, y, w, h,
                        Design.RADIUS_MD, 2, 0.07f * hover, alpha
                );
                Ui.drawHoverGlow(
                        context, x, y, w, h,
                        Design.RADIUS_MD, Ui.ACCENT, hover * 0.5f, alpha
                );
            }

            Ui.drawRoundRect(
                    context, x, y, w, h,
                    Design.RADIUS_MD,
                    Ui.argb(
                            active
                                    ? Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.16f)
                                    : Ui.mix(Ui.SURFACE, Ui.SURFACE_2, hover),
                            alpha
                    )
            );

            Ui.drawRoundBorder(
                    context, x, y, w, h,
                    Design.RADIUS_MD,
                    Ui.argb(
                            active
                                    ? Ui.ACCENT
                                    : Ui.mix(
                                            Ui.BORDER,
                                            Ui.mix(Ui.BORDER, Ui.TEXT, 0.15f),
                                            hover
                                    ),
                            alpha
                    )
            );

            context.drawTextWithShadow(
                    tr,
                    Text.literal(label),
                    x + 11,
                    y + (h - 9) / 2,
                    Ui.argb(
                            active
                                    ? Ui.ACCENT
                                    : Ui.mix(Ui.TEXT_DIM, Ui.TEXT, hover),
                            alpha
                    )
            );

            tabBoxes.add(new int[]{x, y, w, h});

            if (hovered && tab.hintKey() != null) {
                tipText = Lang.t(tab.hintKey());
                tipX = x;
                tipY = y + h + 4;
            }

            x += w + 6;
        }
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    /** Routes a click into the rail, tabs or top bar. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (button != 0) {
            return false;
        }

        for (int i = 0; i < railBoxes.size(); i++) {

            if (hit(railBoxes.get(i), mouseX, mouseY)) {

                RailItem item = rail.get(i);

                if (item.action() != null) {
                    item.action().run();
                }

                return true;
            }
        }

        for (int i = 0; i < tabBoxes.size(); i++) {

            if (hit(tabBoxes.get(i), mouseX, mouseY)) {

                Tab tab = tabs.get(i);

                if (tab.action() != null) {
                    tab.action().run();
                }

                return true;
            }
        }

        for (int i = 0; i < topBoxes.size(); i++) {

            if (hit(topBoxes.get(i), mouseX, mouseY)) {

                Runnable action = topActions.get(i);

                if (action != null) {
                    action.run();
                }

                return true;
            }
        }

        return false;
    }

    private static boolean hit(int[] box, double x, double y) {

        return x >= box[0] && x <= box[0] + box[2] &&
                y >= box[1] && y <= box[1] + box[3];
    }
}
