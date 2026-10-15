package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Punishment log: a summary block on top (totals, breakdowns, top
 * offenders) and the entries themselves below. Clicking an entry opens
 * its details.
 */
public class ActionHistoryScreen extends StScreen {

    private static final int ROW_H = 16;
    private static final int ROW_GAP = 3;

    /**
     * Stats block: a panel that stays clear of the title above it, is
     * taller than its content and reaches past the table on both sides,
     * so the orbit frame never sits on top of the text.
     */
    private static final int STATS_PANEL_Y = 34;
    private static final int STATS_PANEL_H = 96;

    /** How far the frame extends past the table on each side. */
    private static final int STATS_INSET = 7;

    /**
     * Padding between the inner border of the panel and its content.
     * The orbit frame plus its glow takes ~10px of the panel edge,
     * so this has to stay well above that.
     */
    private static final int STATS_PAD = 16;

    private static final int CARD_Y = 46;
    private static final int CARD_GAP = 5;
    private static final int CARD_H = 24;

    private static final int TABLE_TOP = 148;
    private static final int HEADER_H = 12;

    private static final int BOTTOM_PAD = 40;

    private static final int COL_TIME = 0;
    private static final int COL_TYPE = 44;
    private static final int COL_PLAYER = 124;
    private static final int COL_STATUS = 214;

    private final Screen parent;

    private final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm");

    private int scroll;
    private int tableBottom;

    /** Summary of the whole log, recomputed while rendering. */
    private HistoryStats stats;

    public ActionHistoryScreen(Screen parent) {

        super(Lang.text(Key.HISTORY_TITLE));

        this.parent = parent;
    }

    @Override
    protected void init() {

        int formW = formWidth();
        int left = formLeft();
        int half = (formW - 12) / 2;

        tableBottom = height - BOTTOM_PAD;

        button(
                left,
                height - 32,
                half,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );

        button(
                left + half + 12,
                height - 32,
                half,
                22,
                Key.BTN_CLEAR,
                b -> {
                    PunishmentHistory.get().clear();
                    scroll = 0;
                },
                Ui.DANGER
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        List<PunishmentRecord> records =
                PunishmentHistory.get().getRecords();

        int step = ROW_H + ROW_GAP;

        int contentHeight =
                records.size() * step;

        int maxScroll = Math.max(
                0,
                contentHeight - (tableBottom - TABLE_TOP)
        );

        scroll = Math.max(
                0,
                Math.min(
                        maxScroll,
                        scroll + (verticalAmount > 0
                                ? -step
                                : step)
                )
        );

        return true;
    }

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();


if (button == 0) {

            PunishmentRecord record =
                    recordAt(mouseX, mouseY);

            if (record != null) {

                client.setScreen(
                        new PunishmentDetailScreen(
                                this,
                                record
                        )
                );

                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    /** Record rendered at the given cursor position, or null. */
    private PunishmentRecord recordAt(
            double mouseX,
            double mouseY
    ) {

        List<PunishmentRecord> records =
                PunishmentHistory.get().getRecords();

        int left = formLeft();
        int width = formWidth();
        int step = ROW_H + ROW_GAP;

        if (mouseX < left ||
                mouseX >= left + width ||
                mouseY < TABLE_TOP ||
                mouseY > tableBottom) {

            return null;
        }

        int y = TABLE_TOP - scroll;

        for (int i = 0; i < records.size(); i++) {

            if (y + ROW_H < TABLE_TOP) {
                y += step;
                continue;
            }

            if (y > tableBottom) {
                break;
            }

            if (mouseY >= y && mouseY < y + ROW_H) {
                return records.get(i);
            }

            y += step;
        }

        return null;
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawTitle(context, Key.HISTORY_TITLE, alpha);

        int left = formLeft();
        int width = formWidth();

        List<PunishmentRecord> records =
                PunishmentHistory.get().getRecords();

        stats = HistoryStats.of(records);

        drawStats(context, left, width, alpha);

        drawTableHeader(context, left, width, alpha);

        if (records.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_HISTORY),
                    width / 2,
                    TABLE_TOP + 22,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        long now = System.currentTimeMillis();

        int y = TABLE_TOP - scroll;

        for (int i = 0; i < records.size(); i++) {

            if (y + ROW_H < TABLE_TOP) {
                y += ROW_H + ROW_GAP;
                continue;
            }

            if (y > tableBottom) {
                break;
            }

            float rowAlpha = Ui.clamp01(
                    (now - openTime - i * 22L) / 180f
            );

            if (rowAlpha > 0.01f) {

                drawRecord(
                        context,
                        records.get(i),
                        left,
                        y,
                        width,
                        mouseX,
                        mouseY,
                        i % 2 == 0,
                        alpha * rowAlpha
                );
            }

            y += ROW_H + ROW_GAP;
        }
    }

    /**
     * Summary block: four number cards plus the breakdown lines.
     */
    private void drawStats(
            DrawContext context,
            int left,
            int width,
            float alpha
    ) {

        // frame is drawn wider than the table itself
        int px = left - STATS_INSET;
        int pw = width + STATS_INSET * 2;

        Ui.drawPanel(
                context,
                px,
                STATS_PANEL_Y,
                pw,
                STATS_PANEL_H,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                px,
                STATS_PANEL_Y,
                pw,
                STATS_PANEL_H,
                alpha
        );

        // four cards, symmetric inside the padded area
        int cardW =
                (pw - STATS_PAD * 2 - CARD_GAP * 3) / 4;

        drawCard(context, cardX(px, 0, cardW), CARD_Y, cardW,
                Key.STAT_TOTAL, stats.getTotal(),
                Ui.ACCENT_SOFT, alpha);

        drawCard(context, cardX(px, 1, cardW), CARD_Y, cardW,
                Key.STAT_CONFIRMED, stats.getConfirmed(),
                Ui.SUCCESS, alpha);

        drawCard(context, cardX(px, 2, cardW), CARD_Y, cardW,
                Key.STAT_FAILED, stats.getFailed(),
                Ui.DANGER, alpha);

        drawCard(context, cardX(px, 3, cardW), CARD_Y, cardW,
                Key.STAT_NO_RESPONSE, stats.getUnanswered(),
                Ui.TEXT_DIM, alpha);

        // first breakdown line sits a few pixels below the card row
        int lineY = CARD_Y + CARD_H + 10;

        // players / most punished nickname
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.STAT_PLAYERS) + ": "
                                + stats.getUniquePlayers()
                                + "  ·  "
                                + Lang.t(Key.STAT_TOP_PLAYER)
                                + ": "
                                + (stats.getTopPlayer() == null
                                        ? Lang.t(Key.DETAIL_NONE)
                                        : stats.topPlayerLabel())
                ),
                px + STATS_PAD,
                lineY,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        // moderators / entries that came from macros and actions
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.STAT_MODERATORS) + ": "
                                + stats.getModerators()
                                + "  ·  "
                                + Lang.t(Key.STAT_FROM_MACROS)
                                + ": "
                                + stats.getFromMacros()
                                + "  ·  "
                                + Lang.t(Key.STAT_FROM_ACTIONS)
                                + ": "
                                + stats.getFromActions()
                ),
                px + STATS_PAD,
                lineY + 11,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        // per-type breakdown
        String breakdown = stats.typeBreakdown();

        if (breakdown != null) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            Lang.t(Key.STAT_BY_TYPE) + ": "
                                    + breakdown
                    ),
                    px + STATS_PAD,
                    lineY + 22,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }

        // hint last, still inside the panel
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.STAT_AWAITING) + ": "
                                + stats.getAwaiting()
                                + "  ·  "
                                + Lang.t(Key.HIST_HINT_CLICK)
                ),
                px + STATS_PAD,
                lineY + 33,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );
    }

    /** Left edge of the nth summary card, symmetric inside the panel. */
    private static int cardX(
            int panelLeft,
            int index,
            int cardW
    ) {

        return panelLeft + STATS_PAD
                + index * (cardW + CARD_GAP);
    }

    /** One number card: caption on top, count below. */
    private void drawCard(
            DrawContext context,
            int x,
            int y,
            int w,
            String captionKey,
            int value,
            int color,
            float alpha
    ) {

        context.fill(
                x,
                y,
                x + w,
                y + CARD_H,
                Ui.argb(0x18FFFFFF, alpha)
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                w,
                CARD_H,
                3,
                Ui.argb(color, alpha * 0.55f)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(Lang.t(captionKey)),
                x + w / 2
                        - textRenderer.getWidth(
                                Lang.t(captionKey)) / 2,
                y + 3,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        String text = Integer.toString(value);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(text),
                x + w / 2
                        - textRenderer.getWidth(text) / 2,
                y + 13,
                Ui.argb(color, alpha)
        );
    }

    private void drawTableHeader(
            DrawContext context,
            int left,
            int width,
            float alpha
    ) {

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_TIME),
                left + COL_TIME,
                TABLE_TOP - HEADER_H,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_ACTION),
                left + COL_TYPE,
                TABLE_TOP - HEADER_H,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_PLAYER),
                left + COL_PLAYER,
                TABLE_TOP - HEADER_H,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_STATUS),
                left + COL_STATUS,
                TABLE_TOP - HEADER_H,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.fill(
                left,
                TABLE_TOP - 3,
                left + width,
                TABLE_TOP - 2,
                Ui.argb(0x2C3B55, alpha)
        );
    }

    private void drawRecord(
            DrawContext context,
            PunishmentRecord record,
            int x,
            int y,
            int width,
            double mouseX,
            double mouseY,
            boolean alt,
            float alpha
    ) {

        boolean hovered =
                mouseX >= x &&
                        mouseX < x + width &&
                        mouseY >= y &&
                        mouseY < y + ROW_H;

        if (hovered) {

            context.fill(
                    x,
                    y,
                    x + width,
                    y + ROW_H,
                    Ui.argb(0x26202028, alpha)
            );
        } else if (alt) {

            context.fill(
                    x,
                    y,
                    x + width,
                    y + ROW_H,
                    Ui.argb(Ui.PANEL_BG_SOFT, alpha)
            );
        }

        String time =
                TIME_FORMAT.format(
                        record.getTimestamp()
                );

        String type =
                record.getType().getIcon() +
                        " " +
                        record.getType().getLabel();

        String player =
                record.getPlayerName() == null
                        ? "?"
                        : record.getPlayerName();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(time),
                x + COL_TIME,
                y + 4,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(type),
                x + COL_TYPE,
                y + 4,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(player),
                x + COL_PLAYER,
                y + 4,
                Ui.argb(Ui.TEXT, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(statusText(record)),
                x + COL_STATUS,
                y + 4,
                Ui.argb(statusColor(record), alpha)
        );
    }

    /** Short status label of one entry. */
    private String statusText(PunishmentRecord record) {

        return switch (record.getEffectiveStatus()) {

            case DONE -> Lang.t(Key.PUN_STATUS_DONE);
            case FAILED -> Lang.t(Key.PUN_STATUS_FAILED);
            case NO_RESPONSE ->
                    Lang.t(Key.PUN_STATUS_NO_RESPONSE);
            case PENDING -> Lang.t(Key.PUN_STATUS_PENDING);
        };
    }

    /** Colour of the status label: green, red, grey or yellow. */
    private int statusColor(PunishmentRecord record) {

        return switch (record.getEffectiveStatus()) {

            case DONE -> Ui.SUCCESS;
            case FAILED -> Ui.DANGER;
            case NO_RESPONSE -> Ui.TEXT_MUTED;
            case PENDING -> Ui.WARNING;
        };
    }
}
