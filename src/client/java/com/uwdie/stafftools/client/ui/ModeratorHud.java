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
 * Small moderator panel that fades in together with the chat.
 *
 * Shows who is on duty, where, how long the session runs and how many
 * punishments were issued during it, plus one rotating funny phrase.
 * The whole panel can be dragged anywhere by its header.
 */
public final class ModeratorHud {

    public static final ModeratorHud INSTANCE = new ModeratorHud();

    private static final int PANEL_W = 168;
    private static final int PAD = 8;
    private static final int LINE_H = 10;
    private static final int HEAD_SIZE = 20;

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
    };

    private final MinecraftClient client = MinecraftClient.getInstance();

    /** 0 = hidden, 1 = fully shown; animated on every frame. */
    private float shown;
    private long lastFrame;

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
     * Called from the chat screen render. Draws nothing when the panel
     * is disabled or the chat is closed and the fade-out already ended.
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
                ? phrase.length * (LINE_H + 1)
                : 0;

        int panelH =
                PAD + HEAD_SIZE + 4 + 4 * (LINE_H + 3) + 5 + phraseH + PAD;

        if (!placed) {

            panelX = screenW - PANEL_W - 10;
            panelY = 10;

            placed = true;
        }

        panelX = clamp(panelX, -PANEL_W / 2, screenW - PANEL_W / 2);
        panelY = clamp(panelY, 0, screenH - 20);

        float t = Ui.easeOutCubic(shown);

        // slide in from the right, fade out in place
        int x = panelX + (int) ((1.0F - t) * 16.0F);
        int y = panelY - (int) ((1.0F - t) * 6.0F);

        Ui.drawPanel(context, x, y, PANEL_W, panelH, shown);
        Ui.drawOrbitFrame(context, x, y, PANEL_W, panelH, shown);

        int textX = x + PAD;

        drawHeader(context, x, y, nick, shown);

        int textY = y + PAD + HEAD_SIZE + 4;

        drawRow(context, textX, textY, "\u25CF",
                Lang.t(Key.HUD_MODERATOR), nick,
                Ui.ACCENT_2, shown);
        textY += LINE_H + 3;

        drawRow(context, textX, textY, "\u25B2",
                Lang.t(Key.HUD_SERVER), server,
                Ui.TEXT, shown);
        textY += LINE_H + 3;

        drawRow(context, textX, textY, "\u23F1",
                Lang.t(Key.HUD_SESSION), sessionTime(),
                Ui.TEXT, shown);
        textY += LINE_H + 3;

        drawRow(context, textX, textY, "\u2696",
                Lang.t(Key.HUD_PUNISHED),
                String.valueOf(sessionPunishments),
                sessionPunishments == 0
                        ? Ui.TEXT_DIM
                        : Ui.WARNING,
                shown);

        textY += LINE_H + 5;

        if (phraseH > 0) {

            // separator between the data and the phrase
            Ui.drawHeader(
                    context,
                    textX,
                    textY - 4,
                    PANEL_W - PAD * 2,
                    shown * 0.6f
            );

            textY += 2;

            for (String line : phrase) {

                context.drawTextWithShadow(
                        client.textRenderer,
                        Text.literal(line),
                        textX,
                        textY,
                        Ui.argb(Ui.ACCENT_2, shown)
                );

                textY += LINE_H + 1;
            }
        }
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
        float pulse = 0.55F + 0.45F * Ui.wave(
                (System.currentTimeMillis() % 2000L) / 2000.0F
        );

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

    /**
     * Advances the fade animation. Returns false when there is nothing
     * left to draw.
     */
    private boolean tick(boolean chatOpen) {

        long now = System.currentTimeMillis();

        float dt = lastFrame == 0L
                ? 0.0F
                : Math.min(0.1F, (now - lastFrame) / 1000.0F);

        lastFrame = now;

        float target = chatOpen ? 1.0F : 0.0F;

        float before = shown;

        shown += (target - shown) * Math.min(1.0F, dt * 9.0F);

        if (Math.abs(target - shown) < 0.01F) {
            shown = target;
        }

        if (chatOpen) {
            tickSession();
        }

        if (shown <= 0.0F && target == 0.0F) {
            return false;
        }

        return before != shown || shown > 0.0F;
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

        if (phraseIndexAt < 0L || now - phraseIndexAt > 12_000L) {

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

        return PAD + HEAD_SIZE + 4 + 4 * (LINE_H + 3) + 5
                + (StafftoolsClient.getConfig().isModHudPhrases()
                        ? phrase().length * (LINE_H + 1)
                        : 0) + PAD;
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {

        return Math.max(min, Math.min(max, value));
    }

    /** Drops the fade state, e.g. on disconnect. */
    public void reset() {

        shown = 0.0F;
        dragging = false;
        placed = false;
        sessionStart = -1L;
        sessionPunishments = 0;
        phraseIndexAt = -1L;
    }
}