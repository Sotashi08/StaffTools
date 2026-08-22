package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.macro.MacroExecutor;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * In-chat popup with player actions. Drawn on top of the current screen
 * (after the chat text), near the cursor, without closing the chat.
 * Has a pinned "Copy name" section and can be dragged by the corner grip.
 */
public final class PlayerActionOverlay {

    public static final PlayerActionOverlay INSTANCE =
            new PlayerActionOverlay();

    private static final int PANEL_WIDTH = 208;
    private static final int PAD = 6;
    private static final int HEADER_H = 30;
    private static final int FIXED_H = 20;
    private static final int FOOTER_H = 26;
    private static final int ROW_H = 16;
    private static final int ROW_GAP = 4;
    private static final int SEP_H = 15;
    private static final int MAX_HEIGHT = 240;
    private static final int GRIP_SIZE = 16;
    private static final long CONFIRM_MS = 3000L;

    // alias modal geometry
    private static final int ALIAS_HEADER_H = 28;
    private static final int ALIAS_ROW_H = 20;
    private static final int ALIAS_OPT_H = 15;

    private boolean open;
    private PlayerContext player;
    private Screen ownerScreen;
    private double anchorX;
    private double anchorY;
    private float scroll;
    private float scrollTarget;
    private long openTime;
    private long confirmAt;
    private RowItem confirmItem;

    // closing animation
    private boolean closing;
    private long closeAt;

    // "just executed" flash on a row
    private RowItem flashItem;
    private long flashTime;

    private int lastHoverRow = -1;

    // alias selection modal state
    private boolean aliasOpen;
    private Macro aliasMacro;
    private PlayerContext aliasTarget;
    private List<String> aliasList = new ArrayList<>();
    private int aliasScroll;
    private long aliasOpenTime;
    private int aliasGX;
    private int aliasGY;
    private int aliasGW;
    private int aliasGH;

    private boolean manuallyPlaced;
    private boolean dragging;
    private double grabDX;
    private double grabDY;

    private final List<RowItem> items = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int contentTop;
    private int contentBottom;

    private PlayerActionOverlay() {
    }

    public boolean isOpen() {
        return open;
    }

    public void open(PlayerContext target) {

        if (target == null) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        this.player = target;
        this.open = true;
        this.closing = false;
        this.ownerScreen = client.currentScreen;
        this.scroll = 0;
        this.scrollTarget = 0;
        this.confirmItem = null;
        this.confirmAt = 0L;
        this.manuallyPlaced = false;
        this.dragging = false;
        this.openTime = System.currentTimeMillis();
        this.flashItem = null;

        this.anchorX = client.mouse.getX();
        this.anchorY = client.mouse.getY();

        buildItems();
        layout();
    }

    /**
     * User-initiated close: plays the fade-out animation first.
     * delayMs allows showing the "just executed" flash before closing.
     */
    public void requestClose(long delayMs) {

        if (!open || closing) {
            return;
        }

        closing = true;
        closeAt = System.currentTimeMillis() + delayMs;

        confirmItem = null;
        confirmAt = 0L;

        if (delayMs <= 0) {
            clearAliasState();
        }
    }

    private void finishClose() {

        this.open = false;
        this.closing = false;
        this.player = null;
        this.ownerScreen = null;
        this.confirmItem = null;
        this.confirmAt = 0L;
        this.dragging = false;
        this.flashItem = null;

        clearAliasState();

        this.items.clear();
        this.rows.clear();
    }

    private void clearAliasState() {

        aliasOpen = false;
        aliasMacro = null;
        aliasTarget = null;
        aliasScroll = 0;

        if (aliasList != null) {
            aliasList.clear();
        }
    }

    /**
     * ESC handling: closes the alias modal first, then the popup.
     * Returns true if something was closed.
     */
    public boolean handleEscape() {

        if (aliasOpen) {
            clearAliasState();
            return true;
        }

        if (open) {
            requestClose(0);
            return true;
        }

        return false;
    }

    private void buildItems() {

        items.clear();

        for (ActionEntry entry :
                StafftoolsClient.getConfig()
                        .getActionEntries()) {

            if (!entry.isEnabled() || entry.isCopyName()) {
                continue;
            }

            items.add(new RowItem(entry));
        }

        boolean macroHeaderAdded = false;

        for (Macro macro :
                StafftoolsClient.getMacroManager()
                        .getMacros()) {

            if (!macro.isEnabled()) {
                continue;
            }

            if (!macroHeaderAdded) {

                items.add(
                        RowItem.separator(
                                Lang.t(Key.OVERLAY_CUSTOM_MACROS)
                        )
                );

                macroHeaderAdded = true;
            }

            items.add(new RowItem(macro));
        }
    }

    private void layout() {

        rows.clear();

        int cursorY = PAD + HEADER_H + FIXED_H;

        for (RowItem item : items) {

            int h = item.separator ? SEP_H : ROW_H;

            rows.add(new Row(cursorY, h, item));

            cursorY += h + ROW_GAP;
        }

        cursorY -= ROW_GAP;

        contentTop = PAD + HEADER_H + FIXED_H;

        int contentH = cursorY + PAD + FOOTER_H;
        int visibleH = Math.min(contentH, MAX_HEIGHT);

        int maxScroll = Math.max(
                0,
                contentH - visibleH
        );

        scrollTarget = Math.clamp(scrollTarget,
                0f, maxScroll);

        scroll = Math.clamp(scroll,
                0f, maxScroll);

        panelW = PANEL_WIDTH;
        panelH = visibleH;

        contentBottom = panelH - PAD - FOOTER_H;

        MinecraftClient client =
                MinecraftClient.getInstance();

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        if (!manuallyPlaced) {

            panelX = (int) Math.clamp(anchorX,
                    15, screenWidth - panelW - 4);

            panelY = (int) Math.clamp(anchorY,
                    4, screenHeight - panelH - 4);

        } else {

            panelX = Math.clamp(panelX,
                    4, screenWidth - panelW - 4);

            panelY = Math.clamp(panelY,
                    4, screenHeight - panelH - 4);
        }
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        return mouseX >= panelX &&
                mouseX <= panelX + panelW &&
                mouseY >= panelY &&
                mouseY <= panelY + panelH;
    }

    private boolean inGrip(double mouseX, double mouseY) {
        return mouseX >= panelX + panelW - GRIP_SIZE &&
                mouseX <= panelX + panelW &&
                mouseY >= panelY &&
                mouseY <= panelY + GRIP_SIZE;
    }

    private boolean inCopyRow(double mouseX, double mouseY) {

        if (!insidePanel(mouseX, mouseY)) {
            return false;
        }

        int top = PAD + HEADER_H;

        return mouseY >= panelY + top &&
                mouseY < panelY + top + FIXED_H;
    }

    private int hitRow(double mouseY) {

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            if (row.item.separator) {
                continue;
            }

            int absY = Math.round(row.offset - scroll);

            if (absY < contentTop - ROW_H ||
                    absY > contentBottom) {

                continue;
            }

            if (mouseY >= panelY + absY &&
                    mouseY < panelY + absY + ROW_H) {

                return i;
            }
        }

        return -1;
    }

    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (!open || player == null) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.currentScreen == null ||
                client.currentScreen != ownerScreen) {

            finishClose();
            return;
        }

        long now = System.currentTimeMillis();

        float t = Ui.clamp01(
                (now - openTime) / 160f
        );

        float scale =
                0.92f +
                        0.08f * Ui.easeOutCubic(t);

        float alpha = Ui.clamp01(
                (now - openTime) / 140f
        );

        // closing animation: fade out and slide up
        int riseY = 0;

        if (closing) {

            long passed = now - closeAt;

            if (passed < 0) {
                // delayed close (flash window) — draw normally
            } else {

                float k = Ui.clamp01(passed / 160f);

                if (k >= 1f) {
                    finishClose();
                    return;
                }

                alpha *= (1f - k);
                scale *= (1f - 0.03f * k);
                riseY = -(int) (8f * Ui.easeOutCubic(k));
            }
        }

        if (confirmItem != null &&
                now - confirmAt >= CONFIRM_MS) {

            confirmItem = null;
            confirmAt = 0L;
        }

        if (aliasOpen) {

            drawAliasModal(
                    context,
                    mouseX,
                    mouseY
            );

            return;
        }

        layout();

        // smooth scroll: ease `scroll` toward `scrollTarget`
        float diff = scrollTarget - scroll;

        if (Math.abs(diff) > 0.25f) {
            scroll += diff * Math.min(1f, delta * 14f);
        } else {
            scroll = scrollTarget;
        }

        var matrices =
                context.getMatrices();

        matrices.push();

        matrices.translate(
                panelX,
                panelY + riseY,
                0
        );

        matrices.translate(0, 0, 300);

        matrices.scale(
                scale,
                scale,
                1
        );

        Ui.drawPanel(
                context,
                0,
                0,
                panelW,
                panelH,
                alpha
        );

        Ui.drawHeader(
                context,
                0,
                0,
                panelW,
                alpha
        );

        TextRenderer tr =
                client.textRenderer;

        if (player.name() != null) {

            context.drawTextWithShadow(
                    tr,
                    Text.literal(player.name()),
                    PAD,
                    PAD + 3,
                    Ui.argb(Ui.TEXT, alpha)
            );
        }

        context.drawTextWithShadow(
                tr,
                Text.literal(
                        Lang.t(Key.OVERLAY_NP,
                                punishmentSummary(player.name()))
                ),
                PAD,
                PAD + 16,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        drawCopyRow(
                context,
                tr,
                mouseX,
                mouseY,
                alpha
        );

        context.fill(
                PAD,
                PAD + HEADER_H + FIXED_H,
                panelW - PAD,
                PAD + HEADER_H + FIXED_H + 1,
                Ui.argb(0x2C3B55, alpha)
        );

        // clip the scrolling list so rows never overlap
        // the header, copy row or footer
        int clipTop = panelY + PAD + HEADER_H + FIXED_H;
        int clipBottom = panelY + panelH - PAD - FOOTER_H;

        if (clipBottom > clipTop) {

            context.enableScissor(
                    0,
                    clipTop,
                    context.getScaledWindowWidth(),
                    clipBottom
            );

            drawBody(
                    context,
                    tr,
                    mouseX,
                    mouseY,
                    alpha
            );

            context.disableScissor();

        } else {

            drawBody(
                    context,
                    tr,
                    mouseX,
                    mouseY,
                    alpha
            );
        }

        drawFooter(
                context,
                tr,
                alpha
        );

        drawGrip(
                context,
                tr,
                mouseX,
                mouseY,
                alpha
        );

        matrices.pop();
    }

    /**
     * Total punishment count for the target, taken from the local history.
     */
    private String punishmentSummary(String name) {

        int total = 0;

        for (PunishmentRecord record :
                PunishmentHistory.get().getRecords()) {

            String recordPlayer = record.getPlayerName();

            if (recordPlayer == null ||
                    !recordPlayer.equalsIgnoreCase(name)) {

                continue;
            }

            total++;
        }

        return Integer.toString(total);
    }

    /**
     * Centered, non-draggable alias picker with accent styling.
     * Rows slide in left-to-right one after another. Click on an
     * option executes the macro; ESC or outside click cancels.
     */
    private void drawAliasModal(
            DrawContext context,
            double mouseX,
            double mouseY
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        TextRenderer tr = client.textRenderer;

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        long now = System.currentTimeMillis();

        float slide = Ui.easeOutCubic(
                Ui.clamp01(
                        (now - aliasOpenTime) / 220f
                )
        );

        float modalAlpha = Ui.clamp01(
                (now - aliasOpenTime) / 160f
        );

        if (modalAlpha <= 0.01f) {
            return;
        }

        aliasGW = 180;

        int pad = 6;

        int visible = Math.min(
                aliasList.size(),
                8
        );

        aliasGH = ALIAS_HEADER_H + visible * ALIAS_ROW_H + pad;

        // whole window slides in from the left
        int offsetX =
                (int) ((1f - slide) * -28);

        aliasGX = (screenWidth - aliasGW) / 2 + offsetX;
        aliasGY = (screenHeight - aliasGH) / 2;

        Ui.drawPanel(
                context,
                aliasGX,
                aliasGY,
                aliasGW,
                aliasGH,
                modalAlpha
        );

        // accent header strip
        Ui.drawGradientH(
                context,
                aliasGX + 3,
                aliasGY + 1,
                aliasGW - 6,
                2,
                Ui.ACCENT,
                Ui.ACCENT_2,
                modalAlpha
        );

        context.drawCenteredTextWithShadow(
                tr,
                Lang.text(Key.OVERLAY_ALIAS_TITLE),
                aliasGX + aliasGW / 2,
                aliasGY + pad + 4,
                Ui.argb(Ui.TEXT, modalAlpha)
        );

        if (aliasList.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    tr,
                    Lang.text(Key.MSG_NO_ACTIONS),
                    aliasGX + aliasGW / 2,
                    aliasGY + ALIAS_HEADER_H,
                    Ui.argb(Ui.TEXT_MUTED, modalAlpha)
            );

            return;
        }

        for (int i = 0; i < visible; i++) {

            int index = aliasScroll + i;

            String alias = aliasList.get(index);

            // staggered per-row slide-in
            float ri = Ui.easeOutCubic(
                    Ui.clamp01(
                            (now - aliasOpenTime
                                    - 70L - i * 45L) / 190f
                    )
            );

            if (ri <= 0.02f) {
                continue;
            }

            int rowOffsetX =
                    (int) ((1f - ri) * -34);

            // option "button": inset with a gap below the previous one
            int ry = aliasGY + ALIAS_HEADER_H
                    + i * ALIAS_ROW_H;

            int ox = aliasGX + PAD;
            int oy = ry + 2;
            int ow = aliasGW - PAD * 2;
            int oh = ALIAS_OPT_H;

            boolean hovered =
                    mouseX >= ox &&
                            mouseX <= ox + ow &&
                            mouseY >= oy &&
                            mouseY < oy + oh;

            if (hovered) {

                context.fill(
                        ox + rowOffsetX,
                        oy,
                        ox + ow + rowOffsetX,
                        oy + oh,
                        Ui.argb(0x403F8AE0, modalAlpha)
                );
            }

            Ui.drawRoundBorder(
                    context,
                    ox + rowOffsetX,
                    oy,
                    ow,
                    oh,
                    3,
                    Ui.argb(
                            hovered ? Ui.ACCENT : 0xFF3A4A66,
                            modalAlpha * ri
                    )
            );

            String label = tr.trimToWidth(
                    alias,
                    ow - 8
            );

            context.drawTextWithShadow(
                    tr,
                    Text.literal(label),
                    ox + 4 + rowOffsetX,
                    oy + (oh - 9) / 2,
                    Ui.argb(
                            hovered ? Ui.ACCENT_SOFT : Ui.TEXT,
                            modalAlpha * ri
                    )
            );
        }
    }

    private void drawCopyRow(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        int y = PAD + HEADER_H;

        int bg = Ui.argb(0x22000000, alpha);

        if (inCopyRow(mouseX, mouseY)) {
            bg = Ui.argb(0x403F8AE0, alpha);
        }

        context.fill(
                PAD,
                y,
                panelW - PAD,
                y + FIXED_H - 2,
                bg
        );

        Ui.drawRoundBorder(
                context,
                PAD,
                y,
                panelW - PAD * 2,
                FIXED_H - 2,
                3,
                Ui.argb(
                        inCopyRow(mouseX, mouseY)
                                ? Ui.ACCENT
                                : 0xFF2C3B55,
                        alpha
                )
        );

        String text =
                "\uD83D\uDCCB " +
                        Lang.t(Key.OVERLAY_COPY_NAME);

        context.drawTextWithShadow(
                tr,
                Text.literal(text),
                panelW / 2 - tr.getWidth(text) / 2,
                y + (FIXED_H - 9) / 2,
                Ui.argb(Ui.TEXT, alpha)
        );
    }

    private void drawBody(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        if (items.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    tr,
                    Lang.text(Key.OVERLAY_NO_ACTIONS),
                    panelW / 2,
                    contentTop + 8,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        boolean inside =
                insidePanel(mouseX, mouseY);

        long now = System.currentTimeMillis();

        float remain = 1f;

        if (confirmItem != null) {

            remain = Ui.clamp01(
                    (CONFIRM_MS - (now - confirmAt))
                            / (float) CONFIRM_MS
            );
        }

        int hoverNow = -1;

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            Row row = rows.get(rowIndex);

            float absYf = row.offset - scroll;
            int absY = Math.round(absYf);

            if (row.item.separator) {

                if (absY >= contentTop - SEP_H &&
                        absY <= contentBottom) {

                    context.drawCenteredTextWithShadow(
                            tr,
                            Text.literal(
                                    "\u2500 " +
                                            row.item.label +
                                            " \u2500"
                            ),
                            panelW / 2,
                            absY + (SEP_H - 9) / 2,
                            Ui.argb(0xFF88A0CC, alpha)
                    );
                }

                continue;
            }

            if (absY < contentTop - ROW_H ||
                    absY > contentBottom) {

                continue;
            }

            boolean hovered =
                    inside &&
                            mouseY >= panelY + absY &&
                            mouseY < panelY + absY + ROW_H;

            boolean confirming =
                    confirmItem == row.item;

            boolean flashing =
                    flashItem == row.item &&
                            now - flashTime < 300L;

            int bg = 0;

            if (confirming) {
                bg = Ui.argb(
                        0xFF8A2B2B,
                        alpha * (0.30f + 0.70f * remain)
                );
            } else if (flashing) {
                bg = Ui.argb(0x4055DD77, alpha);
            } else if (hovered) {
                bg = Ui.argb(0x403F8AE0, alpha);
            }

            if (bg != 0) {

                context.fill(
                        PAD,
                        absY,
                        panelW - PAD,
                        absY + ROW_H - 2,
                        bg
                );
            }

            int borderColor =
                    confirming
                            ? Ui.DANGER
                            : flashing
                            ? Ui.SUCCESS
                            : hovered
                            ? Ui.ACCENT
                            : 0xFF2C3B55;

            Ui.drawRoundBorder(
                    context,
                    PAD,
                    absY,
                    panelW - PAD * 2,
                    ROW_H - 2,
                    3,
                    Ui.argb(
                            borderColor,
                            alpha * (hovered || confirming
                                    ? 1f
                                    : 0.55f)
                    )
            );

            if (confirming) {

                int pw = (int)
                        ((panelW - PAD * 2) * remain);

                context.fill(
                        PAD + 1,
                        absY + ROW_H - 4,
                        PAD + 1 + pw,
                        absY + ROW_H - 3,
                        Ui.argb(Ui.DANGER, alpha)
                );
            }

            int color =
                    confirming
                            ? Ui.DANGER
                            : row.item.confirmationRequired
                            ? 0xFFE0A0A0
                            : Ui.TEXT;

            context.drawTextWithShadow(
                    tr,
                    Text.literal(row.item.label),
                    PAD + 4,
                    absY + (ROW_H - 9) / 2,
                    Ui.argb(color, alpha)
            );

            if (hovered) {
                hoverNow = rowIndex;
            }
        }

        // hover tick sound on row change
        if (hoverNow != lastHoverRow) {
            lastHoverRow = hoverNow;

            if (hoverNow >= 0) {
                Ui.playHover();
            }
        }
    }

    private void drawFooter(
            DrawContext context,
            TextRenderer tr,
            float alpha
    ) {

        String line1 =
                Lang.t(Key.OVERLAY_HINT_CLOSE) +
                        " • " +
                        Lang.t(Key.OVERLAY_HINT_SCROLL);

        context.drawCenteredTextWithShadow(
                tr,
                Text.literal(line1),
                panelW / 2,
                panelH - PAD - 9,
                Ui.argb(0xFF556070, alpha)
        );

        context.drawCenteredTextWithShadow(
                tr,
                Text.literal(
                        Lang.t(Key.OVERLAY_HINT_DRAG)
                ),
                panelW / 2,
                panelH - PAD + 2,
                Ui.argb(0xFF3A4455, alpha)
        );
    }

    private void drawGrip(
            DrawContext context,
            TextRenderer tr,
            double mouseX,
            double mouseY,
            float alpha
    ) {

        int gx = panelW - GRIP_SIZE;
        int gy = 0;

        boolean hovered =
                dragging ||
                        inGrip(mouseX, mouseY);

        float pulse = 0.5f + 0.5f *
                (float) Math.sin(
                        System.currentTimeMillis() / 280.0
                );

        int borderColor = hovered
                ? Ui.ACCENT
                : Ui.mix(0xFF3A4A66, Ui.ACCENT,
                0.25f + 0.75f * pulse);

        int dotColor = dragging
                ? Ui.ACCENT_SOFT
                : hovered
                ? Ui.ACCENT_SOFT
                : Ui.mix(0xFF556070, Ui.ACCENT_SOFT, pulse);

        if (hovered) {

            context.fill(
                    gx + 1,
                    gy + 1,
                    gx + GRIP_SIZE - 1,
                    gy + GRIP_SIZE - 1,
                    Ui.argb(0x603F8AE0, alpha)
            );
        }

        Ui.drawRoundBorder(
                context,
                gx,
                gy,
                GRIP_SIZE,
                GRIP_SIZE,
                3,
                Ui.argb(borderColor, alpha)
        );

        String dots = "\u22EF";

        context.drawTextWithShadow(
                tr,
                Text.literal(dots),
                gx + (GRIP_SIZE - tr.getWidth(dots)) / 2,
                gy + (GRIP_SIZE - 9) / 2 + 1,
                Ui.argb(dotColor, alpha)
        );
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (!open) {
            return false;
        }

        // swallow clicks while the close animation plays
        if (closing) {
            return true;
        }

        if (button != 0) {
            return true;
        }

        // alias modal swallows all clicks
        if (aliasOpen) {

            boolean inside =
                    mouseX >= aliasGX &&
                            mouseX <= aliasGX + aliasGW &&
                            mouseY >= aliasGY &&
                            mouseY <= aliasGY + aliasGH;

            if (inside && !aliasList.isEmpty()) {

                int startY =
                        aliasGY + ALIAS_HEADER_H + 2;

                // floor, not truncation: clicks above the first
                // option must give a negative row
                int row = (int) Math.floor(
                        (mouseY - startY) / ALIAS_ROW_H
                );

                boolean withinOptionY =
                        mouseY >= startY + row * ALIAS_ROW_H &&
                                mouseY < startY + row * ALIAS_ROW_H
                                        + ALIAS_OPT_H;

                int index = aliasScroll + row;

                if (row >= 0 &&
                        withinOptionY &&
                        index < aliasList.size()) {

                    String alias = aliasList.get(index);

                    MacroExecutor.executeWithAlias(
                            aliasMacro,
                            aliasTarget,
                            alias
                    );

                    Toasts.INSTANCE.success(
                            Lang.t(Key.TOAST_EXECUTED,
                                    aliasMacro.getName())
                    );
                }
            }

            // any click (option or outside) finishes the flow
            requestClose(0);

            return true;
        }

        layout();

        if (inGrip(mouseX, mouseY)) {

            dragging = true;
            manuallyPlaced = true;
            grabDX = mouseX - panelX;
            grabDY = mouseY - panelY;

            return true;
        }

        if (!insidePanel(mouseX, mouseY)) {
            requestClose(0);
            return false;
        }

        if (inCopyRow(mouseX, mouseY)) {
            copyName();
            return true;
        }

        int index = hitRow(mouseY);

        if (index < 0) {
            return true;
        }

        handleRow(rows.get(index).item);

        return true;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    ) {

        if (!open || !dragging) {
            return false;
        }

        panelX = (int) Math.round(mouseX - grabDX);
        panelY = (int) Math.round(mouseY - grabDY);

        layout();

        return true;
    }

    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (!open || !dragging) {
            return false;
        }

        dragging = false;

        return true;
    }

    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (!open) {
            return false;
        }

        if (aliasOpen) {

            if (mouseX >= aliasGX &&
                    mouseX <= aliasGX + aliasGW &&
                    mouseY >= aliasGY &&
                    mouseY <= aliasGY + aliasGH) {

                int visible = Math.min(
                        aliasList.size(),
                        8
                );

                int maxScroll = Math.max(
                        0,
                        aliasList.size() - visible
                );

                aliasScroll = Math.clamp(
                        aliasScroll
                                + (verticalAmount > 0 ? -1 : 1),
                        0,
                        maxScroll
                );
            }

            return true;
        }

        layout();

        if (!insidePanel(mouseX, mouseY)) {
            return false;
        }

        scrollTarget += verticalAmount > 0
                ? -ROW_H
                : ROW_H;

        layout();

        return true;
    }

    private void copyName() {

        MinecraftClient.getInstance().keyboard
                .setClipboard(player.name());

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_COPIED, player.name())
        );

        requestClose(0);
    }

    private void handleRow(RowItem item) {

        // second click on a confirming row — proceed to execution
        if (confirmItem == item) {

            confirmItem = null;
            confirmAt = 0L;

            if (execute(item)) {
                return;
            }

            // short flash on the row, then animated close
            requestClose(300);

            return;
        }

        if (item.confirmationRequired &&
                StafftoolsClient.getConfig()
                        .isDangerousMacroConfirmation()) {

            confirmItem = item;
            confirmAt = System.currentTimeMillis();

            return;
        }

        // deferred: alias modal is open — nothing sent yet,
        // the popup stays alive underneath
        if (execute(item)) {
            return;
        }

        requestClose(300);
    }

    /**
     * Executes the row action. Returns true when execution was DEFERRED
     * (the alias picker modal opened and the flow continues there);
     * false means everything already finished.
     */
    private boolean execute(RowItem item) {

        if (item.macro != null &&
                MacroExecutor.requiresAlias(item.macro)) {

            List<String> aliases =
                    item.macro.getAliases();

            if (aliases.isEmpty()) {

                MacroExecutor.executeWithAlias(
                        item.macro,
                        player,
                        ""
                );

                flashItem = item;
                flashTime = System.currentTimeMillis();

                Toasts.INSTANCE.success(
                        Lang.t(Key.TOAST_EXECUTED,
                                item.label)
                );

                return false;
            }

            aliasMacro = item.macro;
            aliasTarget = player;
            aliasList = new ArrayList<>(aliases);
            aliasScroll = 0;
            aliasOpenTime = System.currentTimeMillis();
            aliasOpen = true;

            return true;
        }

        if (item.macro != null) {

            MacroExecutor.execute(
                    item.macro,
                    player
            );

        } else {

            MacroExecutor.executeCommands(
                    item.commands,
                    player
            );
        }

        flashItem = item;
        flashTime = System.currentTimeMillis();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_EXECUTED, item.label)
        );

        return false;
    }

    private static final class RowItem {

        private String label;
        private final List<String> commands;
        private final boolean dangerous;
        private final boolean confirmationRequired;
        private boolean separator;
        private final Macro macro;

        private RowItem(ActionEntry entry) {

            this.label =
                    (entry.getIcon() == null
                            ? ""
                            : entry.getIcon()) +
                            " " +
                            entry.getLabel();

            this.commands =
                    List.of(entry.getCommand());

            this.dangerous = entry.isDangerous();
            this.confirmationRequired =
                    entry.isConfirmationRequired();

            this.separator = false;
            this.macro = null;
        }

        private RowItem(Macro macro) {

            this.label = "\uD83D\uDCCC " + macro.getName();
            this.commands = macro.getCommands();
            this.dangerous = macro.isDangerous();
            this.confirmationRequired =
                    macro.isConfirmationRequired();

            this.separator = false;
            this.macro = macro;
        }

        private static RowItem separator(String text) {

            RowItem item = new RowItem();
            item.separator = true;
            item.label = text;

            return item;
        }

        private RowItem() {

            this.label = "";
            this.commands = List.of();
            this.dangerous = false;
            this.confirmationRequired = false;
            this.separator = false;
            this.macro = null;
        }
    }

    private record Row(
            int offset,
            int height,
            RowItem item
    ) {
    }
}
