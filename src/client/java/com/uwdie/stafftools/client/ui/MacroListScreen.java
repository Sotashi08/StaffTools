package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Macro list: a framed viewport of cards, each with a drag handle, a
 * status dot, the name, a wrapped description and its own action
 * buttons. Rows can be reordered by dragging.
 */
public class MacroListScreen extends StListScreen {

    private static final int ROW_H = 44;
    private static final int ROW_GAP = 3;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int BTN_H = 18;

    private final Screen parent;

    private boolean needsRebuild;

    /** Drag-to-reorder state for the rows. */
    private final DragReorder drag = new DragReorder();

    private final List<Row> rows = new ArrayList<>();

    public MacroListScreen(Screen parent) {
        super(Lang.text(Key.BTN_MACROS));

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

        List<Macro> macros =
                StafftoolsClient.getMacroManager().getMacros();

        rowWidth = Math.min(420, contentWidth());
        rowLeft = contentLeft();

        listTop = contentTop() + 40;
        listBottom = contentBottom() - 40;

        for (Macro macro : macros) {

            Row row = new Row(macro);

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
                Key.BTN_CREATE_MACRO,
                b -> client.setScreen(
                        new MacroEditorScreen(this, null)
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
                right - 38,
                0,
                38,
                BTN_H,
                Key.BTN_DELETE,
                b -> {
                    StafftoolsClient.getMacroManager()
                            .remove(row.macro.getId());

                    // rebuild from tick(): touching the widget list
                    // inside a click handler breaks the iteration
                    needsRebuild = true;
                },
                Ui.DANGER
        );

        row.btnEdit = button(
                right - 38 - 3 - 48,
                0,
                48,
                BTN_H,
                Key.BTN_EDIT,
                b -> client.setScreen(
                        new MacroEditorScreen(this, row.macro)
                )
        );

        row.btnToggle = button(
                right - 38 - 3 - 48 - 3 - 42,
                0,
                42,
                BTN_H,
                row.macro.isEnabled()
                        ? Key.BTN_ON
                        : Key.BTN_OFF,
                b -> {
                    row.macro.setEnabled(
                            !row.macro.isEnabled()
                    );
                    StafftoolsClient.saveConfig();
                    b.setMessage(
                            Lang.text(
                                    row.macro.isEnabled()
                                            ? Key.BTN_ON
                                            : Key.BTN_OFF
                            )
                    );
                }
        );
    }

    /** Buttons follow their row while scrolling; hidden when off-screen. */
    private void positionRowButtons(Row row, int buttonY, boolean visible) {

        row.btnToggle.setY(buttonY);
        row.btnEdit.setY(buttonY);
        row.btnDelete.setY(buttonY);

        row.btnToggle.setVisible(visible);
        row.btnEdit.setVisible(visible);
        row.btnDelete.setVisible(visible);
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

    /** Moves a macro to a new position and persists the order. */
    private void reorder(int from, int to) {

        // getMacros() returns an immutable snapshot: the reorder has to
        // work on a mutable copy, otherwise remove() throws and the drag
        // silently does nothing
        List<Macro> macros = new java.util.ArrayList<>(
                StafftoolsClient.getMacroManager().getMacros()
        );

        if (from < 0 || from >= macros.size() ||
                to < 0 || to >= macros.size() ||
                from == to) {

            return;
        }

        Macro moved = macros.remove(from);

        macros.add(to, moved);

        StafftoolsClient.getMacroManager()
                .reorder(macros);

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
                "\u26A1",
                Lang.t(Key.BTN_MACROS),
                Lang.t(Key.MSG_SELECT_MACRO),
                alpha
        );

        renderList(context, mouseX, mouseY, delta, alpha);

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_MACROS),
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

        Macro macro = row.macro;

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
                    y + 8 + d * 4,
                    rx + 6,
                    y + 10 + d * 4,
                    Ui.argb(
                            dragging ? Ui.ACCENT : Ui.TEXT_MUTED,
                            alpha * (dragging ? 1f : 0.55f)
                    )
            );
        }

        // status dot: on / off
        context.fill(
                rx + 10,
                y + 4,
                rx + 12,
                y + 6,
                Ui.argb(
                        macro.isEnabled()
                                ? Ui.SUCCESS
                                : Ui.TEXT_MUTED,
                        alpha
                )
        );

        // leave a gap before the button column
        int textW = Math.max(50, rw - 164);

        String name = textRenderer.trimToWidth(
                macro.getName(),
                textW
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                rx + 15,
                y + 3,
                Ui.argb(
                        macro.isEnabled() ? Ui.TEXT : Ui.TEXT_MUTED,
                        alpha
                )
        );

        // "confirm" badge, only when it still fits next to the name
        if ((macro.isDangerous() ||
                macro.isConfirmationRequired()) &&
                textRenderer.getWidth(name) + 28 < textW) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("⚠"),
                    rx + 17 + textRenderer.getWidth(name),
                    y + 3,
                    Ui.argb(Ui.WARNING, alpha)
            );
        }

        String description = macro.getDescription();

        if (description == null || description.isEmpty()) {
            description = Lang.t(Key.MSG_NO_DESCRIPTION);
        }

        List<String> lines = wrapText(
                textRenderer,
                description,
                textW,
                3
        );

        for (int i = 0; i < lines.size(); i++) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(lines.get(i)),
                    rx + 15,
                    y + 15 + i * 9,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    /** Word-aware wrap; hard-trims overlong words, caps line count. */
    private static List<String> wrapText(
            net.minecraft.client.font.TextRenderer tr,
            String text,
            int maxWidth,
            int maxLines
    ) {

        List<String> lines = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {

            if (lines.size() >= maxLines) {
                break;
            }

            String candidate =
                    current.length() == 0
                            ? word
                            : current + " " + word;

            if (tr.getWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }

            if (lines.size() >= maxLines) {
                break;
            }

            while (tr.getWidth(word) > maxWidth
                    && word.length() > 1) {

                word = word.substring(0, word.length() - 1);
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }

        // ellipsis when content was cut off
        int totalWords = text.split(" ").length;

        boolean truncated =
                lines.size() == maxLines &&
                        totalWords > String.join(" ", lines)
                                .split(" ").length;

        if (truncated && !lines.isEmpty()) {

            int lastIdx = lines.size() - 1;

            lines.set(
                    lastIdx,
                    tr.trimToWidth(
                            lines.get(lastIdx) + "...",
                            maxWidth
                    )
            );
        }

        return lines;
    }

    private static final class Row {

        private final Macro macro;
        private StButton btnToggle;
        private StButton btnEdit;
        private StButton btnDelete;

        private Row(Macro macro) {
            this.macro = macro;
        }
    }
}
