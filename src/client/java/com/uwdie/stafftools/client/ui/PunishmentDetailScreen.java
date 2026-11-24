package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import com.uwdie.stafftools.client.punishment.PunishmentStatus;
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

    private int CONTENT_TOP;
    private static final int FOOTER_H = 30;
    private static final int MANUAL_FOOTER_H = 82;
    private static final int SCROLL_STEP = 22;

    private final Screen parent;
    private final PunishmentRecord record;

    /** Server the entry belongs to; null when the history is unfiltered. */
    private final String serverKey;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /** Whether the record is pending and can be manually resolved. */
    private boolean canManualResolve() {
        return record.getEffectiveStatus() == PunishmentStatus.PENDING ||
                record.getEffectiveStatus() == PunishmentStatus.NO_RESPONSE;
    }

    /** Laid-out rows for the current screen width. */
    private final List<Row> rows = new ArrayList<>();

    private int contentHeight;
    private int labelW;
    private int panelX;
    private int panelTop;
    private int panelWidth;
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

        configureShell();

        clearChildren();

        panelWidth = Math.min(contentWidth(), 560);
        panelX = contentLeft()
                + Math.max(0, (contentWidth() - panelWidth) / 2);
        panelTop = contentTop() + 24;
        CONTENT_TOP = panelTop + 38;

        layoutRows();

        panelBottom = Math.min(
                Math.max(
                        CONTENT_TOP + contentHeight
                                + footerHeight() + PAD,
                        panelTop + 120
                ),
                contentBottom()
        );

        // a resize may have folded the last rows out of view
        scroll = Math.clamp(
                scroll,
                0,
                visibleSlack()
        );

        int backButtonWidth = Math.max(1, Math.min(120, panelWidth - 24));
        button(
                panelX + (panelWidth - backButtonWidth) / 2,
                panelBottom - 26,
                backButtonWidth,
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

        // Manual resolution buttons for pending/no-response records
        if (canManualResolve()) {
            int btnY = panelBottom - 52;
            int btnW = Math.min(120, Math.max(1, (panelWidth - 20) / 2));
            int btnH = 20;
            int centerX = panelX + panelWidth / 2;

            button(
                    centerX - btnW - 4,
                    btnY,
                    btnW,
                    btnH,
                    Key.BTN_MARK_DONE,
                    b -> {
                        record.setStatus(PunishmentStatus.DONE);
                        record.setResponse(Lang.t(Key.TOAST_MARKED_DONE));
                        PunishmentHistory.get().save();
                        client.setScreen(new PunishmentDetailScreen(parent, record, serverKey));
                    },
                    Ui.SUCCESS
            );

            button(
                    centerX + 4,
                    btnY,
                    btnW,
                    btnH,
                    Key.BTN_MARK_FAILED,
                    b -> {
                        record.setStatus(PunishmentStatus.FAILED);
                        record.setResponse(Lang.t(Key.TOAST_MARKED_FAILED));
                        PunishmentHistory.get().save();
                        client.setScreen(new PunishmentDetailScreen(parent, record, serverKey));
                    },
                    Ui.DANGER
            );

            button(
                    centerX - btnW / 2,
                    btnY - btnH - 4,
                    btnW,
                    btnH,
                    Key.BTN_MARK_PENDING,
                    b -> {
                        record.setStatus(PunishmentStatus.PENDING);
                        record.setResponse(null);
                        PunishmentHistory.get().save();
                        client.setScreen(new PunishmentDetailScreen(parent, record, serverKey));
                    },
                    Ui.WARNING
            );
        }
    }

    /** Builds every row, pre-wrapped to the current width. */
    private void layoutRows() {

        rows.clear();

        int inner = panelWidth - PAD * 2;

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

        if (mouseX < panelX || mouseX > panelX + panelWidth ||
                mouseY < CONTENT_TOP ||
                mouseY > panelBottom - footerHeight()) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount
            );
        }

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

    private int footerHeight() {
        return canManualResolve()
                ? MANUAL_FOOTER_H
                : FOOTER_H;
    }

    /** Height of the scrollable band between the title and the button. */
    private int visibleHeight() {

        return Math.max(
                0,
                panelBottom - footerHeight() - CONTENT_TOP
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
                panelX,
                panelTop,
                panelWidth,
                panelBottom - panelTop,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                panelX,
                panelTop,
                panelWidth,
                panelBottom - panelTop,
                alpha
        );

        Ui.drawSectionTitle(
                context,
                textRenderer,
                contentLeft(),
                contentTop(),
                "\uD83D\uDCC4",
                Lang.t(Key.DETAIL_TITLE),
                Lang.t(Key.MSG_HISTORY_HINT),
                alpha
        );

        int left = panelX;
        int panelPad = left + PAD;
        int statusWidth = Math.max(1, Math.min(
                panelWidth / 2,
                textRenderer.getWidth(status()) + 16
        ));
        int statusX = panelX + panelWidth - PAD - statusWidth;
        int statusY = panelTop + 8;
        int statusColor = switch (record.getEffectiveStatus()) {
            case DONE -> Ui.SUCCESS;
            case FAILED -> Ui.DANGER;
            case NO_RESPONSE -> Ui.WARNING;
            case PENDING -> Ui.ACCENT;
        };

        Ui.drawRoundRect(
                context,
                statusX,
                statusY,
                statusWidth,
                16,
                Design.RADIUS_SM,
                Ui.argb(Ui.mix(Ui.SURFACE_2, statusColor, 0.18f), alpha)
        );
        Ui.drawRoundBorder(
                context,
                statusX,
                statusY,
                statusWidth,
                16,
                Design.RADIUS_SM,
                Ui.argb(statusColor, alpha * 0.45f)
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(status()),
                statusX + statusWidth / 2,
                statusY + 4,
                Ui.argb(statusColor, alpha)
        );

        String playerName = player() == null || player().isBlank()
                ? Lang.t(Key.DETAIL_NONE)
                : player();
        String heading = Lang.t(Key.DETAIL_PLAYER) + " · " + playerName;
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth(
                        heading,
                        Math.max(1, statusX - panelPad - 8)
                )),
                panelPad,
                panelTop + 12,
                Ui.argb(Ui.TEXT, alpha)
        );
        Ui.drawDivider(
                context,
                panelPad,
                panelTop + 30,
                panelWidth - PAD * 2,
                alpha
        );

        int valueX = left + PAD + labelW;

        int bottom = panelBottom - footerHeight();

        context.enableScissor(
                panelPad,
                CONTENT_TOP,
                panelX + panelWidth - PAD,
                Math.max(CONTENT_TOP + 1, bottom)
        );

        int y = CONTENT_TOP - scroll;
        int rowIndex = 0;
        int rowWidth = Math.max(
                1,
                panelWidth - PAD * 2 - SCROLLBAR_RESERVE
        );

        for (Row row : rows) {

            if (y + row.height < CONTENT_TOP) {
                y += row.height;
                rowIndex++;
                continue;
            }

            if (y > bottom) {
                break;
            }

            Ui.drawRoundRect(
                    context,
                    panelPad,
                    y - 3,
                    rowWidth,
                    Math.max(1, row.height - FIELD_GAP + 3),
                    Design.RADIUS_SM,
                    Ui.argb(
                            rowIndex % 2 == 0
                                    ? Ui.SURFACE_2
                                    : Ui.SURFACE,
                            alpha * 0.75f
                    )
            );

            // label, aligned with the first value line
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(textRenderer.trimToWidth(
                            row.label,
                            Math.max(1, labelW - 4)
                    )),
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

            rowIndex++;
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
                panelX + panelWidth - PAD - 2,
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
