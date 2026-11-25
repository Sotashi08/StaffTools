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

    private static final int CARD_WIDTH = 224;
    private static final int CARD_HEIGHT = 124;

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

        PlayerListEntry entry = entryOf(player);
        boolean online = entry != null;
        int statusColor = online ? Ui.SUCCESS : Ui.TEXT_MUTED;

        // accent ribbon across the top
        int ribbon = Ui.mix(Ui.ACCENT, Ui.ACCENT_2, 0.5f);
        Ui.drawRoundRect(
                context,
                x + 6, y + 4, CARD_WIDTH - 12, 3,
                2, Ui.argb(ribbon, alpha));

        // gradient header background (accent tint fading down)
        context.fillGradient(
                x + 6, y + 6,
                x + CARD_WIDTH - 6, y + 40,
                Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.18f), alpha),
                Ui.argb(Ui.PANEL_BG, 0f));

        // soft accent glow behind the header
        Ui.drawHoverGlow(
                context,
                x + 6, y + 6, CARD_WIDTH - 12, 22,
                4, Ui.ACCENT, 0.5f, alpha * 0.5f);

        // left accent stripe next to the name
        Ui.drawRoundRect(
                context,
                x + 8, y + 12, 3, 12,
                2, Ui.argb(ribbon, alpha));

        // player name
        String name =
                fitText(
                        tr,
                        player.name(),
                        CARD_WIDTH - 30
                );

        context.drawTextWithShadow(
                tr,
                Text.literal(name),
                x + 15,
                y + 12,
                Ui.argb(Ui.TEXT, alpha)
        );

        // animated status dot (pulses while online)
        float pulse = online && Ui.animationsEnabled()
                ? 0.5f + 0.5f * (float) Math.sin(
                        (System.currentTimeMillis() % 1200L)
                                / 1200.0 * Math.PI * 2)
                : 1f;

        int dotX = x + 14;
        int dotY = y + 30;
        Ui.drawRoundRect(
                context,
                dotX - 1, dotY - 1, 6, 6,
                3, Ui.argb(statusColor, alpha * 0.35f * pulse));
        Ui.drawRoundRect(
                context,
                dotX, dotY, 4, 4,
                2, Ui.argb(statusColor, alpha));

        context.drawTextWithShadow(
                tr,
                Text.literal(
                        online
                                ? Lang.t(Key.HOVER_STATS_ONLINE)
                                : Lang.t(Key.HOVER_STATS_OFFLINE)
                ),
                x + 21,
                y + 27,
                Ui.argb(statusColor, alpha)
        );

        // divider
        int separatorY = y + 42;
        context.fill(
                x + 10,
                separatorY,
                x + CARD_WIDTH - 10,
                separatorY + 1,
                Ui.argb(Ui.BORDER, alpha * 0.75f)
        );

        int col1 = x + 12;
        int col2 = x + CARD_WIDTH / 2 + 6;
        int row1 = y + 52;
        int row2 = y + 74;

        drawStat(
                context,
                tr,
                col1,
                row1,
                Lang.t(Key.HOVER_STATS_PING),
                online ? entry.getLatency() + " ms" : "—",
                online
                        ? pingUiColor(entry.getLatency())
                        : Ui.TEXT_MUTED,
                alpha
        );

        drawStat(
                context,
                tr,
                col2,
                row1,
                Lang.t(Key.HOVER_STATS_GAMEMODE),
                online && entry.getGameMode() != null
                        ? entry.getGameMode().getId()
                        : "—",
                Ui.ACCENT_SOFT,
                alpha
        );

        int punish = punishmentCount(player);

        drawStat(
                context,
                tr,
                col1,
                row2,
                Lang.t(Key.HOVER_STATS_PUNISH),
                String.valueOf(punish),
                punish == 0 ? Ui.TEXT_MUTED : Ui.WARNING,
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

        // footer with accent side bar + rotating phrase
        int footerY = y + CARD_HEIGHT - 20;

        context.fill(
                x + 10,
                footerY - 6,
                x + CARD_WIDTH - 10,
                footerY - 5,
                Ui.argb(Ui.BORDER, alpha * 0.55f)
        );

        Ui.drawRoundRect(
                context,
                x + 10, footerY - 1, 3, 11,
                2, Ui.argb(Ui.ACCENT, alpha));

        String phrase =
                addressedPhrase(player.name());

        String fittedPhrase =
                fitText(
                        tr,
                        phrase,
                        CARD_WIDTH - 30
                );

        context.drawTextWithShadow(
                tr,
                Text.literal(fittedPhrase),
                x + 18,
                footerY,
                Ui.argb(Ui.TEXT_DIM, alpha)
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
