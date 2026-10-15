package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Macro list, styled like the rest of the mod UI: framed panel, themed
 * rows with entrance stagger, hover highlight and real StButtons.
 * The list scrolls smoothly — buttons are repositioned, never rebuilt,
 * so their animations never restart.
 */
public class MacroListScreen extends StScreen {

    private static final int ROW_H = 44;
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

        // NOTE: openTime is deliberately NOT reset here — the entrance
        // animation must play once per screen instance (entering the
        // list, or coming back from the editor), never on a rebuild.

        rows.clear();
        clearChildren();

        List<Macro> macros =
                StafftoolsClient.getMacroManager().getMacros();

        rowWidth = Math.min(280, formWidth());
        rowLeft = (width - rowWidth) / 2;

        listBottom = height - BOTTOM_PAD;

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < macros.size(); i++) {

            Row row =
                    new Row(macros.get(i), LIST_TOP + i * step);

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
                Key.BTN_CREATE_MACRO,
                b -> client.setScreen(
                        new MacroEditorScreen(this, null)
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
                right - 38,
                y,
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
                y,
                48,
                BTN_H,
                Key.BTN_EDIT,
                b -> client.setScreen(
                        new MacroEditorScreen(this, row.macro)
                )
        );

        row.btnToggle = button(
                right - 38 - 3 - 48 - 3 - 42,
                y,
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

        drawTitle(context, Key.BTN_MACROS, alpha);

        drawHint(context, Key.MSG_SELECT_MACRO, 30, alpha);

        // frame around the whole list area
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
                    Lang.text(Key.MSG_NO_MACROS),
                    width / 2,
                    LIST_TOP + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        long now = System.currentTimeMillis();

        updateScroll(delta);

        // rows (and their buttons) must never leave the frame:
        // clip everything to the list viewport
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

    /** The entrance animation slides the whole form down by up to 14px. */
    private int entranceShift(float alpha) {
        return (int) ((1f - alpha) * 14f);
    }

    private void drawRow(
            DrawContext context,
            Row row,
            int y,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        Macro macro = row.macro;

        boolean hovered =
                mouseX >= rowLeft + ROW_PAD &&
                        mouseX <= rowLeft + rowWidth - ROW_PAD &&
                        mouseY >= y &&
                        mouseY <= y + ROW_H - 2;

        int bg = hovered
                ? Ui.argb(0x403F8AE0, alpha)
                : Ui.argb(Ui.PANEL_BG_SOFT, alpha * 0.85f);

        int rx = rowLeft + ROW_PAD;
        int rw = rowWidth - ROW_PAD * 2;

        Ui.drawRoundRect(
                context,
                rx,
                y,
                rw,
                ROW_H - 2,
                3,
                bg
        );

        Ui.drawRoundBorder(
                context,
                rx,
                y,
                rw,
                ROW_H - 2,
                3,
                Ui.argb(
                        hovered ? Ui.ACCENT : 0xFF2C3B55,
                        alpha * (hovered ? 1f : 0.55f)
                )
        );

        // status dot: on / off
        context.fill(
                rx + 6,
                y + 4,
                rx + 8,
                y + 6,
                Ui.argb(
                        macro.isEnabled()
                                ? Ui.SUCCESS
                                : Ui.TEXT_MUTED,
                        alpha
                )
        );

        // leave a gap before the button column
        int textW = Math.max(50, rw - 156);

        String name = "\uD83D\uDCCC " + macro.getName();

        name = textRenderer.trimToWidth(name, textW);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                rx + 11,
                y + 2,
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
                    rx + 13 +
                            textRenderer.getWidth(name),
                    y + 2,
                    Ui.argb(Ui.WARNING, alpha)
            );
        }

        String description =
                macro.getDescription();

        if (description == null ||
                description.isEmpty()) {

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
                    rx + 11,
                    y + 14 + i * 9,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
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

                word = word.substring(
                        0,
                        word.length() - 1
                );
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0
                && lines.size() < maxLines) {

            lines.add(current.toString());
        }

        // ellipsis when content was cut off
        int totalWords = text.split(" ").length;

        boolean truncated =
                lines.size() == maxLines &&
                        totalWords > String.join(
                                " ", lines
                        ).split(" ").length;

        if (truncated && !lines.isEmpty()) {

            int lastIdx = lines.size() - 1;

            String last = lines.get(lastIdx);

            last = tr.trimToWidth(
                    last + "...",
                    maxWidth
            );

            lines.set(lastIdx, last);
        }

        return lines;
    }

    private static final class Row {

        private final Macro macro;
        private final int baseY;
        private StButton btnToggle;
        private StButton btnEdit;
        private StButton btnDelete;

        private Row(Macro macro, int baseY) {
            this.macro = macro;
            this.baseY = baseY;
        }
    }
}
