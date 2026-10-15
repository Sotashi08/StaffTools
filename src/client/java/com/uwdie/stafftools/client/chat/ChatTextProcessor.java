package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks every chat message and makes player names clickable so the mod
 * can intercept clicks (copy to clipboard / open the actions menu).
 *
 * Detection layers (in order):
 * 1. ClickEvent with a player-targeting command (/msg, /tell, /w ...) —
 *    many chat plugins make display names clickable with the REAL nick.
 * 2. HoverEvent SHOW_TEXT — plugins often reveal the real nick on hover
 *    while displaying a custom nickname/placeholder in plain text.
 * 3. Plain-text matching against the tab-list (original behavior).
 */
public final class ChatTextProcessor {

    private static final Pattern COMMAND_PLAYER = Pattern.compile(
            "^/?(?:msg|tell|whisper|w|m|t)\\s+([A-Za-z0-9_]{2,16})(?:\\s|$)",
            Pattern.CASE_INSENSITIVE
    );

    private ChatTextProcessor() {
    }

    public static Text process(Text message) {
        if (message == null) {
            return null;
        }

        // every incoming message is a chance to learn a nick: the tab
        // list may be empty while the chat still mentions the player
        if (StafftoolsClient.getConfig().isDetectCache()) {
            NickCache.get().refreshFromTabList();
        }

        return processNode(message);
    }

    /**
     * Resolves exactly ONE nick from an arbitrary piece of text using
     * every enabled layer — this is what the "click a nick in chat"
     * and "nick from the input field" triggers use.
     */
    public static PlayerContext resolveAny(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        String word = text.trim();

        com.uwdie.stafftools.client.config.StaffToolsConfig config =
                StafftoolsClient.getConfig();

        // 1. the word itself is a (possibly nick-suffixed) token
        if (config.isDetectCache()) {

            PlayerContext cached =
                    NickCache.get().resolve(word)
                            .orElse(null);

            if (cached != null) {
                return cached;
            }
        }

        // 2. learned aliases: a plugin's custom display name
        if (config.isDetectAliases()) {

            PlayerContext alias =
                    NickAliases.lookup(word)
                            .orElse(null);

            if (alias != null) {
                return alias;
            }
        }

        // 3. full detection over the word
        List<PlayerMention> mentions =
                PlayerMentionDetector.detect(word);

        if (!mentions.isEmpty()) {
            return mentions.get(0).player();
        }

        // 4. typo-tolerant, only when explicitly enabled
        if (config.isDetectFuzzy()) {

            return NickCache.get()
                    .resolveFuzzy(word)
                    .orElse(null);
        }

        return null;
    }

    /**
     * Resolves a player from a foreign click-event command value
     * (e.g. "/msg Notch " inserted by chat plugins). Used to intercept
     * clicks on custom-styled nicks instead of letting vanilla paste
     * the command into the input field.
     */
    public static PlayerContext resolveFromForeignClick(String value) {

        if (value == null || value.startsWith("stafftools:player:")) {
            return null;
        }

        Matcher matcher =
                COMMAND_PLAYER.matcher(value);

        if (matcher.find()) {

            String name = matcher.group(1);

            // online player first...
            PlayerContext resolved =
                    PlayerResolver.resolve(name)
                            .orElse(null);

            if (resolved != null) {
                return resolved;
            }

            // ...otherwise keep the raw nick from the command:
            // the target may be offline, but actions must still open
            return new PlayerContext(
                    name,
                    offlineUuid(name)
            );
        }

        List<PlayerMention> inValue =
                PlayerMentionDetector.detect(value);

        return inValue.isEmpty()
                ? null
                : inValue.get(0).player();
    }

    /**
     * Deterministic UUID for a nick that is not in the tab list
     * (offline-style: derived from the name itself).
     */
    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + name)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * The command behind a click event, whatever command flavour it is.
     * Since 1.21.11 the event is one of the {@code ClickEvent} records,
     * so both the "send it" and the "suggest it" variant are unwrapped.
     */
    private static String commandOf(ClickEvent click) {

        if (click instanceof ClickEvent.RunCommand run) {
            return run.command();
        }

        if (click instanceof ClickEvent.SuggestCommand suggest) {
            return suggest.command();
        }

        return null;
    }

    /**
     * The text behind a hover event, or {@code null} for every other
     * hover flavour.
     */
    private static Text hoverTextOf(HoverEvent hover) {

        if (hover instanceof HoverEvent.ShowText show) {
            return show.value();
        }

        return null;
    }

    private static Text processNode(Text node) {
        TextContent content = node.getContent();
        Style style = node.getStyle();

        PlayerContext eventPlayer =
                StafftoolsClient.getConfig().isDetectClick()
                        ? resolveFromEvents(style)
                        : null;

        if (eventPlayer != null) {

            NickCache.get().remember(eventPlayer);

            NickAliases.learn(
                    node.getString(),
                    eventPlayer
            );

            MutableText styled = node.copy();

            styled.setStyle(
                    mentionStyle(style, eventPlayer)
            );

            return styled;
        }

        if (content instanceof PlainTextContent plain) {
            String string = plain.string();
            if (string == null || string.isEmpty()) {
                return node;
            }

            List<PlayerMention> mentions =
                    PlayerMentionDetector.detect(string);

            if (mentions.isEmpty()) {

                PlayerContext byDisplayName =
                        StafftoolsClient.getConfig().isDetectDisplay()
                                ? PlayerResolver
                                        .resolveByDisplayName(string)
                                        .orElse(null)
                                : null;

                if (byDisplayName == null) {
                    return node;
                }

                NickCache.get().remember(byDisplayName);

                NickAliases.learn(string, byDisplayName);

                MutableText styled = node.copy();

                styled.setStyle(
                        mentionStyle(style, byDisplayName)
                );

                return styled;
            }

            List<Text> parts =
                    buildHighlighted(string, mentions, style);

            MutableText result =
                    Text.literal("").setStyle(style);

            for (Text part : parts) {
                result.append(part);
            }

            return withProcessedSiblings(
                    result,
                    node
            );
        }

        if (content instanceof TranslatableTextContent translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean changed = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Text arg) {
                    Text processed = processNode(arg);
                    newArgs[i] = processed;
                    changed |= processed != arg;
                } else {
                    newArgs[i] = args[i];
                }
            }

            if (changed) {
                MutableText result =
                        Text.translatableWithFallback(
                                translatable.getKey(),
                                translatable.getFallback(),
                                newArgs
                        ).setStyle(style);

                return withProcessedSiblings(
                        result,
                        node
                );
            }
        }

        return node;
    }

    /**
     * Appends the processed siblings of {@code node} to {@code out}.
     * Branches that were left untouched hand back their very own node,
     * so a copy of a sibling is made only where the highlighting really
     * changed something.
     */
    private static Text withProcessedSiblings(
            MutableText out,
            Text node
    ) {
        for (Text sibling : node.getSiblings()) {
            out.append(processNode(sibling));
        }

        return out;
    }

    /**
     * Tries to identify the player this component belongs to by its
     * click/hover events rather than the visible text. This handles
     * chat plugins that replace the nick with a custom display name.
     */
    private static PlayerContext resolveFromEvents(Style style) {

        if (style == null) {
            return null;
        }

        ClickEvent click = style.getClickEvent();

        if (click != null) {

            PlayerContext fromCommand =
                    resolveFromForeignClick(
                            commandOf(click)
                    );

            if (fromCommand != null) {
                return fromCommand;
            }
        }

        if (!StafftoolsClient.getConfig().isDetectHover()) {
            return null;
        }

        Text hoverText = hoverTextOf(style.getHoverEvent());

        if (hoverText != null) {

            String hoverString = hoverText.getString();

            List<PlayerMention> mentions =
                    PlayerMentionDetector.detect(
                            hoverString
                    );

            if (!mentions.isEmpty()) {
                return mentions.get(0).player();
            }

            return PlayerResolver
                    .resolveByDisplayName(hoverString)
                    .orElse(null);
        }

        return null;
    }

    private static List<Text> buildHighlighted(
            String string,
            List<PlayerMention> mentions,
            Style baseStyle
    ) {
        List<PlayerMention> sorted =
                new ArrayList<>(mentions);

        sorted.sort(
                Comparator.comparingInt(
                        PlayerMention::start
                )
        );

        List<Text> parts = new ArrayList<>();
        int cursor = 0;

        for (PlayerMention mention : sorted) {

            if (mention.start() < cursor) {
                continue;
            }

            if (mention.start() > cursor) {
                parts.add(
                        Text.literal(
                                string.substring(
                                        cursor,
                                        mention.start()
                                )
                        ).setStyle(baseStyle)
                );
            }

            String name = string.substring(
                    mention.start(),
                    mention.end()
            );

            parts.add(
                    styleName(
                            name,
                            baseStyle,
                            mention.player()
                    )
            );

            cursor = mention.end();
        }

        if (cursor < string.length()) {
            parts.add(
                    Text.literal(
                            string.substring(cursor)
                    ).setStyle(baseStyle)
            );
        }

        return parts;
    }

    private static Text styleName(
            String name,
            Style baseStyle,
            PlayerContext player
    ) {
        return Text.literal(name)
                .setStyle(mentionStyle(baseStyle, player));
    }

    private static Style mentionStyle(
            Style baseStyle,
            PlayerContext player
    ) {
        String value =
                "stafftools:player:" +
                        player.name() +
                        ":" +
                        player.uuid();

        return baseStyle
                .withColor(Formatting.YELLOW)
                .withClickEvent(
                        new ClickEvent.RunCommand(value)
                )
                .withHoverEvent(
                        new HoverEvent.ShowText(
                                Text.literal(player.name())
                                        .append("\n")
                                        .append(
                                                Text.literal(Lang.t("hover.click"))
                                                        .setStyle(
                                                                Style.EMPTY
                                                                        .withColor(Formatting.GOLD)
                                                        )
                                        )
                                        .append("\n")
                                        .append(
                                                Text.literal(Lang.t("hover.shiftClick"))
                                                        .setStyle(
                                                                Style.EMPTY
                                                                        .withColor(Formatting.GRAY)
                                                        )
                                        )
                        )
                );
    }
}
