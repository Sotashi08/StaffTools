package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.audit.AuditCategory;
import com.uwdie.stafftools.client.audit.AuditLevel;
import com.uwdie.stafftools.client.audit.AuditLog;
import com.uwdie.stafftools.client.audit.Profiler;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug screen with two panels: the audit journal on the left and the
 * profiler on the right.
 *
 * <p>The journal lists everything the mod did — macro runs, detections,
 * punishment outcomes, settings changes — newest first, filterable by
 * category. The profiler shows per-section timings and the most recent
 * samples, so a slow chat line or a heavy render path is visible without
 * leaving the game.
 */
public class AuditScreen extends StScreen {

    private static final int TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int GAP = 8;
    private static final int PAD = 8;
    private static final int ROW_H = 14;
    private static final int FADE_H = 12;

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final Screen parent;

    private int leftX;
    private int leftW;
    private int rightX;
    private int rightW;
    private int listBottom;
    private int filterRows;

    private float scroll;
    private float scrollPos;
    private float scrollVel;

    /** Active category filter, null = all. */
    private AuditCategory filter;

    /** Filter chip hitboxes, rebuilt every frame. */
    private final List<Object[]> filterChips = new ArrayList<>();

    public AuditScreen(Screen parent) {
        super(Lang.text(Key.TITLE_AUDIT));
        this.parent = parent;
    }

    @Override
    protected void init() {

        resetSlots();

        configureShell();

        int total = contentWidth();

        leftW = (int) (total * 0.58);
        rightW = total - leftW - GAP;

        leftX = contentLeft();
        rightX = leftX + leftW + GAP;

        listBottom = contentBottom() - 34;

        int half = (total - GAP) / 2;

        button(
                leftX,
                contentBottom() - 26,
                half,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(
                        parent == null
                                ? new StafftoolsScreen()
                                : parent
                )
        );

        button(
                leftX + half + GAP,
                contentBottom() - 26,
                half,
                22,
                Key.AUDIT_CLEAR,
                b -> {
                    AuditLog.get().clear();
                    Profiler.get().reset();
                    scroll = 0;
                    scrollPos = 0;
                    scrollVel = 0;
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

        if (mouseX >= leftX && mouseX <= leftX + leftW &&
                mouseY >= listTop() && mouseY < listBottom) {

            float step = ROW_H;

            scroll += verticalAmount > 0 ? -step : step;

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        if (click.button() == 0) {

            for (Object[] chip : filterChips) {

                int x = (int) chip[0];
                int y = (int) chip[1];
                int w = (int) chip[2];

                if (click.x() >= x && click.x() <= x + w &&
                        click.y() >= y && click.y() <= y + 14) {

                    filter = (AuditCategory) chip[3];

                    scroll = 0;
                    scrollPos = 0;
                    scrollVel = 0;

                    Ui.playHover();

                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private int maxScroll(int rows) {

        int content = rows * ROW_H;

        return Math.max(0, content - (listBottom - listTop()));
    }

    private int listTop() {
        return contentTop() + 42 + filterRows * 18 + 2;
    }

    private void updateScroll(float delta, int rows) {

        float step = Ui.clamp01(delta) * 0.05f;
        float max = maxScroll(rows);

        float target = Math.clamp(scroll, 0, max);

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scrollPos) * 240f
                            - scrollVel * 19f;

            scrollVel += accel * s;
            scrollPos += scrollVel * s;
        }

        if (Math.abs(scrollPos - target) < 0.05f &&
                Math.abs(scrollVel) < 0.05f) {

            scrollPos = target;
            scrollVel = 0f;
        }
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        Ui.drawSectionTitle(
                context,
                textRenderer,
                contentLeft(),
                contentTop(),
                "\uD83D\uDD0E",
                Lang.t(Key.TITLE_AUDIT),
                Lang.t(Key.MSG_AUDIT_HINT),
                alpha
        );

        drawJournal(context, mouseX, mouseY, delta, alpha);

        drawProfiler(context, alpha);
    }

    // ------------------------------------------------------------------
    // journal
    // ------------------------------------------------------------------

    private void drawJournal(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        int panelTop = contentTop() + 34;
        int panelHeight = Math.max(0, listBottom - panelTop);

        Ui.drawPanel(
                context,
                leftX,
                panelTop,
                leftW,
                panelHeight,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                leftX,
                panelTop,
                leftW,
                panelHeight,
                alpha
        );

        drawFilterChips(context, alpha);

        List<AuditLog.Entry> entries =
                AuditLog.get().snapshot(filter);

        if (entries.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_AUDIT_EMPTY),
                    leftX + leftW / 2,
                    listTop() + 20,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        updateScroll(delta, entries.size());

        context.enableScissor(
                leftX + PAD,
                listTop(),
                leftX + leftW - PAD,
                listBottom
        );

        int innerW = leftW - PAD * 2;

        for (int i = 0; i < entries.size(); i++) {

            int y = listTop() + i * ROW_H - Math.round(scrollPos);

            if (y + ROW_H < listTop() || y > listBottom) {
                continue;
            }

            drawEntry(
                    context,
                    entries.get(i),
                    leftX + PAD,
                    y,
                    innerW,
                    alpha
            );
        }

        context.disableScissor();

        drawScrollMasks(context, alpha);
    }

    private void drawFilterChips(
            DrawContext context,
            float alpha
    ) {

        filterChips.clear();

        int x = leftX + PAD;
        int y = contentTop() + 42;
        int startX = x;
        int right = leftX + leftW - PAD;

        filterRows = 1;

        List<AuditCategory> categories = new ArrayList<>();
        categories.add(null);
        for (AuditCategory category : AuditCategory.values()) {
            categories.add(category);
        }

        for (AuditCategory category : categories) {

            String label = category == null
                    ? Lang.t(Key.AUDIT_FILTER_ALL)
                    : categoryLabel(category);

            int width = chipWidth(label);

            if (x > startX && x + width > right) {
                x = startX;
                y += 18;
                filterRows++;
            }

            x = drawChip(
                    context,
                    x,
                    y,
                    label,
                    category,
                    alpha
            );
        }
    }

    private int drawChip(
            DrawContext context,
            int x,
            int y,
            String label,
            AuditCategory category,
            float alpha
    ) {

        int w = Math.min(
                chipWidth(label),
                Math.max(1, leftW - PAD * 2)
        );

        boolean active = filter == category;

        int accent = category == null
                ? Ui.ACCENT
                : category.color();

        Ui.drawRoundRect(
                context, x, y, w, 14, Design.RADIUS_SM,
                Ui.argb(
                        active
                                ? Ui.mix(Ui.PANEL_BG, accent, 0.35f)
                                : Ui.mix(Ui.PANEL_BG, Ui.TEXT, 0.03f),
                        alpha
                )
        );

        Ui.drawRoundBorder(
                context, x, y, w, 14, Design.RADIUS_SM,
                Ui.argb(
                        active
                                ? accent
                                : Ui.mix(Ui.PANEL_BORDER, Ui.TEXT, 0.08f),
                        alpha * (active ? 1f : 0.6f)
                )
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(label, Math.max(0, w - 10))
                ),
                x + 5,
                y + 3,
                Ui.argb(active ? Ui.TEXT : Ui.TEXT_DIM, alpha)
        );

        filterChips.add(new Object[]{x, y, w, category});

        return x + w + 4;
    }

    private int chipWidth(String label) {
        return textRenderer.getWidth(label) + 10;
    }

    private void drawEntry(
            DrawContext context,
            AuditLog.Entry entry,
            int x,
            int y,
            int w,
            float alpha
    ) {

        int accent = entry.category().color();

        // level stripe on the left
        context.fill(
                x,
                y + 1,
                x + 2,
                y + ROW_H - 2,
                Ui.argb(entry.level().color(), alpha)
        );

        String time = TIME.format(
                Instant.ofEpochMilli(entry.at())
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(time),
                x + 6,
                y + 2,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        String badge = categoryLabel(entry.category());

        int badgeX = x + 6 + textRenderer.getWidth(time) + 6;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(badge),
                badgeX,
                y + 2,
                Ui.argb(accent, alpha)
        );

        int msgX = badgeX + textRenderer.getWidth(badge) + 6;

        int msgW = Math.max(20, x + w - msgX);

        String message = textRenderer.trimToWidth(
                entry.message(),
                msgW
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(message),
                msgX,
                y + 2,
                Ui.argb(
                        entry.level() == AuditLevel.ERROR
                                ? Ui.DANGER_SOFT
                                : Ui.TEXT,
                        alpha
                )
        );
    }

    private void drawScrollMasks(
            DrawContext context,
            float alpha
    ) {

        int max = maxScroll(AuditLog.get().snapshot(filter).size());

        float top = Ui.clamp01(scrollPos / FADE_H);
        float bottom = Ui.clamp01((max - scrollPos) / FADE_H);

        Ui.drawScrollFade(
                context,
                leftX + PAD,
                listTop(),
                leftW - PAD * 2,
                listBottom,
                top,
                bottom,
                alpha
        );
    }

    // ------------------------------------------------------------------
    // profiler
    // ------------------------------------------------------------------

    private void drawProfiler(
            DrawContext context,
            float alpha
    ) {

        int panelTop = contentTop() + 34;
        int panelHeight = Math.max(0, listBottom - panelTop);

        Ui.drawPanel(
                context,
                rightX,
                panelTop,
                rightW,
                panelHeight,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                rightX,
                panelTop,
                rightW,
                panelHeight,
                alpha
        );

        context.enableScissor(
                rightX + PAD,
                panelTop + 4,
                rightX + rightW - PAD,
                listBottom
        );

        int x = rightX + PAD;
        int y = contentTop() + 42;

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.AUDIT_TAB_PROFILE),
                x,
                y,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        y += 14;

        List<Profiler.Section> sections =
                Profiler.get().sections();

        if (sections.isEmpty()) {

            context.drawTextWithShadow(
                    textRenderer,
                    Lang.text(Key.PROF_EMPTY),
                    x,
                    y,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            context.disableScissor();
            return;
        }

        int innerW = Math.max(1, rightW - PAD * 2);
        int columnW = Math.max(1, Math.min(42, innerW / 4));
        int columnGap = 3;

        // column headers
        String calls = Lang.t(Key.PROF_CALLS);
        String avg = Lang.t(Key.PROF_AVG);
        String peak = Lang.t(Key.PROF_MAX);

        int peakX = x + innerW - columnW;
        int avgX = peakX - columnGap - columnW;
        int callsX = avgX - columnGap - columnW;
        int sectionWidth = Math.max(0, callsX - x - 4);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth(calls, columnW)),
                callsX, y, Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth(avg, columnW)),
                avgX, y, Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth(peak, columnW)),
                peakX, y, Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(
                                Lang.t(Key.PROF_SECTION),
                                sectionWidth
                        )
                ),
                x,
                y,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        y += 12;

        Ui.drawHeader(context, x, y, innerW, alpha * 0.5f);

        y += 4;

        double total = Math.max(0.001, Profiler.get().totalMillis());

        int maxRows = Math.max(
                0,
                (listBottom - y - 60) / 12
        );

        int shown = 0;

        for (Profiler.Section section : sections) {

            if (shown >= maxRows) {
                break;
            }

            // share bar: how much of the total time this section takes
            float share = (float) (section.totalMillis() / total);

            context.fill(
                    x,
                    y + 9,
                    x + innerW,
                    y + 10,
                    Ui.argb(0xFF1A1F28, alpha)
            );

            context.fill(
                    x,
                    y + 9,
                    x + (int) (innerW * Ui.clamp01(share)),
                    y + 10,
                    Ui.argb(Ui.ACCENT, alpha * 0.7f)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(
                                    section.name(),
                                    sectionWidth
                            )
                    ),
                    x,
                    y,
                    Ui.argb(Ui.TEXT, alpha)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(
                                    String.valueOf(section.calls()),
                                    columnW
                            )
                    ),
                    callsX,
                    y,
                    Ui.argb(Ui.TEXT_DIM, alpha)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(textRenderer.trimToWidth(
                            String.format("%.2f", section.avgMicros()),
                            columnW
                    )),
                    avgX,
                    y,
                    Ui.argb(Ui.TEXT_DIM, alpha)
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(textRenderer.trimToWidth(
                            String.format("%.1f", section.maxMicros()),
                            columnW
                    )),
                    peakX,
                    y,
                    Ui.argb(Ui.WARNING, alpha)
            );

            y += 12;
            shown++;
        }

        // recent samples
        y += 6;

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.PROF_TRACE),
                x,
                y,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        y += 12;

        List<Profiler.Sample> trace = Profiler.get().trace();

        int traceRows = Math.max(0, (listBottom - y - 4) / 10);

        for (int i = 0; i < Math.min(traceRows, trace.size()); i++) {

            Profiler.Sample sample = trace.get(i);

            String line = TIME.format(
                    Instant.ofEpochMilli(sample.at())
            ) + "  " + sample.section() + "  "
                    + String.format("%.2f ms",
                    sample.nanos() / 1_000_000.0);

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(line, innerW)
                    ),
                    x,
                    y,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            y += 10;
        }

        context.disableScissor();
    }

    private static String categoryLabel(AuditCategory category) {

        return switch (category) {
            case MACRO -> Lang.t(Key.AUDIT_CAT_MACRO);
            case DETECT -> Lang.t(Key.AUDIT_CAT_DETECT);
            case PUNISH -> Lang.t(Key.AUDIT_CAT_PUNISH);
            case CONFIG -> Lang.t(Key.AUDIT_CAT_CONFIG);
            case CHAT -> Lang.t(Key.AUDIT_CAT_CHAT);
            case ERROR -> Lang.t(Key.AUDIT_CAT_ERROR);
        };
    }
}
