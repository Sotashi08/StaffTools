package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-action list, styled like the rest of the mod UI: framed panel,
 * themed rows with entrance stagger, hover highlight and StButtons.
 * Rows scroll by moving their buttons, so nothing is ever rebuilt.
 */
public class ActionsConfigScreen extends StScreen {

    private static final int ROW_H = 30;
    private static final int ROW_GAP = 3;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int BTN_H = 18;
    private static final int FADE_H = 12;

    /** Rows live inside the frame, never flush with its border. */
    private static final int ROW_PAD = 9;

    // same spring + rubber band as the in-chat actions popup
    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;

    private final Screen parent;

    private float scroll;
    private float scrollPos;
    private float scrollVel;
    private boolean needsRebuild;
    private int listBottom;
    private int rowLeft;
    private int rowWidth;

    private final List<Row> rows =
            new ArrayList<>();

    public ActionsConfigScreen(Screen parent) {

        super(Lang.text(Key.BTN_PLAYER_ACTIONS));
        this.parent = parent;
    }

    @Override
    public void tick() {

        if (needsRebuild) {
            needsRebuild = false;
            init();
        }
    }

    @Override
    protected void init() {

        resetSlots();

        // NOTE: openTime is deliberately NOT reset here — the entrance
        // animation must play once per screen instance (entering the
        // list, or coming back from the editor), never on a rebuild.

        rows.clear();
        clearChildren();

        List<ActionEntry> entries =
                StafftoolsClient.getConfig()
                        .getActionEntries();

        rowWidth = Math.min(300, formWidth());
        rowLeft = (width - rowWidth) / 2;

        listBottom = height - BOTTOM_PAD;

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < entries.size(); i++) {

            Row row =
                    new Row(entries.get(i), i, LIST_TOP + i * step);

            rows.add(row);
            addRowButtons(row);
        }

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;

        int half = (rowWidth - 10) / 2;

        button(
                rowLeft,
                height - 32,
                half,
                22,
                Key.BTN_ADD_ACTION,
                b -> client.setScreen(
                        new ActionEntryEditScreen(this, null, true)
                )
        );

        button(
                rowLeft + half + 10,
                height - 32,
                half,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    private int maxScroll() {

        if (rows.isEmpty()) {
            return 0;
        }

        int content =
                rows.size() * ROW_H +
                        (rows.size() - 1) * ROW_GAP;

        return Math.max(
                0,
                content - (listBottom - LIST_TOP)
        );
    }

    private void addRowButtons(Row row) {

        int y = row.baseY + (ROW_H - BTN_H) / 2;

        int right = rowLeft + rowWidth - ROW_PAD - 5;

        row.btnDelete = button(
                right - 36,
                y,
                36,
                BTN_H,
                Key.BTN_DELETE,
                b -> {
                    StafftoolsClient.getConfig()
                            .getActionEntries()
                            .remove(row.entry);

                    StafftoolsClient.saveConfig();

                    // rebuild from tick(): touching the widget list
                    // inside a click handler breaks the iteration
                    needsRebuild = true;
                },
                Ui.DANGER
        );

        row.btnEdit = button(
                right - 36 - 3 - 48,
                y,
                48,
                BTN_H,
                Key.BTN_EDIT,
                b -> client.setScreen(
                        new ActionEntryEditScreen(
                                this, row.entry, false
                        )
                )
        );

        row.btnDown = glyphButton(
                right - 36 - 3 - 48 - 3 - 20,
                y,
                20,
                "\u25BC",
                b -> move(row.index, 1)
        );

        row.btnUp = glyphButton(
                right - 36 - 3 - 48 - 3 - 20 - 3 - 20,
                y,
                20,
                "\u25B2",
                b -> move(row.index, -1)
        );
    }

    /** Small arrow button (no lang key — the glyph is the label). */
    private StButton glyphButton(
            int x,
            int y,
            int width,
            String glyph,
            ButtonWidget.PressAction onPress
    ) {

        StButton widget = new StButton(
                x,
                y,
                width,
                BTN_H,
                Text.literal(glyph),
                onPress,
                openTime,
                nextSlot(),
                Ui.ACCENT
        );

        addDrawableChild(widget);

        return widget;
    }

    /** Buttons follow their row while scrolling; hidden when off-screen. */
    private void positionRowButtons(Row row, int buttonY, boolean visible) {

        row.btnUp.setY(buttonY);
        row.btnDown.setY(buttonY);
        row.btnEdit.setY(buttonY);
        row.btnDelete.setY(buttonY);

        row.btnUp.setVisible(visible);
        row.btnDown.setVisible(visible);
        row.btnEdit.setVisible(visible);
        row.btnDelete.setVisible(visible);
    }

    private void move(int index, int delta) {

        List<ActionEntry> entries =
                StafftoolsClient.getConfig()
                        .getActionEntries();

        int target = index + delta;

        if (target < 0 ||
                target >= entries.size()) {

            return;
        }

        ActionEntry entry = entries.remove(index);

        entries.add(target, entry);

        StafftoolsClient.saveConfig();

        // rebuild from tick(): touching the widget list inside a click
        // handler breaks the iteration over the children
        needsRebuild = true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        float step = ROW_H + ROW_GAP;

        scroll += verticalAmount > 0 ? -step : step;

        return true;
    }

    /**
     * Same spring as the chat popup: slightly underdamped, so every
     * notch settles with a small overshoot, plus a rubber band at the
     * ends of the list.
     */
    private void updateScroll(float delta) {

        float step = Ui.clamp01(delta) * 0.05f;
        float max = maxScroll();

        float target = Math.clamp(
                scroll,
                -SCROLL_RUBBER,
                max + SCROLL_RUBBER
        );

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scrollPos) * SCROLL_STIFF
                            - scrollVel * SCROLL_DAMP;

            float over = scrollPos < 0f
                    ? scrollPos
                    : scrollPos > max
                    ? scrollPos - max
                    : 0f;

            if (over != 0f) {
                accel -= over * EDGE_STIFF;
                accel -= scrollVel * EDGE_DAMP;
            }

            scrollVel += accel * s;
            scrollPos += scrollVel * s;
        }

        if (Math.abs(scrollPos - target) < 0.05f &&
                Math.abs(scrollVel) < 0.05f) {

            scrollPos = target;
            scrollVel = 0f;
        }
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawTitle(context, Key.BTN_PLAYER_ACTIONS, alpha);

        drawHint(context, Key.MSG_ACTIONS_HINT, 30, alpha);

        Ui.drawPanel(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_ACTIONS),
                    width / 2,
                    LIST_TOP + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        long now = System.currentTimeMillis();

        updateScroll(delta);

        // rows must never leave the frame: clip to the list viewport
        // (the entrance animation shifts the whole form by up to 14px)
        int shift = (int) ((1f - alpha) * 14f);

        context.enableScissor(
                rowLeft + ROW_PAD,
                LIST_TOP,
                rowLeft + rowWidth - ROW_PAD,
                listBottom
        );

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            int y = Math.round(row.baseY - scrollPos);

            // partially visible rows still draw (clipped), but their
            // buttons only show while the whole button fits in view
            boolean drawable =
                    y + ROW_H > LIST_TOP && y < listBottom;

            int buttonY = y + (ROW_H - BTN_H) / 2;

            positionRowButtons(
                    row,
                    buttonY,
                    drawable &&
                            buttonY >= LIST_TOP &&
                            buttonY + BTN_H <= listBottom
            );

            if (!drawable) {
                continue;
            }

            float rowAlpha = Ui.clamp01(
                    (now - openTime - i * 22L) / 180f
            );

            if (rowAlpha <= 0.01f) {
                continue;
            }

            drawRow(
                    context,
                    row,
                    y,
                    mouseX,
                    mouseY,
                    alpha * rowAlpha
            );
        }

        context.disableScissor();

        drawScrollMasks(context, alpha);
    }

    private void drawRow(
            DrawContext context,
            Row row,
            int y,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        ActionEntry entry = row.entry;

        boolean hovered =
                mouseX >= rowLeft + ROW_PAD &&
                        mouseX <= rowLeft + rowWidth - ROW_PAD &&
                        mouseY >= y &&
                        mouseY <= y + ROW_H - 2;

        int bg = hovered
                ? Ui.argb(0x403F8AE0, alpha)
                : Ui.argb(Ui.PANEL_BG_SOFT, alpha * 0.85f);

        Ui.drawRoundRect(
                context,
                rowLeft + ROW_PAD,
                y,
                rowWidth - ROW_PAD * 2,
                ROW_H - 2,
                3,
                bg
        );

        Ui.drawRoundBorder(
                context,
                rowLeft + ROW_PAD,
                y,
                rowWidth - ROW_PAD * 2,
                ROW_H - 2,
                3,
                Ui.argb(
                        hovered ? Ui.ACCENT : 0xFF2C3B55,
                        alpha * (hovered ? 1f : 0.55f)
                )
        );

        // status dot: on / off
        context.fill(
                rowLeft + 5,
                y + 3,
                rowLeft + 7,
                y + 5,
                Ui.argb(
                        entry.isEnabled()
                                ? Ui.SUCCESS
                                : Ui.TEXT_MUTED,
                        alpha
                )
        );

        // leave a gap before the button column
        int textW = Math.max(50, rowWidth - ROW_PAD * 2 - 153);

        String icon =
                entry.getIcon() == null
                        ? ""
                        : entry.getIcon();

        String name = icon + " " + entry.getLabel();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(name, textW)
                ),
                rowLeft + ROW_PAD + 11,
                y + 4,
                Ui.argb(
                        entry.isEnabled() ? Ui.TEXT : Ui.TEXT_MUTED,
                        alpha
                )
        );

        String command =
                entry.isConfirmationRequired()
                        ? "⚠ " + entry.getCommand()
                        : entry.getCommand();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(command, textW)
                ),
                rowLeft + ROW_PAD + 11,
                y + 15,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );
    }

    /** Soft fades at the list edges, kept inside the panel border. */
    private void drawScrollMasks(
            DrawContext context,
            float alpha
    ) {

        float top = Ui.clamp01(scrollPos / FADE_H);

        float bottom = Ui.clamp01(
                (maxScroll() - scrollPos) / FADE_H
        );

        if (top > 0.01f) {

            context.fillGradient(
                    rowLeft + ROW_PAD,
                    LIST_TOP,
                    rowLeft + rowWidth - ROW_PAD,
                    LIST_TOP + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    rowLeft + ROW_PAD,
                    listBottom - FADE_H,
                    rowLeft + rowWidth - ROW_PAD,
                    listBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }

    @Override
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawScrollbar(context, alpha);
    }

    /** Slim scrollbar drawn above the widgets. */
    private void drawScrollbar(
            DrawContext context,
            float alpha
    ) {

        int max = maxScroll();

        if (max <= 0) {
            return;
        }

        int trackH = listBottom - LIST_TOP;

        int thumbH = Math.max(
                16,
                (int) (trackH * trackH /
                        (float) (trackH + max))
        );

        int travel = Math.max(1, trackH - thumbH);

        int x = rowLeft + rowWidth - 5;
        int y = LIST_TOP +
                (int) (travel * (scrollPos / (float) max));

        Ui.drawRoundRect(
                context,
                x,
                y,
                3,
                thumbH,
                2,
                Ui.argb(
                        Ui.mix(Ui.ACCENT_2, Ui.TEXT, 0.12f),
                        alpha * 0.7f
                )
        );
    }

    private static final class Row {

        private final ActionEntry entry;
        private final int index;
        private final int baseY;
        private StButton btnUp;
        private StButton btnDown;
        private StButton btnEdit;
        private StButton btnDelete;

        private Row(ActionEntry entry, int index, int baseY) {
            this.entry = entry;
            this.index = index;
            this.baseY = baseY;
        }
    }
}
