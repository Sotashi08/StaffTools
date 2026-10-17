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
import net.minecraft.text.TextColor;
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

        // EssentialsX /nick rewrites the display name in the tab list,
        // so it is re-read on every message as well
        EssentialsNickTracker.get().refreshFromTabList();

        // and its "is now known as ..." announcements are parsed here
        EssentialsNickTracker.get().observeMessage(
                message.getString()
        );

        return processNode(message, Style.EMPTY);
    }

    /**
     * Second detection pass, run when the per-node pass found nothing.
     *
     * A self-written chat plugin usually sends the whole line as one
     * {@code PlainTextContent} with a rich style, so the per-node pass
     * sees the entire line as one "word" and never matches a nick inside
     * it. This pass flattens the message, detects over the whole string
     * and rebuilds it with the nicks highlighted.
     *
     * Styles are preserved by walking the tree into a flat list of
     * (char, style) pairs, so rebuilding keeps the plugin's colours.
     */
    public static Text processWholeMessage(Text message) {

        if (message == null) {
            return null;
        }

        FlatLine flat = flatten(message);

        if (flat.text().isBlank()) {
            return message;
        }

        List<PlayerMention> mentions =
                PlayerMentionDetector.detect(flat.text());

        if (mentions.isEmpty()) {
            return message;
        }

        MutableText result =
                Text.literal("").setStyle(message.getStyle());

        int cursor = 0;

        for (PlayerMention mention : mentions) {

            if (mention.start() < cursor ||
                    mention.start() >= mention.end()) {

                continue;
            }

            if (mention.start() > cursor) {

                appendSlice(
                        result,
                        flat,
                        cursor,
                        mention.start(),
                        null
                );
            }

            appendSlice(
                    result,
                    flat,
                    mention.start(),
                    mention.end(),
                    mention
            );

            cursor = mention.end();
        }

        if (cursor < flat.text().length()) {

            appendSlice(
                    result,
                    flat,
                    cursor,
                    flat.text().length(),
                    null
            );
        }

        return result;
    }

    /**
 * A flattened message plus the style that was active at every
 * character, so the rebuilt text keeps the plugin's formatting.
 */
    private record FlatLine(
            String text,
            List<Style> styles
    ) {
    }

    /** Flattens the tree into one string with a style per character. */
    private static FlatLine flatten(Text node) {

        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();

        flattenInto(node, Style.EMPTY, text, styles, 0);

        return new FlatLine(text.toString(), styles);
    }

    private static void flattenInto(
            Text node,
            Style inherited,
            StringBuilder text,
            List<Style> styles,
            int depth
    ) {

        // a malformed plugin component must not hang the client
        if (node == null || depth > 8) {
            return;
        }

        Style style = inherited;

        if (node.getStyle() != null &&
                !node.getStyle().isEmpty()) {

            style = node.getStyle().withParent(inherited);
        }

        String content =
                node.getContent() instanceof PlainTextContent plain
                        ? plain.string()
                        : null;

        if (content != null) {

            // the style is captured once: it has to stay effectively
            // final for the code point loop below
            Style charStyle = style;

            content.codePoints().forEach(cp -> {

                text.appendCodePoint(cp);
                styles.add(charStyle);

            });
        }

        // translatable arguments are part of what the player sees
        if (node.getContent()
                instanceof TranslatableTextContent translatable) {

            for (Object arg : translatable.getArgs()) {

                if (arg instanceof Text child) {

                    flattenInto(
                            child,
                            style,
                            text,
                            styles,
                            depth + 1
                    );
                }
            }
        }

        for (Text sibling : node.getSiblings()) {

            flattenInto(
                    sibling,
                    style,
                    text,
                    styles,
                    depth + 1
            );
        }
    }

    /** Appends a range of the flat line, styled per character. */
    private static void appendSlice(
            MutableText out,
            FlatLine flat,
            int from,
            int to,
            PlayerMention mention
    ) {

        if (from < 0 || to > flat.text().codePointCount(0, flat.text().length())) {
            return;
        }

        // a null mention is an ordinary piece of the line: it is copied
        // over exactly as the server styled it
        PlayerContext player = mention == null
                ? null
                : mention.player();

        boolean highlight =
                mention != null && mention.highlight();

        int[] codePoints =
                flat.text().codePoints()
                        .toArray();

        StringBuilder buffer = new StringBuilder();
        Style current = null;
        Style styled = null;

        for (int i = from; i < to; i++) {

            Style at = flat.styles().size() > i
                    ? flat.styles().get(i)
                    : Style.EMPTY;

            Style effective = player == null
                    ? at
                    : mentionStyle(
                            at,
                            new PlayerMention(
                                    player,
                                    0,
                                    0,
                                    highlight
                            )
                    );

            if (current == null ||
                    !current.equals(at) ||
                    styled == null) {

                flush(out, buffer, styled);

                current = at;
                styled = effective;
            }

            buffer.appendCodePoint(codePoints[i]);
        }

        flush(out, buffer, styled);
    }

    private static void flush(
            MutableText out,
            StringBuilder buffer,
            Style style
    ) {

        if (buffer.isEmpty()) {
            return;
        }

        out.append(
                Text.literal(buffer.toString())
                        .setStyle(style == null
                                ? Style.EMPTY
                                : style)
        );

        buffer.setLength(0);
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

        // 1b. a player known only under a nick they changed to
        if (config.isDetectAliases()) {

            PlayerContext renamed =
                    EssentialsNickTracker.get()
                            .resolve(word);

            if (renamed != null) {
                return renamed;
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

    private static Text processNode(
            Text node,
            Style inherited
    ) {

        TextContent content = node.getContent();

        // a chat plugin often puts the hover on a parent and the nick in
        // a child, so the style of the node alone is not enough: the
        // child inherits from the parent while walking
        Style style = inherited;

        Style own = node.getStyle();

        if (own != null && !own.isEmpty()) {
            style = own.withParent(inherited);
        }

        // the click and the hover are separate settings, so the branches are
        // gated inside resolveFromEvents instead of here
        Resolved eventPlayer =
                resolveFromEvents(style);

        if (eventPlayer != null) {

            NickCache.get().remember(eventPlayer.player());

            NickAliases.learn(
                    node.getString(),
                    eventPlayer.player()
            );

            Style mentioned = mentionStyle(
                    style,
                    new PlayerMention(
                            eventPlayer.player(),
                            0,
                            0,
                            eventPlayer.highlight()
                    )
            );

            // A node can hold more than one nick. When the event only
            // explains the whole node, the plain-text pass still runs on
            // top, so a second player in the very same component is not
            // swallowed by the first.
            MutableText result = bare(node, mentioned);

            if (content instanceof PlainTextContent plain) {

                String string = plain.string();

                if (string != null && !string.isEmpty()) {

                    List<PlayerMention> others =
                            PlayerMentionDetector.detect(string);

                    if (!others.isEmpty()) {

                        result = Text.literal("")
                                .setStyle(mentioned);

                        for (Text part :
                                buildHighlighted(
                                        string,
                                        others,
                                        mentioned
                                )) {

                            result.append(part);
                        }
                    }
                }
            }

            // the siblings keep the ORIGINAL inherited style on purpose:
            // the click must not bleed into the rest of the message, and
            // a child that inherits the same hover resolves on its own
            return withProcessedSiblings(
                    result,
                    node,
                    style
            );
        }

        if (content instanceof PlainTextContent plain) {
            String string = plain.string();

            // A chat plugin very often sends the line as one spacer node
            // with everything else in its siblings. Bailing out here
            // used to drop that whole subtree, so no nick below the
            // spacer was ever looked at.
            if (string == null || string.isEmpty()) {

                return withProcessedSiblings(
                        bare(node, style),
                        node,
                        style
                );
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

                    return withProcessedSiblings(
                            bare(node, style),
                            node,
                            style
                    );
                }

                NickCache.get().remember(byDisplayName);

                NickAliases.learn(string, byDisplayName);

                return withProcessedSiblings(
                        bare(
                                node,
                                mentionStyle(
                                        style,
                                        new PlayerMention(
                                                byDisplayName,
                                                0,
                                                0
                                        )
                                )
                        ),
                        node,
                        style
                );
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
                    node,
                    style
            );
        }

        if (content instanceof TranslatableTextContent translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean changed = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Text arg) {
                    Text processed = processNode(arg, style);
                    newArgs[i] = processed;
                    changed |= processed != arg;
                } else {
                    newArgs[i] = args[i];
                }
            }

            // even when no argument changed the siblings still have to be
            // visited: that is where a chat plugin puts the nick
            if (changed) {

                MutableText result =
                        Text.translatableWithFallback(
                                translatable.getKey(),
                                translatable.getFallback(),
                                newArgs
                        ).setStyle(style);

                return withProcessedSiblings(
                        result,
                        node,
                        style
                );
            }
        }

        return withProcessedSiblings(
                bare(node, style),
                node,
                style
        );
    }

    /**
     * A copy of a node that carries only its own content and style.
     *
     * {@link Text#copy()} would bring the siblings along, and appending
     * them again afterwards would print every one of them twice.
     */
    private static MutableText bare(
            Text node,
            Style style
    ) {

        MutableText out;

        TextContent content = node.getContent();

        if (content instanceof PlainTextContent plain) {

            String string = plain.string();

            out = Text.literal(
                    string == null ? "" : string);

        } else if (content
                instanceof TranslatableTextContent translatable) {

            out = Text.translatableWithFallback(
                    translatable.getKey(),
                    translatable.getFallback(),
                    translatable.getArgs()
            );

        } else {

            out = Text.literal("");
        }

        return out.setStyle(style);
    }

    /**
     * Appends the processed siblings of {@code node} to {@code out}.
     * Branches that were left untouched hand back their very own node,
     * so a copy of a sibling is made only where the highlighting really
     * changed something.
     */
    private static Text withProcessedSiblings(
            MutableText out,
            Text node,
            Style inherited
    ) {
        for (Text sibling : node.getSiblings()) {
            out.append(processNode(sibling, inherited));
        }

        return out;
    }

    /**
     * A player identified through a click or hover event, plus whether
     * the visible word may be repainted.
     */
    private record Resolved(
            PlayerContext player,
            boolean highlight
    ) {
    }

    /**
     * Identifies the player a component belongs to through its events
     * rather than through its text.
     *
     * The tooltip is what makes servers with EssentialsX work: after
     * {@code /nick} the chat prints the new name, while hovering it
     * shows the real account name. Resolving that tooltip against the
     * tab list is an exact comparison against {@code profile.name()},
     * so it can never mistake a rank or a suffix word for a nick.
     */
    private static Resolved resolveFromEvents(Style style) {

        if (style == null) {
            return null;
        }

        boolean byClick =
                StafftoolsClient.getConfig().isDetectClick();

        // 1. hover with a real entity: uuid + name, nothing to guess
        PlayerContext fromEntity =
                resolveFromEntityHover(style.getHoverEvent());

        if (fromEntity != null) {
            return new Resolved(fromEntity, true);
        }

        if (byClick) {

            // 2. custom click payload: plugins ship the nick inside
            PlayerContext fromCustom =
                    resolveFromCustomClick(style.getClickEvent());

            if (fromCustom != null) {
                return new Resolved(fromCustom, true);
            }

            // 3. click command aimed at a player
            PlayerContext fromCommand =
                    resolveFromForeignClick(
                            commandOf(style.getClickEvent())
                    );

            if (fromCommand != null) {
                return new Resolved(fromCommand, true);
            }
        }

        if (!StafftoolsClient.getConfig().isDetectHover()) {
            return null;
        }

        // 4. the tooltip of the visible word: on EssentialsX it holds
        //    the real nick of a renamed player
        Text hoverText = hoverTextOf(style.getHoverEvent());

        if (hoverText != null) {

            PlayerContext player =
                    resolveFromTooltip(hoverText.getString());

            if (player != null) {
                // the word on screen is a /nick, so it keeps the
                // colour the server gave it
                return new Resolved(player, false);
            }
        }

        return null;
    }
    /**
     * Words that mark a tooltip as an information block instead of a
     * name. A chat plugin often hangs one such block over the whole
     * message, and scanning it for names used to swallow the entire
     * line: the moment the real nick appeared inside it, every word
     * of the message was styled as that player.
     */
    private static final java.util.Set<String> TOOLTIP_MARKERS =
            java.util.Set.of(
                    "\u043a\u043b\u0430\u043d", "\u0432\u043b\u0430\u0434\u0435\u043b\u0435\u0446",
                    "\u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a", "\u043c\u043e\u0434\u0435\u0440\u0430\u0442\u043e\u0440",
                    "\u0443\u0431\u0438\u0439\u0441\u0442\u0432", "\u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442",
                    "\u0441\u043e\u0437\u0434\u0430\u043d", "\u043e\u043d\u043b\u0430\u0439\u043d",
                    "\u043c\u0435\u0441\u0442\u043e", "\u0443\u0440\u043e\u0432\u0435\u043d\u044c", "\u043e\u043f\u044b\u0442",
                    "\u0433\u0440\u0443\u043f\u043f\u0430", "\u0433\u0438\u043b\u044c\u0434\u0438\u044f", "\u0442\u0438\u0442\u0443\u043b",
                    "\u0437\u0432\u0430\u043d\u0438\u0435", "\u0434\u043e\u043b\u0436\u043d\u043e\u0441\u0442\u044c", "\u0440\u0430\u043d\u0433",
                    "\u0431\u0430\u043b\u0430\u043d\u0441",
                    "clan", "owner", "members", "moderators", "kills",
                    "exists", "created", "rank", "title", "level",
                    "balance", "guild", "faction", "nation", "group"
            );

    /**
     * Resolves a tooltip that shows the real nick of a renamed player.
     *
     * Deliberately strict: only a tooltip that IS a single name is
     * accepted. Hovering a renamed nick shows that name alone, while
     * any information block — clan, owner, members, kills — is
     * rejected outright instead of being mined for a name.
     */
    private static PlayerContext resolveFromTooltip(String tooltip) {

        if (tooltip == null || tooltip.isBlank()) {
            return null;
        }

        // an information block never belongs to a single player
        String lower = tooltip.toLowerCase();

        for (String marker : TOOLTIP_MARKERS) {
            if (lower.contains(marker)) {
                return null;
            }
        }

        String first = "";

        for (String line : tooltip.split("\\R")) {

            String trimmed = line.trim();

            if (!trimmed.isEmpty()) {

                if (first.isEmpty()) {
                    first = trimmed;
                } else {
                    // a second line means this is a block, not a name
                    return null;
                }
            }
        }

        if (first.isEmpty()) {
            return null;
        }

        // the tooltip must be the bare name: "Player", nothing else
        if (!first.chars().allMatch(c ->
                TextNormalizer.isNameChar((char) c) || c == ' ')) {
            return null;
        }

        if (first.contains(" ")) {
            return null;
        }

        PlayerContext exact =
                PlayerResolver.resolve(first)
                        .orElse(null);

        if (exact != null) {
            return exact;
        }

        // and finally a nick learned from a /nick announcement
        return EssentialsNickTracker.get().resolve(first);
    }

    /**
     * {@code HoverEvent.SHOW_ENTITY} carries the UUID and the name of
     * the entity the component points at. Chat plugins almost always
     * keep it on the nick, which makes it the most reliable source
     * available: unlike text matching it cannot be broken by prefixes,
     * colours, look-alike characters or a custom display name.
     */
    private static PlayerContext resolveFromEntityHover(
            HoverEvent hover
    ) {

        if (!(hover instanceof HoverEvent.ShowEntity show)) {
            return null;
        }

        if (!StafftoolsClient.getConfig().isDetectHover()) {
            return null;
        }

        HoverEvent.EntityContent entity = show.entity();

        if (entity == null || entity.uuid == null) {
            return null;
        }

        PlayerContext online =
                PlayerResolver.resolveByUuid(entity.uuid)
                        .orElse(null);

        if (online != null) {
            return online;
        }

        String name = entity.name
                .map(Text::getString)
                .filter(value -> !value.isBlank())
                .orElse(null);

        if (name == null) {
            return null;
        }

        return new PlayerContext(name.trim(), entity.uuid);
    }

    /**
     * {@code ClickEvent.CUSTOM} carries an arbitrary plugin payload.
     * Chat plugins put the nick into it, so the payload is turned into
     * its text form and scanned for anything that resolves to a player.
     */
    private static PlayerContext resolveFromCustomClick(
            ClickEvent click
    ) {

        if (!(click instanceof ClickEvent.Custom custom)) {
            return null;
        }

        var payload = custom.payload();

        if (payload.isEmpty()) {
            return null;
        }

        // a plugin payload is arbitrary NBT, so it is read as text with
        // quotes and braces dropped: what is left is searched for a name
        // exactly, never claimed as one by resemblance
        String haystack = payload.get()
                .toString()
                .replaceAll("[\"'{}:,]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (haystack.isEmpty() || haystack.length() > 256) {
            return null;
        }

        // the payload is usually the bare nick, maybe with extra words
        for (String word :
                haystack.split("[^A-Za-z0-9_]")) {

            PlayerContext exact =
                    PlayerResolver.resolve(word)
                            .orElse(null);

            if (exact != null) {
                return exact;
            }
        }

        return PlayerResolver.resolveByDisplayName(haystack)
                .orElse(null);
    }

    /**
     * Splits a plain string around the detected ranges so each of them
     * can be styled on its own.
     */
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

            parts.add(
                    styleName(
                            string.substring(
                                    mention.start(),
                                    mention.end()
                            ),
                            baseStyle,
                            mention
                    )
            );

            cursor = mention.end();
        }

        if (cursor < string.length()) {
            parts.add(
                    Text.literal(string.substring(cursor))
                            .setStyle(baseStyle)
            );
        }

        return parts;
    }

    private static Text styleName(
            String name,
            Style baseStyle,
            PlayerMention mention
    ) {
        return Text.literal(name)
                .setStyle(mentionStyle(baseStyle, mention));
    }

    /**
     * The style applied to a detected nick.
     *
     * A mention found through the real account name is repainted in
     * the mod's colour, so it stands out in a wall of text. A mention
     * that is only known through a changed /nick keeps the colour the
     * server sent, because that word belongs to somebody's message.
     * Both are clickable either way.
     */
    private static Style mentionStyle(
            Style baseStyle,
            PlayerMention mention
    ) {

        PlayerContext player = mention.player();

        String value =
                "stafftools:player:" +
                        player.name() +
                        ":" +
                        player.uuid();

        Style styled = baseStyle
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

        return mention.highlight()
                ? styled.withColor(nickColor())
                : styled;
    }

    /**
     * The color a detected nick is painted with.
     *
     * It comes from the theme by default and can be overridden by hand
     * in the theme screen, which is why it is read live instead of
     * being cached per message.
     */
    private static TextColor nickColor() {

        int rgb = com.uwdie.stafftools.client.ui.Ui
                .NICK_HIGHLIGHT & 0xFFFFFF;

        return TextColor.fromRgb(rgb);
    }
}
