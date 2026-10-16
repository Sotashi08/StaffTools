package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Punishment details: every recorded fact about one entry, laid out as a
 * two-column stats table — label on the left, value on the right, long
 * values wrapped over as many lines as they need.
 *
 * Rows are pre-wrapped once and their heights are stored with them, so
 * the drawing pass and the height calculation can never drift apart.
 * Content taller than the visible area scrolls inside the panel.
 */
public class PunishmentDetailScreen extends StScreen {

    private static final int LINE_H = 11;
    private static final int FIELD_GAP = 8;

    private static final int PAD = 10;
    private static final int LABEL_MAX = 104;

    /** Width kept free on the right for the scroll bar. */
    private static final int SCROLLBAR_RESERVE = 8;

    private static final int CONTENT_TOP = 58;
    private static final int FOOTER_H = 30;
    private static final int SCROLL_STEP = 22;

    private final Screen parent;
    private final PunishmentRecord record;

    /** Server the entry belongs to; null when the history is unfiltered. */
    private final String serverKey;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /** Laid-out rows for the current screen width. */
    private final List<Row> rows = new ArrayList<>();

    private int contentHeight;
    private int labelW;
    private int panelBottom;
    private int scroll;

    public PunishmentDetailScreen(
            Screen parent,
            PunishmentRecord record
    ) {

        this(parent, record, null);
    }

    /** @param serverKey of the view that opened this entry, if any. */
    public PunishmentDetailScreen(
            Screen parent,
            PunishmentRecord record,
            String serverKey
    ) {

        super(Lang.text(Key.DETAIL_TITLE));

        this.parent = parent;
        this.record = record;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {

        resetSlots();

        clearChildren();

        layoutRows();

        panelBottom = Math.min(
                CONTENT_TOP + contentHeight + FOOTER_H,
                height - 8
        );

        // a resize may have folded the last rows out of view
        scroll = Math.clamp(
                scroll,
                0,
                visibleSlack()
        );

        button(
                width / 2 - 60,
                panelBottom - 26,
                120,
                20,
                Key.BTN_BACK,
                b -> client.setScreen(
                        serverKey == null
                                ? parent
                                : new ActionHistoryScreen(
                                        this,
                                        serverKey
                                )
                )
        );
    }

    /** Builds every row, pre-wrapped to the current width. */
    private void layoutRows() {

        rows.clear();

        int inner = formWidth() - PAD * 2;

        // label column: as wide as the widest label, but never more than
        // a third of the panel, so values always keep a usable column
        labelW = Math.max(
                Math.min(LABEL_MAX, inner / 3),
                Math.min(LABEL_MAX, inner - 48)
        );

        // a few pixels of slack keep the scroll bar off the last glyph
        int valueW = inner - labelW - SCROLLBAR_RESERVE;

        addRow(Key.DETAIL_PLAYER, player(), valueW);
        addRow(Key.DETAIL_TIME, time(), valueW);
        addRow(Key.DETAIL_MODERATOR, moderator(), valueW);
        addRow(Key.DETAIL_TYPE, type(), valueW);

        if (hasSource()) {

            addRow(
                    sourceLabelKey(),
                    source(),
                    valueW
            );
        }

        addRow(Key.DETAIL_COMMAND, command(), valueW);
        addRow(Key.DETAIL_STATUS, status(), valueW);

        if (serverReply() != null) {

            addRow(
                    Key.DETAIL_SERVER_REPLY,
                    serverReply(),
                    valueW
            );
        }

        contentHeight = 0;

        for (Row row : rows) {
            contentHeight += row.height;
        }
    }

    private void addRow(
            String labelKey,
            String value,
            int valueW
    ) {

        String shown = value == null
                ? Lang.t(Key.DETAIL_NONE)
                : value;

        rows.add(new Row(
                Lang.t(labelKey),
                wrap(shown, valueW)
        ));
    }

    /** Only macros and actions have a source row. */
    private boolean hasSource() {
        return sourceLabelKey() != null;
    }

    private String player() {
        return record.getPlayerName();
    }
    private String time() {
        return dateFormat.format(
                new Date(record.getTimestamp())
        );
    }

    private String moderator() {
        return record.getModeratorOrNull();
    }

    /**
     * Source label: "Макрос" / "Действие" depending on what sent it.
     * A hand-typed command has no source row at all.
     */
    private String sourceLabelKey() {

        return switch (record.getSourceKind()) {

            case MACRO -> Key.DETAIL_MACRO;
            case ACTION -> Key.DETAIL_ACTION;
            case MANUAL -> null;
        };
    }

    private String source() {

        String name = record.getSourceName();

        return name == null || name.isBlank()
                ? null
                : name;
    }

    private String type() {
        return record.getType().getIcon() +
                " " +
                record.getType().getLabel();
    }

    private String command() {
        return record.getFullCommand();
    }

    private String status() {

        return switch (record.getEffectiveStatus()) {

            case DONE -> Lang.t(Key.PUN_STATUS_DONE);
            case FAILED -> Lang.t(Key.PUN_STATUS_FAILED);
            case NO_RESPONSE ->
                    Lang.t(Key.PUN_STATUS_NO_RESPONSE);
            case PENDING -> Lang.t(Key.PUN_STATUS_PENDING);
        };
    }

    private String serverReply() {

        String reply = record.getResponse();

        return reply == null || reply.isBlank()
                ? null
                : reply;
    }

    /**
     * Greedy word wrap. A single word too long for the value column is
     * split mid-word, so a long command can never spill sideways.
     */
    private List<String> wrap(String text, int maxWidth) {

        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        List<String> tokens = new ArrayList<>();

        for (String word : text.split("\\s+")) {

            if (word.isEmpty()) {
                continue;
            }

            if (textRenderer.getWidth(word) <= maxWidth) {

                tokens.add(word);
                continue;
            }

            // split an over-long word into chunks that each fit
            StringBuilder chunk = new StringBuilder();

            for (char c : word.toCharArray()) {

                if (chunk.length() > 0 &&
                        textRenderer.getWidth(
                                chunk.toString() + c) > maxWidth) {

                    tokens.add(chunk.toString());
                    chunk.setLength(0);
                }

                chunk.append(c);
            }

            if (chunk.length() > 0) {
                tokens.add(chunk.toString());
            }
        }

        StringBuilder line = new StringBuilder();

        for (String token : tokens) {

            if (line.length() == 0) {

                line.append(token);
                continue;
            }

            if (textRenderer.getWidth(
                    line + " " + token) > maxWidth) {

                lines.add(line.toString());
                line.setLength(0);
                line.append(token);
                continue;
            }

            line.append(' ').append(token);
        }

        if (line.length() > 0 || lines.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        scroll += verticalAmount > 0
                ? -SCROLL_STEP
                : SCROLL_STEP;

        scroll = Math.clamp(
                scroll,
                0,
                visibleSlack()
        );

        return true;
    }

    /** How much content overflows the visible area. */
    private int visibleSlack() {
        return Math.max(0, contentHeight - visibleHeight());
    }

    /** Height of the scrollable band between the title and the button. */
    private int visibleHeight() {

        return Math.max(
                0,
                panelBottom - FOOTER_H - CONTENT_TOP
        );
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        Ui.drawPanel(
                context,
                formLeft(),
                34,
                formWidth(),
                panelBottom - 34,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                formLeft(),
                34,
                formWidth(),
                panelBottom - 34,
                alpha
        );

        drawTitle(context, Key.DETAIL_TITLE, alpha);

        int left = formLeft();
        int valueX = left + PAD + labelW;

        int bottom = panelBottom - FOOTER_H;

        // clip the band the rows may paint into
        if (bottom > CONTENT_TOP) {

            context.enableScissor(
                    left,
                    CONTENT_TOP,
                    left + formWidth(),
                    bottom
            );
        }

        int y = CONTENT_TOP - scroll;

        for (Row row : rows) {

            if (y + row.height < CONTENT_TOP) {
                y += row.height;
                continue;
            }

            if (y > bottom) {
                break;
            }

            // label, aligned with the first value line
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(row.label),
                    left + PAD,
                    y + 1,
                    Ui.argb(Ui.TEXT_DIM, alpha)
            );

            int lineY = y;

            for (String line : row.lines) {

                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal(line),
                        valueX,
                        lineY,
                        Ui.argb(Ui.TEXT, alpha)
                );

                lineY += LINE_H;
            }

            y += row.height;
        }

        context.disableScissor();

        drawScrollbar(context, alpha);
    }

    /** Slim scroll track on the right, only when there is something hidden. */
    private void drawScrollbar(
            DrawContext context,
            float alpha
    ) {

        int slack = visibleSlack();

        if (slack <= 0) {
            return;
        }

        int trackTop = CONTENT_TOP;
        int trackH = Math.max(1, visibleHeight());

        int thumbH = Math.max(
                16,
                (int) (trackH * trackH
                        / (float) (trackH + slack))
        );

        int thumbY = trackTop + Math.round(
                (trackH - thumbH)
                        * (scroll / (float) slack)
        );

        Ui.drawRoundRect(
                context,
                formLeft() + formWidth() - PAD - 2,
                thumbY,
                2,
                thumbH,
                1,
                Ui.argb(
                        Ui.mix(Ui.ACCENT, Ui.TEXT, 0.15f),
                        alpha * 0.5f
                )
        );
    }

    /** One laid-out row: its label, its pre-wrapped lines and its height. */
    private final class Row {

        private final String label;
        private final List<String> lines;
        private final int height;

        private Row(String label, List<String> lines) {

            this.label = label;
            this.lines = lines;

            this.height = Math.max(1, lines.size()) * LINE_H
                    + FIELD_GAP;
        }
    }
}
