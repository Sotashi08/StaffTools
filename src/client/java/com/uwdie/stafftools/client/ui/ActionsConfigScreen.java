package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-action list: a framed viewport of cards, each with a drag
 * handle, a status dot, the label, the command and its own buttons.
 * Rows can be reordered by dragging.
 */
public class ActionsConfigScreen extends StListScreen {

    private static final int ROW_H = 30;
    private static final int ROW_GAP = 3;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int BTN_H = 18;

    private final Screen parent;

    private boolean needsRebuild;

    /** Drag-to-reorder state for the rows. */
    private final DragReorder drag = new DragReorder();

    private final List<Row> rows = new ArrayList<>();

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

        configureShell();

        List<ActionEntry> entries =
                StafftoolsClient.getConfig().getActionEntries();

        rowWidth = Math.min(440, contentWidth());
        rowLeft = contentLeft();

        listTop = contentTop() + 40;
        listBottom = contentBottom() - 40;

        for (int i = 0; i < entries.size(); i++) {

            Row row = new Row(entries.get(i), i);

            rows.add(row);
            addRowButtons(row);
        }

        resetScroll();

        int half = (rowWidth - 10) / 2;

        button(
                rowLeft,
                contentBottom() - 26,
                half,
                Design.H_BUTTON_LG,
                Key.BTN_ADD_ACTION,
                b -> client.setScreen(
                        new ActionEntryEditScreen(this, null, true)
                )
        );

        button(
                rowLeft + half + 10,
                contentBottom() - 26,
                half,
                Design.H_BUTTON_LG,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected int rowStep() {
        return ROW_H + ROW_GAP;
    }

    @Override
    protected int rowHeight() {
        return ROW_H;
    }

    private void addRowButtons(Row row) {

        int right = rowLeft + rowWidth - ROW_PAD - 5;

        row.btnDelete = button(
                right - 36,
                0,
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
                0,
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
                0,
                20,
                "\u25BC",
                b -> move(row.index, 1)
        );

        row.btnUp = glyphButton(
                right - 36 - 3 - 48 - 3 - 20 - 3 - 20,
                0,
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
                StafftoolsClient.getConfig().getActionEntries();

        int target = index + delta;

        if (target < 0 || target >= entries.size()) {
            return;
        }

        ActionEntry entry = entries.remove(index);

        entries.add(target, entry);

        StafftoolsClient.saveConfig();

        // rebuild from tick(): touching the widget list inside a click
        // handler breaks the iteration over the children
        needsRebuild = true;
    }

    // ------------------------------------------------------------------
    // drag to reorder
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        if (click.button() == 0 &&
                !drag.isDragging() &&
                !isOverWidget(click.x(), click.y()) &&
                insideList(click.x(), click.y())) {

            int index = rowIndexAt(click.y());

            if (index >= 0) {
                drag.press(index, click.y());
            }
        }

        return super.mouseClicked(click, doubled);
    }

    /** True when a row button sits under the cursor. */
    private boolean isOverWidget(double mouseX, double mouseY) {

        for (var child : children()) {

            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(
            Click click,
            double offsetX,
            double offsetY
    ) {

        if (drag.drag(
                click.y(),
                listTop,
                rowStep(),
                rows.size(),
                scrollPos
        )) {

            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {

        int to = drag.targetIndex();

        int from = drag.release();

        if (from >= 0) {
            reorder(from, to);
        }

        return super.mouseReleased(click);
    }

    /** Moves an action to a new position and persists the order. */
    private void reorder(int from, int to) {

        List<ActionEntry> entries =
                StafftoolsClient.getConfig().getActionEntries();

        if (from < 0 || from >= entries.size() ||
                to < 0 || to >= entries.size() ||
                from == to) {

            return;
        }

        ActionEntry moved = entries.remove(from);

        entries.add(to, moved);

        StafftoolsClient.saveConfig();

        needsRebuild = true;
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

        Ui.drawSectionTitle(
                context,
                textRenderer,
                contentLeft(),
                contentTop(),
                "\uD83D\uDCCB",
                Lang.t(Key.BTN_PLAYER_ACTIONS),
                Lang.t(Key.MSG_ACTIONS_HINT),
                alpha
        );

        renderList(context, mouseX, mouseY, delta, alpha);

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_ACTIONS),
                    rowLeft + rowWidth / 2,
                    listTop + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    @Override
    protected void renderRows(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        int dragFrom = drag.isDragging() ? drag.fromIndex() : -1;
        int dragTo = drag.isDragging() ? drag.targetIndex() : -1;

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            int y = rowY(i);

            boolean drawable = rowVisible(i);

            int buttonY = y + (ROW_H - BTN_H) / 2;

            positionRowButtons(
                    row,
                    buttonY,
                    drawable &&
                            buttonY >= listTop &&
                            buttonY + BTN_H <= listBottom &&
                            !drag.isDragging()
            );

            if (!drawable) {
                continue;
            }

            float rowAlpha = rowAlpha(i);

            if (rowAlpha <= 0.01f) {
                continue;
            }

            drawRow(
                    context,
                    row,
                    y,
                    mouseX,
                    mouseY,
                    alpha * rowAlpha,
                    i == dragFrom
            );
        }

        drawDropIndicator(context, dragTo, alpha);
    }

    /** Accent line showing where the dragged row will land. */
    private void drawDropIndicator(
            DrawContext context,
            int target,
            float alpha
    ) {

        if (target < 0) {
            return;
        }

        int lineY = Math.clamp(
                rowY(target),
                listTop,
                listBottom - 2
        );

        Ui.drawGradientH(
                context,
                rowLeft + ROW_PAD,
                lineY,
                rowWidth - ROW_PAD * 2,
                2,
                Ui.ACCENT,
                Ui.ACCENT_2,
                alpha
        );
    }

    private void drawRow(
            DrawContext context,
            Row row,
            int y,
            int mouseX,
            int mouseY,
            float alpha,
            boolean dragging
    ) {

        ActionEntry entry = row.entry;

        int rx = rowLeft + ROW_PAD;
        int rw = rowWidth - ROW_PAD * 2;
        int rh = ROW_H - 2;

        boolean hovered =
                mouseX >= rx && mouseX <= rx + rw &&
                        mouseY >= y && mouseY <= y + rh;

        if (dragging) {

            Ui.drawCardAccent(
                    context, rx, y, rw, rh,
                    Ui.ACCENT, alpha
            );

        } else if (hovered) {

            Ui.drawRoundRect(
                    context, rx, y, rw, rh, Design.RADIUS_MD,
                    Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.16f), alpha)
            );

            Ui.drawRoundBorder(
                    context, rx, y, rw, rh, Design.RADIUS_MD,
                    Ui.argb(Ui.ACCENT, alpha * 0.9f)
            );

        } else {

            Ui.drawCard(context, rx, y, rw, rh, alpha);
        }

        // drag handle: three dots on the left edge
        for (int d = 0; d < 3; d++) {

            context.fill(
                    rx + 4,
                    y + 6 + d * 4,
                    rx + 6,
                    y + 8 + d * 4,
                    Ui.argb(
                            dragging ? Ui.ACCENT : Ui.TEXT_MUTED,
                            alpha * (dragging ? 1f : 0.55f)
                    )
            );
        }

        // status dot: on / off
        context.fill(
                rx + 10,
                y + 3,
                rx + 12,
                y + 5,
                Ui.argb(
                        entry.isEnabled()
                                ? Ui.SUCCESS
                                : Ui.TEXT_MUTED,
                        alpha
                )
        );

        // leave a gap before the button column
        int textW = Math.max(50, rw - 160);

        String icon = entry.getIcon() == null
                ? ""
                : entry.getIcon();

        String name = icon + " " + entry.getLabel();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(name, textW)
                ),
                rx + 15,
                y + 4,
                Ui.argb(
                        entry.isEnabled() ? Ui.TEXT : Ui.TEXT_MUTED,
                        alpha
                )
        );

        String command = entry.isConfirmationRequired()
                ? "⚠ " + entry.getCommand()
                : entry.getCommand();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(command, textW)
                ),
                rx + 15,
                y + 15,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );
    }

    private static final class Row {

        private final ActionEntry entry;
        private final int index;
        private StButton btnUp;
        private StButton btnDown;
        private StButton btnEdit;
        private StButton btnDelete;

        private Row(ActionEntry entry, int index) {
            this.entry = entry;
            this.index = index;
        }
    }
}
