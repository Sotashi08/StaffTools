package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Small moderator panel with a rotating set of chat-transition effects.
 *
 * Shows who is on duty, where, how long the session runs and how many
 * punishments were issued during it, plus one rotating funny phrase.
 * The whole panel can be dragged anywhere by its header.
 */
public final class ModeratorHud {

    public static final ModeratorHud INSTANCE = new ModeratorHud();

    private static final int PANEL_W = 176;
    private static final int PAD = 10;
    private static final int LINE_H = 10;
    private static final int HEAD_SIZE = 22;
    private static final int EFFECT_DISINTEGRATE = 0;
    private static final int EFFECT_BLOCKS = 1;
    private static final int EFFECT_FADE = 2;
    private static final int EFFECT_GLITCH = 3;
    private static final int EFFECT_COUNT = 4;
    private static final long TRANSITION_DURATION_MS = 520L;

    /** Custom phrases; they wrap and the frame follows their height. */
    private static final String[] PHRASES = {
            "Спокойно, я тут не для красоты",
            "Сегодня мы выдаём только баны",
            "Терпение — тоже админ-качество",
            "Не трогай меня, я на обеде",
            "Правило первое: не будить модератора",
            "Я тут, чтобы вы не будили модератора",
            "Нарушитель? Каждый через это проходит",
            "Скоро отпущу. Досмотри сериал",
            "Мой прайм уже на паузе",
            "Обещаю, это будет быстро",
            "Чат читаю, но не всё прощаю",
            "Мут — это тоже забота",
            "Я не злой, я по правилам",
            "Кто тут шумит? А, это я",
            "Работаю за спасибо и за чай",
            "Ещё один флуд — и я в отпуске",
            "Спокойствие, только спокойствие",
            "Правила читали? Я так и думал",
            "Модератор тоже человек. Почти",
            "Тишина в чате — моя лучшая награда",
            "Не спорь со мной, я с банхаммером",
            "Сегодня без банов. Наверное",
            "Я всё вижу. Даже твой капс",
            "Отдохни от чата, я подожду",
            "Хорошего дня. Это не просьба",
            "Мут выдан с любовью",
            "Я не сплю, я наблюдаю",
            "Кнопка «бан» такая близкая",
            "Давай без крайностей, ладно?",
            "Улыбнись, тебя видит модератор",
            "Порядок в чате начинается с уважения",
            "Здесь можно спорить, но без капса",
            "Нарушение замечено, терпение тоже",
            "Проверка чата: всё под контролем",
            "Я за диалог, но логи уже открыты",
            "Не торопись, я читаю каждую строку",
            "Тишина — это тоже командная работа",
            "Сегодня у меня режим «всё замечаю»",
            "Пожалуйста, оставьте токсичность в инвентаре",
            "Команда «пожаловаться» быстрее спора",
            "Если что — правила рядом, я дальше",
            "Модерация без паники, но с логами",
            "Чат чистый — у модератора выходной",
            "Проверяю сообщения, а не телепатию",
            "Сначала предупреждение, потом история",
            "Каждой жалобе — своё внимание",
            "Не флуди, оставь место другим",
            "Банхаммер заряжен, но я спокоен",
            "Я видел это сообщение. И предыдущее тоже",
            "Пусть сегодня победит здравый смысл",
    };

    private final MinecraftClient client = MinecraftClient.getInstance();

    /** 0 = hidden, 1 = fully shown; animated on every frame. */
    private float shown;
    private boolean chatStateInitialized;
    private boolean lastChatOpen;
    private boolean transitionOpening;
    private int animationIndex = -1;
    private long transitionStartedAt;
    private float transitionStartShown;
    private float transitionTarget;
    private float transitionProgress;

    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    private boolean placed;
    private int panelX;
    private int panelY;

    private long sessionStart = -1L;
    private int sessionPunishments;

    private long phraseIndexAt = -1L;
    private int phraseIndex;

    private ModeratorHud() {
    }

    /**
     * Called while the chat is open and during the short closing
     * transition. Draws nothing after the panel has fully disappeared.
     */
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {

        boolean enabled = StafftoolsClient.getConfig().isModHudEnabled();
        boolean chatOpen = client.currentScreen instanceof ChatScreen;

        if (!enabled) {

            shown = 0.0F;
            chatStateInitialized = false;
            return;
        }

        if (!tick(chatOpen)) {
            return;
        }

        // the panel follows the cursor; mouseDragged lives on
        // ParentElement and cannot be injected into ChatScreen, so the
        // position is polled every frame instead
        tickDrag(mouseX, mouseY);

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        String nick = nick();
        String server = server();
        String[] phrase = phrase();

        int phraseH = StafftoolsClient.getConfig().isModHudPhrases()
                ? phrase.length * (LINE_H + 1) + 8
                : 0;

        int panelH =
                PAD + HEAD_SIZE + 8 + 4 * (LINE_H + 4) + phraseH + PAD;

        if (!placed) {

            panelX = screenW - PANEL_W - 10;
            panelY = 10;

            placed = true;
        }

        panelX = clamp(panelX, -PANEL_W / 2, screenW - PANEL_W / 2);
        panelY = clamp(panelY, 0, screenH - 20);

        float t = shown;
        int effect = Math.max(0, animationIndex);
        float panelAlpha = t;

        int xShift = switch (effect) {
            case EFFECT_DISINTEGRATE -> Math.round((1.0F - t) * 26.0F);
            case EFFECT_BLOCKS -> Math.round((1.0F - t) * 18.0F);
            case EFFECT_GLITCH -> 0;
            case EFFECT_FADE -> Math.round((1.0F - t) * 14.0F);
            default -> 0;
        };
        int x = panelX + xShift;
        int yOffset = switch (effect) {
            case EFFECT_DISINTEGRATE -> 5;
            case EFFECT_BLOCKS -> 4;
            case EFFECT_FADE -> 8;
            default -> 0;
        };
        int y = panelY - Math.round((1.0F - t) * yOffset);
        float glitchIntensity = 0.0F;

        if (effect == EFFECT_GLITCH && Ui.animationsEnabled()) {
            glitchIntensity = (float) Math.sin(
                    transitionProgress * Math.PI
            );
            long now = System.currentTimeMillis();
            float horizontalWave = (float) (
                    Math.sin(now * 0.014) * 0.68
                            + Math.sin(now * 0.009 + 1.2) * 0.32
            );
            x += Math.round(
                    horizontalWave * glitchIntensity * 3.0F
            );
            y += Math.round(
                    (float) Math.sin(now * 0.011 + 0.7)
                            * glitchIntensity * 1.5F
            );
        }

        float scale = 1.0F;
        if (Ui.animationsEnabled()) {
            float settle = (float) Math.sin(
                    transitionProgress * Math.PI
            ) * 0.008F;
            scale = switch (effect) {
                case EFFECT_DISINTEGRATE -> 0.92F + 0.08F * t + settle;
                case EFFECT_BLOCKS -> 0.95F + 0.05F * t + settle * 0.5F;
                case EFFECT_GLITCH -> 0.99F + 0.01F * t;
                case EFFECT_FADE -> 0.96F + 0.04F * t + settle;
                default -> 1.0F;
            };
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                x + PANEL_W / 2f,
                y + panelH / 2f
        );
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(
                -(x + PANEL_W / 2f),
                -(y + panelH / 2f)
        );

        Ui.drawPopupFrame(context, x, y, PANEL_W, panelH, panelAlpha);

        int textX = x + PAD;

        float headerAlpha = componentAlpha(t, 0.02F);
        drawHeader(context, x, y, nick, headerAlpha);

        int textY = y + PAD + HEAD_SIZE + 8;
        float moderatorAlpha = componentAlpha(t, 0.04F);

        drawRow(context, textX, textY, "\u25C9",
                Lang.t(Key.HUD_MODERATOR), nick,
                Ui.ACCENT_SOFT, moderatorAlpha);
        textY += LINE_H + 4;
        float serverAlpha = componentAlpha(t, 0.08F);

        drawRow(context, textX, textY, "\u25B2",
                Lang.t(Key.HUD_SERVER), server,
                Ui.TEXT, serverAlpha);
        textY += LINE_H + 4;
        float sessionAlpha = componentAlpha(t, 0.12F);

        drawRow(context, textX, textY, "\u23F1",
                Lang.t(Key.HUD_SESSION), sessionTime(),
                Ui.TEXT, sessionAlpha);
        textY += LINE_H + 4;
        float punishmentAlpha = componentAlpha(t, 0.16F);

        drawRow(context, textX, textY, "\u2696",
                Lang.t(Key.HUD_PUNISHED),
                String.valueOf(sessionPunishments),
                sessionPunishments == 0
                        ? Ui.TEXT_DIM
                        : Ui.WARNING,
                punishmentAlpha);

        textY += LINE_H + 6;

        if (phraseH > 0) {
            float phraseAlpha = componentAlpha(t, 0.22F);

            // separator between the data and the phrase
            Ui.drawDivider(
                    context,
                    textX,
                    textY - 4,
                    PANEL_W - PAD * 2,
                    phraseAlpha * 0.8f
            );

            textY += 4;

            for (int i = 0; i < phrase.length; i++) {

                float lineAlpha = componentAlpha(
                        t,
                        0.22F + i * 0.035F
                );

                context.drawTextWithShadow(
                        client.textRenderer,
                        Text.literal(phrase[i]),
                        textX,
                        textY,
                        Ui.argb(Ui.ACCENT_SOFT, lineAlpha)
                );

                textY += LINE_H + 1;
            }
        }

        if (Ui.animationsEnabled()) {
            drawTransitionEffect(
                    context,
                    x,
                    y,
                    panelH,
                    transitionProgress,
                    glitchIntensity
            );
        }

        context.getMatrices().popMatrix();
    }

    private void drawTransitionEffect(
            DrawContext context,
            int x,
            int y,
            int panelH,
            float progress,
            float glitchIntensity
    ) {

        if (animationIndex == EFFECT_DISINTEGRATE) {

            for (int i = 0; i < 16; i++) {

                float pieceProgress = staggeredProgress(
                        progress,
                        i % 7,
                        0.045F
                );
                float detached = transitionOpening
                        ? 1.0F - pieceProgress
                        : pieceProgress;
                float opacity = transitionOpening
                        ? (1.0F - pieceProgress) * 0.82F
                        : pieceProgress * (1.0F - pieceProgress) * 2.2F;

                if (opacity <= 0.01F) {
                    continue;
                }

                double angle = i * 2.399963229728653;
                float travel = detached * (24.0F + i % 3 * 5.0F);
                int fragmentX = x + 8 + i * 29 % (PANEL_W - 20);
                int fragmentY = y + 8
                        + i * 19 % Math.max(1, panelH - 16);
                int offsetX = Math.round((float) Math.cos(angle) * travel);
                int offsetY = Math.round((float) Math.sin(angle) * travel);
                int size = 2 + i % 3;
                int color = i % 2 == 0 ? Ui.ACCENT_SOFT : Ui.ACCENT_2;

                context.fill(
                        fragmentX + offsetX,
                        fragmentY + offsetY,
                        fragmentX + offsetX + size,
                        fragmentY + offsetY + size,
                        Ui.argb(color, opacity)
                );
            }

            return;
        }

        if (animationIndex == EFFECT_BLOCKS) {

            for (int i = 0; i < 10; i++) {

                float blockProgress = staggeredProgress(
                        progress,
                        i % 5,
                        0.055F
                );
                float detached = transitionOpening
                        ? 1.0F - blockProgress
                        : blockProgress;
                float opacity = transitionOpening
                        ? (1.0F - blockProgress) * 0.72F
                        : blockProgress * (1.0F - blockProgress) * 2.0F;

                if (opacity <= 0.01F) {
                    continue;
                }

                int size = 4 + i % 3 * 2;
                int fragmentY = y + 12
                        + i * Math.max(1, panelH - 24) / 10;
                boolean left = i % 2 == 0;
                int travel = Math.round(detached * 18.0F);
                int fragmentX = left
                        ? x - travel
                        : x + PANEL_W - size + travel;
                int color = i % 3 == 0 ? Ui.ACCENT_SOFT : Ui.ACCENT;

                context.fill(
                        fragmentX,
                        fragmentY,
                        fragmentX + size,
                        fragmentY + size,
                        Ui.argb(color, opacity)
                );
            }

            return;
        }

        if (animationIndex == EFFECT_GLITCH &&
                glitchIntensity > 0.01F) {

            int availableHeight = Math.max(1, panelH - 16);
            float easedProgress = smootherstep(progress);

            for (int i = 0; i < 5; i++) {

                float bandPosition = 0.10F + i * 0.16F
                        + easedProgress * 0.12F;
                int bandY = y + 8
                        + Math.round(bandPosition * availableHeight);
                float sliceWave = (float) Math.sin(
                        (progress * 1.6F + i * 0.23F)
                                * Math.PI * 2.0
                );
                int shift = Math.round(
                        sliceWave * glitchIntensity * (4 + i * 2)
                );
                int left = x + Math.max(0, shift);
                int right = x + PANEL_W - Math.max(0, -shift);
                int color = i % 2 == 0 ? Ui.ACCENT_SOFT : Ui.ACCENT_2;

                context.fill(
                        left,
                        bandY,
                        right,
                        bandY + 2 + i % 2,
                        Ui.argb(color, glitchIntensity * 0.48F)
                );
            }
        }
    }

    private static float componentAlpha(float progress, float delay) {
        return smootherstep((progress - delay) / (1.0F - delay));
    }

    private static float staggeredProgress(
            float progress,
            int order,
            float stagger
    ) {
        float delay = order * stagger;
        return smootherstep((progress - delay) / (1.0F - delay));
    }

    private static float smootherstep(float value) {
        float t = Ui.clamp01(value);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    /**
     * Head row: avatar, nickname and a pulsing "on duty" dot. It is the
     * only part that is bold, so the eye lands there first.
     */
    private void drawHeader(
            DrawContext context,
            int x,
            int y,
            String nick,
            float alpha
    ) {

        int headX = x + PAD;
        int headY = y + PAD - 2;

        drawHead(context, headX, headY, alpha);

        int textX = headX + HEAD_SIZE + 6;
        int textY = headY + 1;

        String label = textRenderer().trimToWidth(
                nick.isEmpty()
                        ? Lang.t(Key.HUD_OFFLINE)
                        : nick,
                PANEL_W - PAD * 3 - HEAD_SIZE - 20
        );

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(label),
                textX,
                textY,
                Ui.argb(Ui.TEXT, alpha)
        );

        // small caption under the nick
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(Lang.t(Key.HUD_TITLE)),
                textX,
                textY + 9,
                Ui.argb(Ui.TEXT_MUTED, alpha * 0.9f)
        );

        // live dot: it breathes so it is obvious the panel is active
        float pulse = Ui.animationsEnabled()
                ? 0.55F + 0.45F * Ui.wave(
                        (System.currentTimeMillis() % 2000L) / 2000.0F
                )
                : 1f;

        int dotX = x + PANEL_W - PAD - 5;
        int dotY = headY + HEAD_SIZE / 2 - 2;

        context.fill(
                dotX - 3,
                dotY,
                dotX + 3,
                dotY + 4,
                Ui.argb(Ui.SUCCESS, alpha * pulse)
        );
    }

    /** One data row: icon, dimmed caption, bright value. */
    private void drawRow(
            DrawContext context,
            int x,
            int y,
            String icon,
            String label,
            String value,
            int color,
            float alpha
    ) {

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(icon),
                x,
                y,
                Ui.argb(Ui.TEXT_MUTED, alpha * 0.8f)
        );

        int valueX = x + 10;

        String prefix = label + ": ";

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(prefix),
                valueX,
                y,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        int textW = valueX + textRenderer().getWidth(prefix);

        int max = PANEL_W - PAD * 2 - (textW - x) - 4;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(textRenderer().trimToWidth(value, max)),
                textW,
                y,
                Ui.argb(color, alpha)
        );
    }

    private net.minecraft.client.font.TextRenderer textRenderer() {
        return client.textRenderer;
    }

    /** Advances the transition and its chat-open effect cycle. */
    private boolean tick(boolean chatOpen) {

        long now = System.currentTimeMillis();
        float before = shown;

        if (!chatStateInitialized) {
            chatStateInitialized = true;
            lastChatOpen = chatOpen;
            beginTransition(chatOpen, now);
            if (chatOpen) {
                advanceAnimation();
            }
        } else if (chatOpen != lastChatOpen) {
            lastChatOpen = chatOpen;
            beginTransition(chatOpen, now);
            advanceAnimation();
        }

        transitionProgress = Ui.animationProgress(
                transitionStartedAt,
                TRANSITION_DURATION_MS
        );
        float easedProgress = smootherstep(transitionProgress);
        shown = transitionStartShown
                + (transitionTarget - transitionStartShown) * easedProgress;

        if (transitionProgress >= 1.0F) {
            shown = transitionTarget;
        }

        if (chatOpen) {
            tickSession();
        }

        if (shown <= 0.0F && transitionTarget == 0.0F &&
                transitionProgress >= 1.0F) {
            return false;
        }

        return before != shown || shown > 0.0F;
    }

    private void beginTransition(boolean opening, long now) {
        transitionOpening = opening;
        transitionStartShown = shown;
        transitionTarget = opening ? 1.0F : 0.0F;
        transitionStartedAt = now;
    }

    private void advanceAnimation() {
        animationIndex = (animationIndex + 1) % EFFECT_COUNT;
    }

    /** New session after login or after a server change. */
    private void tickSession() {

        String server = server();

        boolean switched = !server.equals(lastServer);

        if (sessionStart < 0L || switched) {

            sessionStart = System.currentTimeMillis();
            sessionPunishments = 0;

            lastServer = server;

            return;
        }

        sessionPunishments = countSession();
    }

    private String lastServer = "";

    /** Punishments of the current server recorded after the session start. */
    private int countSession() {

        String key = PunishmentHistory.currentServerKey();

        if (key == null || sessionStart < 0L) {
            return 0;
        }

        int count = 0;

        for (PunishmentRecord record : PunishmentHistory.get()
                .getRecordsOf(key)) {

            if (record.getTimestamp() >= sessionStart) {
                count++;
            }
        }

        return count;
    }

    /** Head of the local player from its own skin. */
    private void drawHead(
            DrawContext context,
            int x,
            int y,
            float alpha
    ) {

        if (client.player == null) {
            return;
        }

        Identifier skin = client.player
                .getSkin()
                .body()
                .texturePath();

        int color = Ui.argb(0xFFFFFFFF, alpha);

        // front face of the head, with the hat layer on top
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                skin,
                x,
                y,
                8.0F,
                8.0F,
                8,
                8,
                64,
                64,
                color
        );

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                skin,
                x,
                y,
                40.0F,
                8.0F,
                8,
                8,
                64,
                64,
                color
        );
    }

    private String nick() {

        return client.player == null
                ? Lang.t(Key.HUD_OFFLINE)
                : client.player.getGameProfile().name();
    }

    private String server() {

        var entry = client.getCurrentServerEntry();

        return entry == null
                ? Lang.t(Key.HUD_OFFLINE)
                : entry.name;
    }

    /** Rotating phrase, re-picked every 12 seconds. */
    private String[] phrase() {

        long now = System.currentTimeMillis();

        if (phraseIndexAt < 0L ||
                Ui.animationsEnabled() && now - phraseIndexAt > 12_000L) {

            phraseIndexAt = now;
            phraseIndex = (phraseIndex + 1) % PHRASES.length;
        }

        return wrap(PHRASES[phraseIndex], PANEL_W - PAD * 2);
    }

    /** Word wrap into the panel width. */
    private static String[] wrap(
            String text,
            int maxWidth
    ) {

        var tr = MinecraftClient.getInstance().textRenderer;

        List<String> lines = new ArrayList<>();

        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {

            String candidate = line.isEmpty()
                    ? word
                    : line + " " + word;

            if (tr.getWidth(candidate) <= maxWidth) {

                line = new StringBuilder(candidate);

                continue;
            }

            if (!line.isEmpty()) {

                lines.add(line.toString());

                line = new StringBuilder();
            }

            // a single word longer than the panel
            if (tr.getWidth(word) <= maxWidth) {

                line.append(word);

            } else {

                String rest = word;

                while (tr.getWidth(rest) > maxWidth) {

                    int cut = rest.length();

                    while (cut > 1 && tr.getWidth(rest.substring(0, cut)) > maxWidth) {
                        cut--;
                    }

                    lines.add(rest.substring(0, cut));

                    rest = rest.substring(cut);
                }

                line.append(rest);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines.toArray(new String[0]);
    }

    /** Seconds of the current session, as {@code m:ss} or {@code h:mm:ss}. */
    public String sessionTime() {

        if (sessionStart < 0L) {
            return "0:00";
        }

        long seconds = (System.currentTimeMillis() - sessionStart) / 1000L;

        return format(seconds);
    }

    private static String format(long seconds) {

        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;

        return h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%d:%02d", m, s);
    }

    // ── dragging ────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY) {

        if (shown <= 0.0F) {
            return false;
        }

        if (!inside(mouseX, mouseY)) {
            return false;
        }

        dragging = true;

        dragOffsetX = mouseX - panelX;
        dragOffsetY = mouseY - panelY;

        placed = true;

        return true;
    }

    /**
     * Per-frame drag tracking. The chat screen is rendered every frame,
     * so polling the cursor is enough to move the panel and to notice
     * that the button was released.
     */
    private void tickDrag(
            double mouseX,
            double mouseY
    ) {

        if (!dragging) {
            return;
        }

        panelX = (int) (mouseX - dragOffsetX);
        panelY = (int) (mouseY - dragOffsetY);

        placed = true;

        if (!mouseDown()) {
            dragging = false;
        }
    }

    private boolean mouseDown() {

        var window = client.getWindow();

        if (window == null) {
            return false;
        }

        return GLFW.glfwGetMouseButton(
                window.getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;
    }

    public void mouseReleased() {

        dragging = false;
    }

    private boolean inside(double mouseX, double mouseY) {

        // a slightly generous area: the head and the frame are part of it
        return mouseX >= panelX - 2
                && mouseX <= panelX + PANEL_W + 2
                && mouseY >= panelY - 2
                && mouseY <= panelY + panelHeight();
    }

    /** Hit area of the whole panel, used while dragging. */
    private int panelHeight() {

        return PAD + HEAD_SIZE + 8 + 4 * (LINE_H + 4)
                + (StafftoolsClient.getConfig().isModHudPhrases()
                        ? phrase().length * (LINE_H + 1) + 8
                        : 0) + PAD;
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {

        return Math.max(min, Math.min(max, value));
    }

    /** Drops transition state, e.g. on disconnect. */
    public void reset() {

        shown = 0.0F;
        chatStateInitialized = false;
        lastChatOpen = false;
        transitionOpening = false;
        animationIndex = -1;
        transitionStartedAt = 0L;
        transitionStartShown = 0.0F;
        transitionTarget = 0.0F;
        transitionProgress = 0.0F;
        dragging = false;
        placed = false;
        sessionStart = -1L;
        sessionPunishments = 0;
        phraseIndexAt = -1L;
    }
}