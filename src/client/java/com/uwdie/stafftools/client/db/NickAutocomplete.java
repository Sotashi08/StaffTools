package com.uwdie.stafftools.client.db;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.uwdie.stafftools.client.chat.NickCache;
import com.uwdie.stafftools.client.ui.Ui;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Registers the {@code /stnicks} client command, which suggests nicks
 * stored in the database (including players who have left).
 *
 * <p>Two features:
 * <ul>
 *   <li><b>Tab-completion</b> — {@code /stnicks pl<Tab>} suggests nicks
 *       that <em>start with</em> the prefix.</li>
 *   <li><b>Search + pagination</b> — {@code /stnicks pl} prints every
 *       nick that <em>contains</em> "pl", 20 per page, with clickable
 *       {@code << Page N >>} navigation.</li>
 * </ul>
 */
public final class NickAutocomplete {

    private static final int PAGE_SIZE = 20;

    private NickAutocomplete() {
    }

    /** Theme accent as a TextColor. */
    private static TextColor accent() {
        return TextColor.fromRgb(Ui.ACCENT);
    }

    /** Theme text color as a TextColor. */
    private static TextColor text() {
        return TextColor.fromRgb(Ui.TEXT);
    }

    /** Theme muted color as a TextColor. */
    private static TextColor muted() {
        return TextColor.fromRgb(Ui.TEXT_MUTED);
    }

    /** Builds a single-line colored text from parts. */
    private static Text line(Object... parts) {

        Text out = Text.empty();

        for (Object p : parts) {

            if (p instanceof Text t) {
                out = out.copy().append(t);
            } else if (p instanceof String s) {
                out = out.copy().append(
                        Text.literal(s).setStyle(
                                Style.EMPTY.withColor(text())));
            }
        }

        return out;
    }

    public static void register() {

        // Tab-completion: prefix match from the in-memory cache.
        SuggestionProvider<FabricClientCommandSource> provider =
                (ctx, builder) -> {

                    String remaining = builder.getRemaining()
                            .toLowerCase(Locale.ROOT);

                    for (String nick : NickCache.get().allKnownNicks()) {

                        if (remaining.isEmpty()
                                || nick.toLowerCase(Locale.ROOT)
                                        .startsWith(remaining)) {

                            builder.suggest(nick);
                        }
                    }

                    return builder.buildFuture();
                };

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) ->
                        dispatcher.register(
                                ClientCommandManager.literal("stnicks")
                                        .then(ClientCommandManager
                                                .literal("list")
                                                .executes(ctx -> {
                                                    showStats();
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager
                                                .argument("query",
                                                        StringArgumentType.word())
                                                .suggests(provider)
                                                .executes(ctx -> {

                                                    String query =
                                                            StringArgumentType
                                                                    .getString(
                                                                            ctx,
                                                                            "query");

                                                    showPage(query, 0);

                                                    return 1;
                                                })
                                                .then(ClientCommandManager
                                                        .argument("page",
                                                                com.mojang.brigadier
                                                                        .arguments
                                                                        .IntegerArgumentType
                                                                        .integer(0))
                                                        .executes(ctx -> {

                                                            String query =
                                                                    StringArgumentType
                                                                            .getString(
                                                                                    ctx,
                                                                                    "query");

                                                            int page =
                                                                    com.mojang.brigadier
                                                                            .arguments
                                                                            .IntegerArgumentType
                                                                            .getInteger(
                                                                                    ctx,
                                                                                    "page");

                                                            showPage(query,
                                                                    page);

                                                            return 1;
                                                        })))));
    }

    /** Prints nick statistics: total, online, and a joke phrase. */
    private static void showStats() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        com.uwdie.stafftools.client.db.Database db =
                com.uwdie.stafftools.client.db.Database.get();

        int total = db == null ? 0 : db.countNicks();

        int online = 0;

        if (client.getNetworkHandler() != null) {
            online = client.getNetworkHandler()
                    .getPlayerList().size();
        }

        String[] jokes = {
                "Память у меня лучше, чем у твоего сервера",
                "Каждый ник — это чья-то история",
                "Я помню всех, даже тех, кто вышел",
                "База данных не жалуется на объём",
                "Ники копятся быстрее, чем проблемы",
                "Тут есть даже те, кого ты уже забыл"
        };

        String joke = jokes[(int) (System.currentTimeMillis()
                % jokes.length)];

        client.inGameHud.getChatHud().addMessage(
                line(Text.literal("StaffTools ")
                                .setStyle(Style.EMPTY.withColor(muted())),
                        Text.literal("— ").setStyle(
                                Style.EMPTY.withColor(muted())),
                        Text.literal(total + " ").setStyle(
                                Style.EMPTY.withColor(accent())),
                        Text.literal("nicks total, ").setStyle(
                                Style.EMPTY.withColor(muted())),
                        Text.literal(online + " ").setStyle(
                                Style.EMPTY.withColor(accent())),
                        Text.literal("online").setStyle(
                                Style.EMPTY.withColor(muted()))));

        client.inGameHud.getChatHud().addMessage(
                Text.literal(joke).setStyle(
                        Style.EMPTY.withColor(muted())));
    }

    /** Prints one page of nicks containing {@code query}. */
    private static void showPage(String query, int page) {

        String lower = query.toLowerCase(Locale.ROOT);

        List<String> matches = new ArrayList<>();

        for (String nick : NickCache.get().allKnownNicks()) {

            if (nick.toLowerCase(Locale.ROOT).contains(lower)) {
                matches.add(nick);
            }
        }

        matches.sort(String::compareToIgnoreCase);

        int totalPages = Math.max(
                1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        page = Math.clamp(page, 0, totalPages - 1);

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, matches.size());

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud.getChatHud() == null) {
            return;
        }

        // header
        client.inGameHud.getChatHud().addMessage(
                line(Text.literal("StaffTools ").setStyle(
                                Style.EMPTY.withColor(muted())),
                        Text.literal("— nicks containing ").setStyle(
                                Style.EMPTY.withColor(muted())),
                        Text.literal(query).setStyle(
                                Style.EMPTY.withColor(accent())),
                        Text.literal(" (page " + (page + 1) + "/"
                                + totalPages + ")").setStyle(
                                Style.EMPTY.withColor(muted()))));

        // nicks on this page
        for (int i = from; i < to; i++) {
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("  " + matches.get(i)).setStyle(
                            Style.EMPTY.withColor(text())));
        }

        if (matches.isEmpty()) {
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("  no matches").setStyle(
                            Style.EMPTY.withColor(muted())));
        }

        // single-line navigation: only the arrows are clickable
        Text nav = Text.literal("[ ").setStyle(
                Style.EMPTY.withColor(muted()));

        if (page > 0) {
            nav = nav.copy().append(
                    Text.literal("<<")
                            .setStyle(Style.EMPTY.withColor(accent())
                                    .withClickEvent(
                                            new ClickEvent.RunCommand(
                                                    "/stnicks " + query
                                                            + " "
                                                            + (page - 1)))));
        } else {
            nav = nav.copy().append(Text.literal("<<").setStyle(
                    Style.EMPTY.withColor(muted())));
        }

        nav = nav.copy().append(
                Text.literal(" Page " + (page + 1) + "/" + totalPages
                        + " ").setStyle(
                        Style.EMPTY.withColor(muted())));

        if (page < totalPages - 1) {
            nav = nav.copy().append(
                    Text.literal(">>")
                            .setStyle(Style.EMPTY.withColor(accent())
                                    .withClickEvent(
                                            new ClickEvent.RunCommand(
                                                    "/stnicks " + query
                                                            + " "
                                                            + (page + 1)))));
        } else {
            nav = nav.copy().append(Text.literal(">>").setStyle(
                    Style.EMPTY.withColor(muted())));
        }

        nav = nav.copy().append(Text.literal(" ]").setStyle(
                Style.EMPTY.withColor(muted())));

        client.inGameHud.getChatHud().addMessage(nav);
    }
}