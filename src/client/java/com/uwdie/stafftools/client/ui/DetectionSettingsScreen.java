package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Toggles for every nick-detection method the mod supports.
 * Same visual language as the rest of the mod UI: framed panel, themed
 * rows with entrance stagger, spring scrolling.
 */
public class DetectionSettingsScreen extends StScreen {

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 2;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 40;
    private static final int FADE_H = 12;

    /** Rows live inside the frame, never flush with its border. */
    private static final int ROW_PAD = 9;

    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;

    private final Screen parent;

    private float scroll;
    private float scrollPos;
    private float scrollVel;

    private int listBottom;
    private int rowLeft;
    private int rowWidth;

    /** Rows are rebuilt on every init(), widgets follow them. */
    private final List<ToggleRow> rows = new java.util.ArrayList<>();

    public DetectionSettingsScreen(Screen parent) {

        super(Lang.text(Key.TITLE_DETECTION));
        this.parent = parent;
    }

    private static final class ToggleRow {

        private final String key;
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private StToggle widget;

        private ToggleRow(
                String key,
                Supplier<Boolean> getter,
                Consumer<Boolean> setter
        ) {
            this.key = key;
            this.getter = getter;
            this.setter = setter;
        }
    }

    @Override
    protected void init() {

        resetSlots();

        rows.clear();
        clearChildren();

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        rowWidth = Math.min(260, formWidth());
        rowLeft = (width - rowWidth) / 2;

        listBottom = height - BOTTOM_PAD;

        add(Key.TOGGLE_DETECT_PLAIN,
                config::isDetectPlain,
                config::setDetectPlain);

        add(Key.TOGGLE_DETECT_CACHE,
                config::isDetectCache,
                config::setDetectCache);

        add(Key.TOGGLE_DETECT_DISPLAY,
                config::isDetectDisplay,
                config::setDetectDisplay);

        add(Key.TOGGLE_DETECT_ALIASES,
                config::isDetectAliases,
                config::setDetectAliases);

        add(Key.TOGGLE_DETECT_CLICK,
                config::isDetectClick,
                config::setDetectClick);

        add(Key.TOGGLE_DETECT_HOVER,
                config::isDetectHover,
                config::setDetectHover);

        add(Key.TOGGLE_DETECT_UNICODE,
                config::isDetectUnicode,
                config::setDetectUnicode);

        add(Key.TOGGLE_DETECT_DECOR,
                config::isDetectDecor,
                config::setDetectDecor);

        add(Key.TOGGLE_DETECT_LINE,
                config::isDetectLine,
                config::setDetectLine);

        add(Key.TOGGLE_DETECT_FUZZY,
                config::isDetectFuzzy,
                config::setDetectFuzzy);

        add(Key.TOGGLE_CHAT_INPUT,
                config::isChatInputDetectEnabled,
                config::setChatInputDetectEnabled);

        add(Key.TOGGLE_CHAT_RESIZE,
                config::isChatResizeEnabled,
                config::setChatResizeEnabled);

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < rows.size(); i++) {

            ToggleRow row = rows.get(i);

            row.widget = toggle(
                    rowLeft + ROW_PAD,
                    LIST_TOP + i * step,
                    rowWidth - ROW_PAD * 2,
                    18,
                    row.key,
                    row.getter.get(),
                    row.setter
            );
        }

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;

        button(
                rowLeft,
                height - 32,
                rowWidth,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    private void add(
            String key,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
        rows.add(new ToggleRow(key, getter, setter));
    }

    private int maxScroll() {

        if (rows.isEmpty()) {
            return 0;
        }

        int content =
                rows.size() * ROW_H +
                        (rows.size() - 1) * ROW_GAP;

        return Math.max(
                0,
                content - (listBottom - LIST_TOP)
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        float step = ROW_H + ROW_GAP;

        scroll += verticalAmount > 0 ? -step : step;

        return true;
    }

    private void updateScroll(float delta) {

        float step = Ui.clamp01(delta) * 0.05f;
        float max = maxScroll();

        float target = Math.clamp(
                scroll,
                -SCROLL_RUBBER,
                max + SCROLL_RUBBER
        );

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scrollPos) * SCROLL_STIFF
                            - scrollVel * SCROLL_DAMP;

            float over = scrollPos < 0f
                    ? scrollPos
                    : scrollPos > max
                    ? scrollPos - max
                    : 0f;

            if (over != 0f) {
                accel -= over * EDGE_STIFF;
                accel -= scrollVel * EDGE_DAMP;
            }

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

        drawTitle(context, Key.TITLE_DETECTION, alpha);

        drawHint(context, Key.MSG_DETECT_HINT, 30, alpha);

        Ui.drawPanel(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_DETECT_EMPTY),
                    width / 2,
                    LIST_TOP + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        updateScroll(delta);

        int shift = (int) ((1f - alpha) * 14f);

        context.enableScissor(
                rowLeft + ROW_PAD,
                LIST_TOP,
                rowLeft + rowWidth - ROW_PAD,
                listBottom
        );

        long now = System.currentTimeMillis();

        for (int i = 0; i < rows.size(); i++) {

            ToggleRow row = rows.get(i);

            int y = Math.round(
                    LIST_TOP + i * (ROW_H + ROW_GAP) - scrollPos
            );

            boolean drawable =
                    y + ROW_H > LIST_TOP && y < listBottom;

            if (row.widget != null) {

                row.widget.setY(y);

                row.widget.setVisible(
                        drawable &&
                                y >= LIST_TOP &&
                                y + ROW_H <= listBottom
                );
            }

            if (!drawable) {
                continue;
            }

            float rowAlpha = Ui.clamp01(
                    (now - openTime - i * 22L) / 180f
            );

            if (rowAlpha <= 0.01f) {
                continue;
            }

            // rows are widgets, so only the backdrop is drawn here
            context.fill(
                    rowLeft + ROW_PAD,
                    y,
                    rowLeft + rowWidth - ROW_PAD,
                    y + ROW_H,
                    Ui.argb(Ui.PANEL_BG_SOFT, alpha * rowAlpha * 0.85f)
            );
        }

        context.disableScissor();

        drawScrollMasks(context, alpha);
    }

    private void drawScrollMasks(
            DrawContext context,
            float alpha
    ) {

        float top = Ui.clamp01(scrollPos / FADE_H);
        float bottom = Ui.clamp01(
                (maxScroll() - scrollPos) / FADE_H
        );

        if (top > 0.01f) {

            context.fillGradient(
                    rowLeft + ROW_PAD,
                    LIST_TOP,
                    rowLeft + rowWidth - ROW_PAD,
                    LIST_TOP + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    rowLeft + ROW_PAD,
                    listBottom - FADE_H,
                    rowLeft + rowWidth - ROW_PAD,
                    listBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }

    @Override
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawScrollbar(context, alpha);
    }

    private void drawScrollbar(
            DrawContext context,
            float alpha
    ) {

        int max = maxScroll();

        if (max <= 0) {
            return;
        }

        int trackH = listBottom - LIST_TOP;

        int thumbH = Math.max(
                16,
                (int) (trackH * trackH /
                        (float) (trackH + max))
        );

        int travel = Math.max(1, trackH - thumbH);

        Ui.drawRoundRect(
                context,
                rowLeft + rowWidth - 5,
                LIST_TOP +
                        (int) (travel * (scrollPos / (float) max)),
                3,
                thumbH,
                2,
                Ui.argb(
                        Ui.mix(Ui.ACCENT_2, Ui.TEXT, 0.12f),
                        alpha * 0.7f
                )
        );
    }
}
