package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen, laid out like the reference launcher UI:
 *
 * <pre>
 *   [rail] | top bar: title, status pill, profile, window controls
 *          | tabs: General / Interface / Chat / System
 *          | content: section title + description, dropdown,
 *          |          colour swatches, two-column toggle rows
 * </pre>
 *
 * <p>The General tab is the dashboard: language, accent colour and the
 * most-used switches. The other tabs group the remaining settings and
 * the navigation entries, so nothing is more than one click away.
 */
public class StafftoolsScreen extends StScreen {

    private static final int TAB_GENERAL = 0;
    private static final int TAB_INTERFACE = 1;
    private static final int TAB_CHAT = 2;
    private static final int TAB_SYSTEM = 3;

    private static final int ROW_H = 30;
    private static final int ROW_GAP = 8;
    private static final int GENERAL_ROW_GAP = 4;
    private static final int GENERAL_LANGUAGE_TITLE_GAP = 30;
    private static final int GENERAL_LANGUAGE_HEIGHT = 26;
    private static final int GENERAL_LANGUAGE_GAP = 38;
    private static final int GENERAL_ACCENT_TITLE_GAP = 30;
    private static final int GENERAL_SWITCH_GAP = 14;
    private static final int GENERAL_SWITCH_ROWS = 3;
    private static final int SWATCH = 30;
    private static final int SWATCH_GAP = 8;

    private int tab = TAB_GENERAL;

    /** Hit boxes of the interactive content, rebuilt every frame. */
    private final List<Object[]> hits = new ArrayList<>();

    /** Language dropdown state. */
    private boolean langOpen;
    private float frameDelta;
    private int generalScroll;

    /** Animated knob positions, keyed by setting id. */
    private final java.util.Map<String, Float> knobs =
            new java.util.HashMap<>();
    private final java.util.Map<String, Float> hoverAmounts =
            new java.util.HashMap<>();

    public StafftoolsScreen() {
        super(Lang.text(Key.APP_TITLE));
    }

    @Override
    protected void init() {

        resetSlots();

        clearChildren();

        configureShell();

        shell.tabs(
                new StShell.Tab(Key.AUDIT_TAB_LOG, Key.HINT_TAB_GENERAL,
                        () -> switchTab(TAB_GENERAL)),
                new StShell.Tab(Key.MENU_SECTION_TOOLS, Key.HINT_TAB_INTERFACE,
                        () -> switchTab(TAB_INTERFACE)),
                new StShell.Tab(Key.HUD_TITLE, Key.HINT_TAB_CHAT,
                        () -> switchTab(TAB_CHAT)),
                new StShell.Tab(Key.MENU_SECTION_SYSTEM, Key.HINT_TAB_SYSTEM,
                        () -> switchTab(TAB_SYSTEM))
        );

        shell.activeTab(tab);
        shell.activeRail(-1);
    }

    private void switchTab(int next) {

        if (tab == next) {
            return;
        }

        tab = next;
        langOpen = false;

        init();
    }

    @Override
    protected String shellStatus() {

        String server = PunishmentHistory.currentServerKey();

        return server == null || server.isBlank()
                ? Lang.t(Key.HUD_OFFLINE)
                : server;
    }

    @Override
    protected int shellTitleAnimation() {
        return StafftoolsClient.getConfig().getTitleAnimation();
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        hits.clear();
        frameDelta = delta;

        int x = contentLeft();
        int w = contentWidth();
        int y = contentTop();

        if (tab == TAB_GENERAL) {
            int top = contentTop();
            int bottom = contentBottom();
            int viewportHeight = Math.max(1, bottom - top);
            int maxScroll = generalMaxScroll(w);

            generalScroll = Math.clamp(generalScroll, 0, maxScroll);
            context.enableScissor(x, top, x + w, bottom);
            renderGeneral(
                    context,
                    x,
                    y - generalScroll,
                    w,
                    mouseX,
                    mouseY,
                    alpha
            );
            context.disableScissor();

            if (maxScroll > 0) {
                Ui.drawScrollFade(
                        context,
                        x,
                        top,
                        w,
                        bottom,
                        Ui.clamp01(generalScroll / 12f),
                        Ui.clamp01((maxScroll - generalScroll) / 12f),
                        alpha
                );
                Ui.drawScrollbar(
                        context,
                        x + w - 4,
                        top,
                        viewportHeight,
                        generalScroll / (float) maxScroll,
                        viewportHeight / (float) (viewportHeight + maxScroll),
                        alpha
                );
            }

            return;
        }

        switch (tab) {

            case TAB_INTERFACE -> renderInterface(context, x, y, w, mouseX, mouseY, alpha);
            case TAB_CHAT -> renderChat(context, x, y, w, mouseX, mouseY, alpha);
            case TAB_SYSTEM -> renderSystem(context, x, y, w, mouseX, mouseY, alpha);
            default -> {
            }
        }
    }

    private int generalMaxScroll(int width) {

        int perRow = Math.max(
                1,
                (width - 150) / (SWATCH + SWATCH_GAP)
        );
        int themeRows = (Theme.count() + perRow - 1) / perRow;
        int swatchHeight = themeRows * (SWATCH + SWATCH_GAP)
                - SWATCH_GAP;
        int contentHeight =
                GENERAL_LANGUAGE_TITLE_GAP
                        + GENERAL_LANGUAGE_HEIGHT
                        + GENERAL_LANGUAGE_GAP
                        + GENERAL_ACCENT_TITLE_GAP
                        + swatchHeight
                        + GENERAL_SWITCH_GAP
                        + GENERAL_SWITCH_ROWS * ROW_H
                        + (GENERAL_SWITCH_ROWS - 1) * GENERAL_ROW_GAP;

        return Math.max(
                0,
                contentHeight - (contentBottom() - contentTop())
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (tab == TAB_GENERAL &&
                mouseX >= contentLeft() &&
                mouseX <= contentLeft() + contentWidth() &&
                mouseY >= contentTop() &&
                mouseY <= contentBottom() &&
                verticalAmount != 0) {

            generalScroll = Math.clamp(
                    generalScroll + (verticalAmount > 0 ? -32 : 32),
                    0,
                    generalMaxScroll(contentWidth())
            );
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    // ------------------------------------------------------------------
    // General tab
    // ------------------------------------------------------------------

    private void renderGeneral(
            DrawContext context,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        // --- language ---
        Ui.drawSectionTitle(
                context, textRenderer,
                x, y,
                "\u25C9",
                Lang.t(Key.TOGGLE_LANGUAGE),
                Lang.t(Key.MSG_LANGUAGE_HINT),
                alpha
        );

        y += GENERAL_LANGUAGE_TITLE_GAP;

        int selectW = Math.min(300, w);

        boolean langHovered =
                mouseX >= x && mouseX <= x + selectW &&
                        mouseY >= y &&
                                mouseY <= y + GENERAL_LANGUAGE_HEIGHT;
        float langHover = hoverAmount("language-select", langHovered);

        Ui.drawSelect(
                context, textRenderer,
                x, y, selectW, GENERAL_LANGUAGE_HEIGHT,
                Lang.getLanguage().getDisplayName(),
                langOpen, langHover, alpha
        );

        hits.add(new Object[]{
                "lang", x, y, selectW, GENERAL_LANGUAGE_HEIGHT
        });

        if (langOpen) {

            Lang.Language[] values = Lang.Language.values();

            int menuY = y + 28;

            for (int i = 0; i < values.length; i++) {

                int itemY = menuY + i * 20;

                boolean over =
                        mouseX >= x && mouseX <= x + selectW &&
                                mouseY >= itemY && mouseY <= itemY + 20;

                Ui.drawRoundRect(
                        context, x, itemY, selectW, 20,
                        Design.RADIUS_SM,
                        Ui.argb(
                                over
                                        ? Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.20f)
                                        : Ui.SURFACE_2,
                                alpha
                        )
                );

                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal(values[i].getDisplayName()),
                        x + 10,
                        itemY + 6,
                        Ui.argb(
                                values[i] == Lang.getLanguage()
                                        ? Ui.ACCENT
                                        : Ui.TEXT,
                                alpha
                        )
                );

                hits.add(new Object[]{"lang:" + i, x, itemY, selectW, 20});
            }
        }

        y += GENERAL_LANGUAGE_GAP;

        // --- accent colour ---
        Ui.drawSectionTitle(
                context, textRenderer,
                x, y,
                "\u25D0",
                Lang.t(Key.LABEL_ACCENT_COLOR),
                Lang.t(Key.MSG_ACCENT_HINT),
                alpha
        );

        y += GENERAL_ACCENT_TITLE_GAP;

        int perRow = Math.max(
                1,
                (w - 150) / (SWATCH + SWATCH_GAP)
        );

        int col = 0;
        int rowY = y;

        for (int i = 0; i < Theme.count(); i++) {

            int sx = x + col * (SWATCH + SWATCH_GAP);

            boolean selected = i == Theme.getIndex();

            boolean hovered =
                    mouseX >= sx && mouseX <= sx + SWATCH &&
                            mouseY >= rowY && mouseY <= rowY + SWATCH;
            float hover = hoverAmount("theme-" + i, hovered);

            Ui.drawSwatch(
                    context,
                    sx, rowY, SWATCH,
                    Theme.accentOf(i),
                    selected, hover, alpha
            );

            hits.add(new Object[]{"theme:" + i, sx, rowY, SWATCH, SWATCH});

            col++;

            if (col >= perRow) {
                col = 0;
                rowY += SWATCH + SWATCH_GAP;
            }
        }

        // the swatch block must never run under the custom colour card
        int swatchRows = (Theme.count() + perRow - 1) / perRow;
        int swatchBottom = y
                + swatchRows * (SWATCH + SWATCH_GAP)
                - SWATCH_GAP;

        // custom colour card on the right, like the reference
        int cardW = 132;
        int cardX = x + w - cardW;

        Ui.drawRoundRect(
                context, cardX, y, cardW, 44,
                Design.RADIUS_MD,
                Ui.argb(Ui.SURFACE, alpha)
        );

        Ui.drawRoundBorder(
                context, cardX, y, cardW, 44,
                Design.RADIUS_MD,
                Ui.argb(Ui.BORDER, alpha)
        );

        Ui.drawRoundRect(
                context, cardX + 8, y + 10, 24, 24,
                Design.RADIUS_MD,
                Ui.argb(Ui.NICK_HIGHLIGHT, alpha)
        );

        // the label is trimmed to the space left of the card edge, so it
        // can never spill outside the panel
        int labelW = cardW - 40 - 8;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(
                                Lang.t(Key.LABEL_ACCENT_COLOR),
                                labelW
                        )
                ),
                cardX + 40,
                y + 12,
                Ui.argb(Ui.TEXT, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(String.format(
                        "#%06X",
                        Ui.NICK_HIGHLIGHT & 0xFFFFFF
                )),
                cardX + 40,
                y + 24,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        hits.add(new Object[]{"nick", cardX, y, cardW, 44});

        y = swatchBottom + GENERAL_SWITCH_GAP;

        // --- quick switches, two columns ---
        StaffToolsConfig config = StafftoolsClient.getConfig();

        int colGap = 10;
        int colW = (w - colGap) / 2;

        String[][] left = {
                {"actions", Lang.t(Key.TOGGLE_ACTIONS_POPUP)},
                {"confirm", Lang.t(Key.TOGGLE_CONFIRM_DANGER)},
                {"toasts", Lang.t(Key.TOGGLE_TOASTS)}
        };

        String[][] right = {
                {"chatInput", Lang.t(Key.TOGGLE_CHAT_INPUT)},
                {"chatFrame", Lang.t(Key.TOGGLE_CHAT_FRAME)},
                {"modHud", Lang.t(Key.TOGGLE_MOD_HUD)}
        };

        for (int i = 0; i < left.length; i++) {

            int ry = y + i * (ROW_H + GENERAL_ROW_GAP);

            drawToggle(
                    context, left[i][0], left[i][1],
                    x, ry, colW,
                    toggleValue(config, left[i][0]),
                    mouseX, mouseY, alpha
            );

            drawToggle(
                    context, right[i][0], right[i][1],
                    x + colW + colGap, ry, colW,
                    toggleValue(config, right[i][0]),
                    mouseX, mouseY, alpha
            );
        }
    }

    // ------------------------------------------------------------------
    // Interface tab
    // ------------------------------------------------------------------

    private void renderInterface(
            DrawContext context,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        Ui.drawSectionTitle(
                context, textRenderer,
                x, y,
                "\u25A6",
                Lang.t(Key.MENU_SECTION_TOOLS),
                Lang.t(Key.MSG_INTERFACE_HINT),
                alpha
        );

        y += 34;

        StaffToolsConfig config = StafftoolsClient.getConfig();

        int colGap = 10;
        int colW = (w - colGap) / 2;

        String[][] left = {
                {"actions", Lang.t(Key.TOGGLE_ACTIONS_POPUP)},
                {"confirm", Lang.t(Key.TOGGLE_CONFIRM_DANGER)},
                {"toasts", Lang.t(Key.TOGGLE_TOASTS)},
                {"anim", Lang.t(Key.TOGGLE_MENU_ANIMATION)},
                {"savePopup", Lang.t(Key.TOGGLE_SAVE_POPUP_SIZE)}
        };

        String[][] right = {
                {"chatFrame", Lang.t(Key.TOGGLE_CHAT_FRAME)},
                {"chatResize", Lang.t(Key.TOGGLE_CHAT_RESIZE)},
                {"saveChat", Lang.t(Key.TOGGLE_SAVE_CHAT_SIZE)},
                {"modHud", Lang.t(Key.TOGGLE_MOD_HUD)},
                {"modHudPhrases", Lang.t(Key.TOGGLE_MOD_HUD_PHRASES)}
        };

        for (int i = 0; i < left.length; i++) {

            int ry = y + i * (ROW_H + ROW_GAP);

            drawToggle(
                    context, left[i][0], left[i][1],
                    x, ry, colW,
                    toggleValue(config, left[i][0]),
                    mouseX, mouseY, alpha
            );

            drawToggle(
                    context, right[i][0], right[i][1],
                    x + colW + colGap, ry, colW,
                    toggleValue(config, right[i][0]),
                    mouseX, mouseY, alpha
            );
        }

        y += left.length * (ROW_H + ROW_GAP) + 14;

        drawNavRow(context, x, y, w, mouseX, mouseY, alpha);
    }

    // ------------------------------------------------------------------
    // Chat tab
    // ------------------------------------------------------------------

    private void renderChat(
            DrawContext context,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        Ui.drawSectionTitle(
                context, textRenderer,
                x, y,
                "\u25C8",
                Lang.t(Key.TITLE_DETECTION),
                Lang.t(Key.MSG_DETECT_HINT),
                alpha
        );

        y += 34;

        StaffToolsConfig config = StafftoolsClient.getConfig();

        int colGap = 10;
        int colW = (w - colGap) / 2;

        String[][] left = {
                {"plain", Lang.t(Key.TOGGLE_DETECT_PLAIN)},
                {"cache", Lang.t(Key.TOGGLE_DETECT_CACHE)},
                {"display", Lang.t(Key.TOGGLE_DETECT_DISPLAY)},
                {"aliases", Lang.t(Key.TOGGLE_DETECT_ALIASES)},
                {"click", Lang.t(Key.TOGGLE_DETECT_CLICK)}
        };

        String[][] right = {
                {"hover", Lang.t(Key.TOGGLE_DETECT_HOVER)},
                {"unicode", Lang.t(Key.TOGGLE_DETECT_UNICODE)},
                {"decor", Lang.t(Key.TOGGLE_DETECT_DECOR)},
                {"line", Lang.t(Key.TOGGLE_DETECT_LINE)},
                {"chatInput", Lang.t(Key.TOGGLE_CHAT_INPUT)}
        };

        for (int i = 0; i < left.length; i++) {

            int ry = y + i * (ROW_H + ROW_GAP);

            drawToggle(
                    context, left[i][0], left[i][1],
                    x, ry, colW,
                    toggleValue(config, left[i][0]),
                    mouseX, mouseY, alpha
            );

            drawToggle(
                    context, right[i][0], right[i][1],
                    x + colW + colGap, ry, colW,
                    toggleValue(config, right[i][0]),
                    mouseX, mouseY, alpha
            );
        }

        y += left.length * (ROW_H + ROW_GAP) + 14;

        drawNavRow(context, x, y, w, mouseX, mouseY, alpha);
    }

    // ------------------------------------------------------------------
    // System tab
    // ------------------------------------------------------------------

    private void renderSystem(
            DrawContext context,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        Ui.drawSectionTitle(
                context, textRenderer,
                x, y,
                "\u2699",
                Lang.t(Key.MENU_SECTION_SYSTEM),
                Lang.t(Key.MSG_SYSTEM_HINT),
                alpha
        );

        y += 34;

        StaffToolsConfig config = StafftoolsClient.getConfig();

        int colGap = 10;
        int colW = (w - colGap) / 2;

        drawToggle(
                context, "audit", Lang.t(Key.TOGGLE_AUDIT),
                x, y, colW,
                toggleValue(config, "audit"),
                mouseX, mouseY, alpha
        );

        drawToggle(
                context, "profiler", Lang.t(Key.TOGGLE_PROFILER),
                x + colW + colGap, y, colW,
                toggleValue(config, "profiler"),
                mouseX, mouseY, alpha
        );

        y += ROW_H + ROW_GAP + 14;

        drawNavRow(context, x, y, w, mouseX, mouseY, alpha);
    }

    // ------------------------------------------------------------------
    // shared pieces
    // ------------------------------------------------------------------

    /** Navigation cards: macros, actions, history, profiles, audit. */
    private void drawNavRow(
            DrawContext context,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        String[][] entries = {
                {"macros", "\u26A1", Lang.t(Key.BTN_MACROS)},
                {"actions", "\uD83D\uDCCB", Lang.t(Key.BTN_PLAYER_ACTIONS)},
                {"history", "\uD83D\uDD52", Lang.t(Key.BTN_HISTORY)},
                {"profiles", "\uD83D\uDC64", Lang.t(Key.BTN_PROFILES)},
                {"audit", "\uD83D\uDD0E", Lang.t(Key.BTN_AUDIT)},
                {"settings", "\u2699", Lang.t(Key.BTN_SETTINGS)}
        };

        int gap = 8;
        int cardW = (w - gap * 2) / 3;
        int cardH = 34;

        for (int i = 0; i < entries.length; i++) {

            int cx = x + (i % 3) * (cardW + gap);
            int cy = y + (i / 3) * (cardH + gap);

            boolean hovered =
                    mouseX >= cx && mouseX <= cx + cardW &&
                            mouseY >= cy && mouseY <= cy + cardH;

            Ui.drawRoundRect(
                    context, cx, cy, cardW, cardH,
                    Design.RADIUS_MD,
                    Ui.argb(
                            hovered
                                    ? Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.16f)
                                    : Ui.SURFACE,
                            alpha
                    )
            );

            Ui.drawRoundBorder(
                    context, cx, cy, cardW, cardH,
                    Design.RADIUS_MD,
                    Ui.argb(hovered ? Ui.ACCENT : Ui.BORDER, alpha)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(entries[i][1]),
                    cx + 10,
                    cy + (cardH - 9) / 2,
                    Ui.argb(hovered ? Ui.ACCENT : Ui.TEXT_MUTED, alpha)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(
                                    entries[i][2],
                                    cardW - 34
                            )
                    ),
                    cx + 28,
                    cy + (cardH - 9) / 2,
                    Ui.argb(Ui.TEXT, alpha)
            );

            hits.add(new Object[]{"nav:" + entries[i][0], cx, cy, cardW, cardH});
        }
    }

    /** One settings row with an animated pill toggle. */
    private void drawToggle(
            DrawContext context,
            String id,
            String label,
            int x,
            int y,
            int w,
            boolean value,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        boolean hovered =
                mouseX >= x && mouseX <= x + w &&
                        mouseY >= y && mouseY <= y + ROW_H;
        float hover = hoverAmount(id, hovered);

        float target = value ? 1f : 0f;

        float current = knobs.getOrDefault(id, target);

        current = Ui.animateTowards(
                current,
                target,
                frameDelta,
                15f
        );

        knobs.put(id, current);

        Ui.drawToggleRow(
                context, textRenderer,
                x, y, w, ROW_H,
                label, current, hover, alpha
        );

        hits.add(new Object[]{"toggle:" + id, x, y, w, ROW_H});
    }

    private float hoverAmount(String id, boolean hovered) {

        float current = hoverAmounts.getOrDefault(id, 0f);
        float next = Ui.animateTowards(
                current,
                hovered ? 1f : 0f,
                frameDelta,
                14f
        );
        hoverAmounts.put(id, next);
        return next;
    }

    /** Current value of a setting by its short id. */
    private static boolean toggleValue(
            StaffToolsConfig config,
            String id
    ) {

        return switch (id) {
            case "actions" -> config.isPlayerActionsEnabled();
            case "confirm" -> config.isDangerousMacroConfirmation();
            case "toasts" -> config.isToastsEnabled();
            case "anim" -> config.isMenuAnimation();
            case "savePopup" -> config.isSavePopupSize();
            case "chatFrame" -> config.isChatFrameEnabled();
            case "chatResize" -> config.isChatResizeEnabled();
            case "saveChat" -> config.isSaveChatSize();
            case "modHud" -> config.isModHudEnabled();
            case "modHudPhrases" -> config.isModHudPhrases();
            case "plain" -> config.isDetectPlain();
            case "cache" -> config.isDetectCache();
            case "display" -> config.isDetectDisplay();
            case "aliases" -> config.isDetectAliases();
            case "click" -> config.isDetectClick();
            case "hover" -> config.isDetectHover();
            case "unicode" -> config.isDetectUnicode();
            case "decor" -> config.isDetectDecor();
            case "line" -> config.isDetectLine();
            case "chatInput" -> config.isChatInputDetectEnabled();
            case "audit" -> config.isAuditLogEnabled();
            case "profiler" -> config.isProfilerEnabled();
            default -> false;
        };
    }

    /** Flips a setting by its short id and persists it. */
    private static void flip(StaffToolsConfig config, String id) {

        switch (id) {
            case "actions" -> config.setPlayerActionsEnabled(
                    !config.isPlayerActionsEnabled());
            case "confirm" -> config.setDangerousMacroConfirmation(
                    !config.isDangerousMacroConfirmation());
            case "toasts" -> config.setToastsEnabled(
                    !config.isToastsEnabled());
            case "anim" -> config.setMenuAnimation(
                    !config.isMenuAnimation());
            case "savePopup" -> config.setSavePopupSize(
                    !config.isSavePopupSize());
            case "chatFrame" -> config.setChatFrameEnabled(
                    !config.isChatFrameEnabled());
            case "chatResize" -> config.setChatResizeEnabled(
                    !config.isChatResizeEnabled());
            case "saveChat" -> config.setSaveChatSize(
                    !config.isSaveChatSize());
            case "modHud" -> config.setModHudEnabled(
                    !config.isModHudEnabled());
            case "modHudPhrases" -> config.setModHudPhrases(
                    !config.isModHudPhrases());
            case "plain" -> config.setDetectPlain(
                    !config.isDetectPlain());
            case "cache" -> config.setDetectCache(
                    !config.isDetectCache());
            case "display" -> config.setDetectDisplay(
                    !config.isDetectDisplay());
            case "aliases" -> config.setDetectAliases(
                    !config.isDetectAliases());
            case "click" -> config.setDetectClick(
                    !config.isDetectClick());
            case "hover" -> config.setDetectHover(
                    !config.isDetectHover());
            case "unicode" -> config.setDetectUnicode(
                    !config.isDetectUnicode());
            case "decor" -> config.setDetectDecor(
                    !config.isDetectDecor());
            case "line" -> config.setDetectLine(
                    !config.isDetectLine());
            case "chatInput" -> config.setChatInputDetectEnabled(
                    !config.isChatInputDetectEnabled());
            case "audit" -> config.setAuditLogEnabled(
                    !config.isAuditLogEnabled());
            case "profiler" -> config.setProfilerEnabled(
                    !config.isProfilerEnabled());
            default -> {
            }
        }

        StafftoolsClient.saveConfig();
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {

        if (click.button() == 0) {
            if (tab == TAB_GENERAL &&
                    (click.y() < contentTop() ||
                            click.y() > contentBottom())) {
                langOpen = false;
                return super.mouseClicked(click, doubled);
            }

            for (Object[] hit : hits) {

                int hx = (int) hit[1];
                int hy = (int) hit[2];
                int hw = (int) hit[3];
                int hh = (int) hit[4];

                if (click.x() < hx || click.x() > hx + hw ||
                        click.y() < hy || click.y() > hy + hh) {

                    continue;
                }

                String id = (String) hit[0];

                if (id.equals("lang")) {

                    langOpen = !langOpen;
                    return true;
                }

                if (id.startsWith("lang:")) {

                    int index = Integer.parseInt(id.substring(5));

                    Lang.setLanguage(Lang.Language.values()[index]);

                    StafftoolsClient.getConfig()
                            .setLanguage(Lang.getLanguage());

                    StafftoolsClient.saveConfig();

                    langOpen = false;

                    init();

                    return true;
                }

                if (id.startsWith("theme:")) {

                    int index = Integer.parseInt(id.substring(6));

                    StafftoolsClient.getConfig().setThemeIndex(index);

                    Theme.apply(index);

                    StafftoolsClient.saveConfig();

                    Ui.playHover();

                    init();

                    return true;
                }

                if (id.equals("nick")) {

                    client.setScreen(new ThemesScreen());

                    return true;
                }

                if (id.startsWith("toggle:")) {

                    flip(
                            StafftoolsClient.getConfig(),
                            id.substring(7)
                    );

                    Ui.playHover();

                    return true;
                }

                if (id.startsWith("nav:")) {

                    openNav(id.substring(4));

                    return true;
                }
            }

            // a click outside the language menu closes it
            if (langOpen) {
                langOpen = false;
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void openNav(String id) {

        switch (id) {
            case "macros" -> client.setScreen(new MacroListScreen(this));
            case "actions" -> client.setScreen(new ActionsConfigScreen(this));
            case "history" -> client.setScreen(new ActionHistoryScreen(this));
            case "profiles" -> client.setScreen(new ProfilesScreen(this));
            case "audit" -> client.setScreen(new AuditScreen(this));
            case "settings" -> client.setScreen(new SettingsScreen(this));
            default -> {
            }
        }
    }
}
