package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import com.uwdie.stafftools.client.ui.PlayerActionOverlay;
import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

/**
 * Builds the hover tooltip of a detected nick.
 *
 * <p>Besides the click hints the tooltip carries a small, read-only
 * summary of the player: whether they are online, their ping, game mode,
 * how many punishments the local history holds for them and the server
 * the line came from. Everything is read from data the client already
 * has, so the tooltip never triggers a network request.
 */
public final class NickStats {

    private static final int CARD_WIDTH = 208;
    private static final int CARD_HEIGHT = 112;

    private static final String[] ADDRESS_PHRASES_RU = {
            "{name}, я всё вижу",
            "Спокойно, {name}",
            "{name}, держи планку",
            "Не шуми, {name}",
            "{name}, чат следит",
            "Улыбнись, {name}",
            "{name}, без крайностей",
            "Я за тобой слежу, {name}",
            "{name}, правила рядом",
            "Смотри, {name}, порядок",
            "Без флуда, {name}",
            "{name}, дышим ровно",
            "Ты под наблюдением, {name}",
            "{name}, без капса",
            "За тобой следит админ, {name}",
            "{name}, всё под контролем",
            "Лови внимание, {name}",
            "{name}, прошу уважения",
            "Хорошего чата, {name}",
            "{name}, берегись модератора"
    };

    private static final String[] ADDRESS_PHRASES_EN = {
            "{name}, I am watching",
            "Easy there, {name}",
            "{name}, keep it clean",
            "No noise, {name}",
            "{name}, chat is watching",
            "Smile, {name}",
            "{name}, keep calm",
            "I am keeping an eye on you, {name}",
            "{name}, rules are nearby",
            "Stay in line, {name}",
            "No spam, {name}",
            "{name}, breathe easy",
            "You are being observed, {name}",
            "{name}, no caps",
            "Admin sees you, {name}",
            "{name}, all under control",
            "Heads up, {name}",
            "{name}, please show respect",
            "Have a nice chat, {name}",
            "{name}, beware the moderator"
    };

    private static String hoveredName;
    private static long hoverStartedAt;
    private static float hoverProgress;
    private static int phraseIndex;
    private static boolean hoverPhraseCursorInitialized;

    private NickStats() {
    }

    /**
     * Draws the custom StaffTools player card while the cursor rests on
     * a resolved nick in chat. It replaces Minecraft's vanilla tooltip
     * for nicks processed by StaffTools.
     */
    public static void renderHoverCard(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (PlayerActionOverlay.INSTANCE.isOpen()) {
            resetHoverCard();
            return;
        }

        String word =
                ChatLinePicker.wordAt(mouseX, mouseY);

        if (word == null ||
                word.length() < 3) {

            resetHoverCard();
            return;
        }

        PlayerContext player =
                ChatTextProcessor.resolveAny(word);

        if (player == null) {
            resetHoverCard();
            return;
        }

        if (!player.name()
                .equalsIgnoreCase(hoveredName)) {

            hoveredName = player.name();
            hoverStartedAt = System.currentTimeMillis();
            hoverProgress = 0f;
            phraseIndex = nextHoverPhraseIndex();
        }

        hoverProgress = Ui.animateTowards(
                hoverProgress,
                1f,
                delta,
                16f
        );

        float entrance =
                Ui.animationsEnabled()
                        ? Ui.entranceProgress(
                                hoverStartedAt,
                                20L,
                                180L
                        )
                        : 1f;

        float progress =
                Math.max(entrance, hoverProgress);

        MinecraftClient client =
                MinecraftClient.getInstance();

        int screenW =
                client.getWindow().getScaledWidth();
        int screenH =
                client.getWindow().getScaledHeight();

        int x =
                mouseX + 6;
        int y =
                mouseY - CARD_HEIGHT - 5;

        if (y < 4) {
            y = mouseY + 6;
        }

        x = Math.clamp(
                x,
                4,
                screenW - CARD_WIDTH - 4
        );
        y = Math.clamp(
                y,
                4,
                screenH - CARD_HEIGHT - 4
        );

        drawAnimatedCard(
                context,
                x,
                y,
                player,
                Ui.easeOutCubic(progress),
                delta
        );
    }

    private static void resetHoverCard() {

        hoveredName = null;
        hoverStartedAt = 0L;
        hoverProgress = 0f;
    }

    private static void drawAnimatedCard(
            DrawContext context,
            int x,
            int y,
            PlayerContext player,
            float progress,
            float delta
    ) {

        float alpha =
                Ui.clamp01(progress);
        float scale =
                Ui.animationsEnabled()
                        ? 0.965f + 0.035f * alpha
                        : 1f;
        int rise =
                Ui.animationsEnabled()
                        ? Math.round((1f - alpha) * 5f)
                        : 0;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                x + CARD_WIDTH / 2f,
                y + CARD_HEIGHT / 2f + rise
        );
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(
                -(x + CARD_WIDTH / 2f),
                -(y + CARD_HEIGHT / 2f + rise)
        );

        drawCard(
                context,
                x,
                y,
                player,
                alpha
        );

        context.getMatrices().popMatrix();
    }

    private static void drawCard(
            DrawContext context,
            int x,
            int y,
            PlayerContext player,
            float alpha
    ) {

        Ui.drawPopupFrame(
                context,
                x,
                y,
                CARD_WIDTH,
                CARD_HEIGHT,
                alpha
        );

        TextRenderer tr =
                MinecraftClient.getInstance().textRenderer;

        String name =
                fitText(
                        tr,
                        player.name(),
                        CARD_WIDTH - 24
                );

        context.drawTextWithShadow(
                tr,
                Text.literal(name),
                x + 12,
                y + 10,
                Ui.argb(Ui.TEXT, alpha)
        );

        PlayerListEntry entry = entryOf(player);
        int statusColor =
                entry == null
                        ? Ui.TEXT_MUTED
                        : Ui.SUCCESS;

        context.fill(
                x + 12,
                y + 24,
                x + 14,
                y + 26,
                Ui.argb(statusColor, alpha)
        );

        context.drawTextWithShadow(
                tr,
                Text.literal(
                        entry == null
                                ? Lang.t(Key.HOVER_STATS_OFFLINE)
                                : Lang.t(Key.HOVER_STATS_ONLINE)
                ),
                x + 17,
                y + 21,
                Ui.argb(statusColor, alpha)
        );

        int separatorY = y + 39;
        context.fill(
                x + 10,
                separatorY,
                x + CARD_WIDTH - 10,
                separatorY + 1,
                Ui.argb(Ui.BORDER, alpha * 0.75f)
        );

        int col1 = x + 12;
        int col2 = x + CARD_WIDTH / 2 + 4;
        int row1 = y + 48;
        int row2 = y + 66;

        drawStat(
                context,
                tr,
                col1,
                row1,
                Lang.t(Key.HOVER_STATS_PING),
                entry == null ? "—" : entry.getLatency() + " ms",
                entry == null
                        ? Ui.TEXT_MUTED
                        : pingUiColor(entry.getLatency()),
                alpha
        );

        drawStat(
                context,
                tr,
                col2,
                row1,
                Lang.t(Key.HOVER_STATS_GAMEMODE),
                entry == null || entry.getGameMode() == null
                        ? "—"
                        : entry.getGameMode().getId(),
                Ui.ACCENT_SOFT,
                alpha
        );

        drawStat(
                context,
                tr,
                col1,
                row2,
                Lang.t(Key.HOVER_STATS_PUNISH),
                String.valueOf(punishmentCount(player)),
                punishmentCount(player) == 0
                        ? Ui.TEXT_MUTED
                        : Ui.WARNING,
                alpha
        );

        String server = serverName();
        drawStat(
                context,
                tr,
                col2,
                row2,
                Lang.t(Key.HOVER_STATS_SERVER),
                server == null || server.isBlank()
                        ? "—"
                        : fitText(tr, server, CARD_WIDTH / 2 - 18),
                Ui.ACCENT_SOFT,
                alpha
        );

        int footerY = y + CARD_HEIGHT - 18;

        context.fill(
                x + 10,
                footerY - 5,
                x + CARD_WIDTH - 10,
                footerY - 4,
                Ui.argb(Ui.BORDER, alpha * 0.55f)
        );

        String phrase =
                addressedPhrase(player.name());

        int phraseMaxWidth =
                CARD_WIDTH - 24;

        String fittedPhrase =
                fitText(
                        tr,
                        phrase,
                        phraseMaxWidth
                );

        int phraseWidth =
                tr.getWidth(fittedPhrase);

        context.drawTextWithShadow(
                tr,
                Text.literal(fittedPhrase),
                x + (CARD_WIDTH - phraseWidth) / 2,
                footerY,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );
    }

    private static void drawStat(
            DrawContext context,
            TextRenderer tr,
            int x,
            int y,
            String label,
            String value,
            int valueColor,
            float alpha
    ) {

        context.drawTextWithShadow(
                tr,
                Text.literal(label),
                x,
                y,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        context.drawTextWithShadow(
                tr,
                Text.literal(value),
                x,
                y + 10,
                Ui.argb(valueColor, alpha)
        );
    }

    private static String fitText(
            TextRenderer tr,
            String text,
            int maxWidth
    ) {

        if (text == null || text.isBlank()) {
            return "—";
        }

        if (tr.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth =
                tr.getWidth(ellipsis);
        String candidate = text;

        while (candidate.length() > 1 &&
                tr.getWidth(candidate) + ellipsisWidth > maxWidth) {

            candidate =
                    candidate.substring(
                            0,
                            candidate.length() - 1
                    );
        }

        return candidate + ellipsis;
    }

    private static int nextHoverPhraseIndex() {

        String[] pool =
                Lang.getLanguage() == Lang.Language.EN
                        ? ADDRESS_PHRASES_EN
                        : ADDRESS_PHRASES_RU;

        if (!hoverPhraseCursorInitialized) {
            hoverPhraseCursorInitialized = true;
            return 0;
        }

        phraseIndex =
                Math.floorMod(
                        phraseIndex + 1,
                        pool.length
                );

        return phraseIndex;
    }

    private static String addressedPhrase(String name) {

        String[] pool =
                Lang.getLanguage() == Lang.Language.EN
                        ? ADDRESS_PHRASES_EN
                        : ADDRESS_PHRASES_RU;

        String template =
                pool[
                        Math.floorMod(
                                phraseIndex,
                                pool.length
                        )
                ];

        return template.replace(
                "{name}",
                name == null ? "" : name
        );
    }

    private static int pingUiColor(int ping) {

        if (ping <= 60) {
            return Ui.SUCCESS;
        }

        if (ping <= 150) {
            return Ui.WARNING;
        }

        return Ui.DANGER_SOFT;
    }

    /** Tab-list entry of the player, or null when they are offline. */
    private static PlayerListEntry entryOf(PlayerContext player) {

        var handler = MinecraftClient.getInstance()
                .getNetworkHandler();

        if (handler == null || player == null) {
            return null;
        }

        PlayerListEntry byUuid =
                handler.getPlayerListEntry(player.uuid());

        if (byUuid != null) {
            return byUuid;
        }

        return handler.getPlayerListEntry(player.name());
    }

    /** Punishments recorded for this player on the current server. */
    private static int punishmentCount(PlayerContext player) {

        PunishmentHistory history = PunishmentHistory.get();

        if (history == null || player == null) {
            return 0;
        }

        String key = PunishmentHistory.currentServerKey();

        int count = 0;

        for (PunishmentRecord record :
                history.getRecordsOf(key)) {

            if (record.getPlayerName() != null &&
                    record.getPlayerName()
                            .equalsIgnoreCase(player.name())) {

                count++;
            }
        }

        return count;
    }

    private static String serverName() {

        var entry = MinecraftClient.getInstance()
                .getCurrentServerEntry();

        return entry == null ? null : entry.name;
    }
}
