package com.uwdie.stafftools.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated toast notifications (bottom-right corner).
 *
 * <p>Each toast slides in, holds while a progress bar drains, then fades
 * out. Two interactions make the stack usable:
 * <ul>
 *   <li><b>Hover</b> freezes the lifetime, so a message never disappears
 *       while it is being read.</li>
 *   <li><b>Click</b> expands the toast into a detail card with the full
 *       text; an expanded toast never expires until it is collapsed.</li>
 * </ul>
 *
 * <p>Rendering happens on screens (via mixins) and directly on the HUD.
 * Clicks are routed in from the screen mixins, because the HUD has no
 * input of its own.
 */
public final class Toasts {

    public static final Toasts INSTANCE = new Toasts();

    private static final int MAX = 4;

    private static final long IN_MS = 200;
    private static final long HOLD_MS = 2600;
    private static final long OUT_MS = 280;

    private static final int H = 18;
    private static final int GAP = 4;
    private static final int PAD = 8;

    /** Kind of a toast; drives the accent and the caption. */
    public enum Kind {
        INFO("info"),
        SUCCESS("success"),
        WARN("warn"),
        DANGER("danger");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public int accent() {
            return switch (this) {
                case INFO -> Ui.ACCENT_SOFT;
                case SUCCESS -> Ui.SUCCESS;
                case WARN -> Ui.WARNING;
                case DANGER -> Ui.DANGER;
            };
        }
    }

    /** Mutable toast state. */
    private static final class Toast {

        private final String text;
        private final Kind kind;

        /** Start of the current lifetime; reset when a toast collapses. */
        private long born;

        /** Total time the toast spent frozen by hover. */
        private long pausedMs;

        /** When the current hover pause started, 0 when not paused. */
        private long pauseStart;

        private boolean expanded;

        /** 0..1 expansion animation. */
        private float expandAmt;

        /** 0..1 hover treatment. */
        private float hoverAmt;

        private boolean hovering;

        private Toast(String text, Kind kind, long born) {
            this.text = text;
            this.kind = kind;
            this.born = born;
        }

        private long age(long now) {

            long paused = pausedMs;

            if (pauseStart > 0) {
                paused += now - pauseStart;
            }

            return now - born - paused;
        }

        /** Restarts the lifetime, used when an expanded toast collapses. */
        private void restart(long now) {
            this.born = now;
            this.pausedMs = 0;
            this.pauseStart = 0;
        }
    }

    private final List<Toast> active = new ArrayList<>();

    /** Screen-space bounds of the last rendered toasts, for hit-testing. */
    private final List<int[]> hitboxes = new ArrayList<>();

    private long lastFrameAt;

    private Toasts() {
    }

    public void info(String text) {
        push(text, Kind.INFO);
    }

    public void success(String text) {
        push(text, Kind.SUCCESS);
    }

    public void warn(String text) {
        push(text, Kind.WARN);
    }

    public void danger(String text) {
        push(text, Kind.DANGER);
    }

    private void push(String text, Kind kind) {

        if (text == null || text.isBlank()) {
            return;
        }

        if (!com.uwdie.stafftools.client.StafftoolsClient
                .getConfig()
                .isToastsEnabled()) {

            return;
        }

        while (active.size() >= MAX) {
            active.remove(0);
        }

        active.add(
                new Toast(
                        text,
                        kind,
                        System.currentTimeMillis()
                )
        );

        // distinct cue per notification kind
        switch (kind) {
            case SUCCESS -> UiSound.success();
            case WARN -> UiSound.warn();
            case DANGER -> UiSound.danger();
            case INFO -> UiSound.tick();
        }
    }

    /**
     * Routes a click into the toast stack.
     *
     * @return true when a toast consumed the click
     */
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (button != 0 || active.isEmpty()) {
            return false;
        }

        for (int i = 0; i < hitboxes.size(); i++) {

            int[] box = hitboxes.get(i);

            if (mouseX < box[0] || mouseX > box[0] + box[2] ||
                    mouseY < box[1] || mouseY > box[1] + box[3]) {

                continue;
            }

            if (i >= active.size()) {
                continue;
            }

            Toast toast = active.get(i);

            // clicking an expanded toast collapses it, clicking a
            // collapsed one expands it
            toast.expanded = !toast.expanded;

            if (!toast.expanded) {
                // a collapsed toast gets a fresh lifetime instead of
                // vanishing the instant it is closed
                toast.restart(System.currentTimeMillis());
            }

            Ui.playHover();

            return true;
        }

        // a click anywhere else collapses every expanded toast, but the
        // click itself still reaches the screen underneath
        long now = System.currentTimeMillis();

        for (Toast toast : active) {

            if (toast.expanded) {
                toast.expanded = false;
                toast.restart(now);
            }
        }

        return false;
    }

    public void render(DrawContext context) {

        if (active.isEmpty()) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        var tr = client.textRenderer;

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        long now = System.currentTimeMillis();

        float dt = lastFrameAt == 0L
                ? 0f
                : Math.min(0.1f, (now - lastFrameAt) / 1000f);

        lastFrameAt = now;

        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();

        // drop expired toasts (expanded ones never expire)
        active.removeIf(t ->
                !t.expanded &&
                        t.age(now) > IN_MS + HOLD_MS + OUT_MS);

        hitboxes.clear();

        int maxW = Math.max(120, screenWidth / 2);

        // stack from the bottom edge upward
        int y = screenHeight - 8;

        for (Toast t : active) {

            long age = t.age(now);

            float in = Ui.animationsEnabled()
                    ? Ui.easeOutCubic(age / (float) IN_MS)
                    : 1f;

            float out = Ui.animationsEnabled()
                    ? Ui.easeInCubic(
                            (age - IN_MS - HOLD_MS) / (float) OUT_MS
                    )
                    : 0f;

            // an expanded toast is pinned: it never fades out
            float a = t.expanded
                    ? Math.max(in, 0.999f)
                    : Math.min(in, 1f - out);

            // expansion animation
            float target = t.expanded ? 1f : 0f;

            if (Ui.animationsEnabled()) {
                t.expandAmt = Ui.animateTowardsSeconds(
                        t.expandAmt,
                        target,
                        dt,
                        12f
                );
            } else {
                t.expandAmt = target;
            }

            if (Math.abs(target - t.expandAmt) < 0.002f) {
                t.expandAmt = target;
            }

            float expand = Ui.easeOutCubic(t.expandAmt);

            int collapsedW = Math.min(
                    tr.getWidth(t.text) + 26,
                    maxW
            );

            int expandedW = Math.min(
                    Math.max(collapsedW, 220),
                    maxW
            );

            int w = (int) (collapsedW +
                    (expandedW - collapsedW) * expand);

            List<String> lines = expand > 0.05f
                    ? wrap(tr, t.text, w - PAD * 2 - 6)
                    : List.of();

            int expandedH = Math.max(
                    H,
                    PAD * 2 + 10 + lines.size() * 10
            );

            int h = (int) (H + (expandedH - H) * expand);

            if (a <= 0.01f && expand <= 0.01f) {
                continue;
            }

            float slide = Ui.animationsEnabled()
                    ? (1f - in) * 20f
                            + out * 10f
                    : 0f;

            int x = screenWidth - w - 8 + (int) slide;

            int top = y - h;

            // hover detection for the pause
            boolean over =
                    mouseX >= x && mouseX <= x + w &&
                            mouseY >= top && mouseY <= top + h;

            if (over && !t.hovering) {
                t.hovering = true;
                t.pauseStart = now;
            } else if (!over && t.hovering) {
                t.hovering = false;

                if (t.pauseStart > 0) {
                    t.pausedMs += now - t.pauseStart;
                    t.pauseStart = 0;
                }
            }

            t.hoverAmt = Ui.animateTowardsSeconds(
                    t.hoverAmt,
                    over ? 1f : 0f,
                    dt,
                    16f
            );

            hitboxes.add(new int[]{x, top, w, h});

            drawToast(
                    context,
                    tr,
                    t,
                    x,
                    top,
                    w,
                    h,
                    a,
                    expand,
                    lines,
                    t.hoverAmt
            );

            y = top - GAP;
        }
    }

    private void drawToast(
            DrawContext context,
            net.minecraft.client.font.TextRenderer tr,
            Toast t,
            int x,
            int y,
            int w,
            int h,
            float a,
            float expand,
            List<String> lines,
            float hover
    ) {

        int accent = t.kind.accent();
        float hoverAmount = Ui.clamp01(hover);
        float scale = Ui.animationsEnabled()
                ? 0.96f + 0.04f * Ui.easeOutCubic(a)
                : 1f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                x + w / 2f,
                y + h / 2f
        );
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(
                -(x + w / 2f),
                -(y + h / 2f)
        );

        int body = Ui.mix(
                Ui.PANEL_BG,
                accent,
                0.10f * hoverAmount
        );

        Ui.drawRoundRect(
                context, x, y, w, h, 3,
                Ui.argb(body, a)
        );

        Ui.drawRoundBorder(
                context, x, y, w, h, 3,
                Ui.argb(
                        Ui.mix(0xFF2C3B55, accent, 0.6f * hoverAmount),
                        a
                )
        );

        Ui.drawHoverGlow(
                context, x, y, w, h, 3,
                accent, hoverAmount, a
        );

        // accent side bar
        context.fill(
                x, y + 2, x + 2, y + h - 2,
                Ui.argb(accent, a)
        );

        if (expand < 0.5f) {

            // collapsed: one trimmed line
            String text = tr.trimToWidth(t.text, w - 26);

            context.drawTextWithShadow(
                    tr,
                    Text.literal(text),
                    x + PAD,
                    y + (H - 9) / 2,
                    Ui.argb(Ui.TEXT, a)
            );

        } else {

            // expanded: caption + wrapped body
            context.drawTextWithShadow(
                    tr,
                    Text.literal(t.kind.id().toUpperCase()),
                    x + PAD,
                    y + PAD - 2,
                    Ui.argb(accent, a)
            );

            int lineY = y + PAD + 10;

            for (String line : lines) {

                context.drawTextWithShadow(
                        tr,
                        Text.literal(line),
                        x + PAD,
                        lineY,
                        Ui.argb(Ui.TEXT, a)
                );

                lineY += 10;
            }
        }

        // lifetime progress bar along the bottom (frozen while paused)
        if (!t.expanded) {

            long total = IN_MS + HOLD_MS + OUT_MS;

            float life = Ui.animationsEnabled()
                    ? Ui.clamp01(
                            1f - t.age(System.currentTimeMillis())
                                    / (float) total
                    )
                    : 1f;

            int barW = (int) ((w - 6) * life);

            if (barW > 0) {

                context.fill(
                        x + 3,
                        y + h - 2,
                        x + 3 + barW,
                        y + h - 1,
                        Ui.argb(accent, a * 0.85f)
                );
            }
        }

        context.getMatrices().popMatrix();
    }

    /** Word wrap into the toast width. */
    private static List<String> wrap(
            net.minecraft.client.font.TextRenderer tr,
            String text,
            int maxWidth
    ) {

        List<String> lines = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {

            String candidate = current.length() == 0
                    ? word
                    : current + " " + word;

            if (tr.getWidth(candidate) <= maxWidth) {

                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }

            while (tr.getWidth(word) > maxWidth
                    && word.length() > 1) {

                word = word.substring(0, word.length() - 1);
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }
}
