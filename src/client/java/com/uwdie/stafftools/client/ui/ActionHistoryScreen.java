package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Simple punishment log: time / action / player.
 */
public class ActionHistoryScreen extends StScreen {

    private static final int ROW_H = 16;
    private static final int ROW_GAP = 3;
    private static final int TABLE_TOP = 54;
    private static final int BOTTOM_PAD = 40;

    private static final int COL_TIME = 0;
    private static final int COL_ACTION = 78;
    private static final int COL_PLAYER = 158;

    private final Screen parent;

    private final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm");

    private int scroll;
    private int tableBottom;

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
                b -> PunishmentHistory.get().clear(),
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

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_TIME),
                left + COL_TIME,
                42,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_ACTION),
                left + COL_ACTION,
                42,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_COL_PLAYER),
                left + COL_PLAYER,
                42,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.fill(
                left,
                51,
                left + width,
                52,
                Ui.argb(0x2C3B55, alpha)
        );

        List<PunishmentRecord> records =
                PunishmentHistory.get().getRecords();

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
                        i % 2 == 0,
                        alpha * rowAlpha
                );
            }

            y += ROW_H + ROW_GAP;
        }
    }

    private void drawRecord(
            DrawContext context,
            PunishmentRecord record,
            int x,
            int y,
            int width,
            boolean alt,
            float alpha
    ) {

        if (alt) {

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
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(type),
                x + COL_ACTION,
                y,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(player),
                x + COL_PLAYER,
                y,
                Ui.argb(Ui.TEXT, alpha)
        );
    }
}
