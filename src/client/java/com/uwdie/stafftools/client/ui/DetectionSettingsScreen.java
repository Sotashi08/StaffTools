package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Toggles for every nick-detection method the mod supports, plus the
 * chat-input and chat-resize switches. Same list chrome as the other
 * screens: framed viewport, spring scrolling, edge fades.
 */
public class DetectionSettingsScreen extends StListScreen {

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 2;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 40;

    private final Screen parent;

    /** Rows are rebuilt on every init(), widgets follow them. */
    private final List<ToggleRow> rows = new ArrayList<>();

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

        configureShell();

        StaffToolsConfig config = StafftoolsClient.getConfig();

        rowWidth = Math.min(440, contentWidth());
        rowLeft = contentLeft();

        listTop = contentTop() + 40;
        listBottom = contentBottom() - 40;

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

        add(Key.TOGGLE_CHAT_INPUT,
                config::isChatInputDetectEnabled,
                config::setChatInputDetectEnabled);

        add(Key.TOGGLE_CHAT_RESIZE,
                config::isChatResizeEnabled,
                config::setChatResizeEnabled);

        for (int i = 0; i < rows.size(); i++) {

            ToggleRow row = rows.get(i);

            row.widget = toggle(
                    rowLeft + ROW_PAD,
                    listTop + i * rowStep(),
                    rowWidth - ROW_PAD * 2,
                    18,
                    row.key,
                    row.getter.get(),
                    row.setter
            );
        }

        resetScroll();

        button(
                rowLeft,
                contentBottom() - 26,
                rowWidth,
                Design.H_BUTTON_LG,
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

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected int rowStep() {
        return ROW_H + ROW_GAP;
    }

    @Override
    protected int rowHeight() {
        return ROW_H;
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
                "\u25C8",
                Lang.t(Key.TITLE_DETECTION),
                Lang.t(Key.MSG_DETECT_HINT),
                alpha
        );

        renderList(context, mouseX, mouseY, delta, alpha);

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_DETECT_EMPTY),
                    rowLeft + rowWidth / 2,
                    listTop + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    @Override
    protected void renderRows(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        for (int i = 0; i < rows.size(); i++) {

            ToggleRow row = rows.get(i);

            int y = rowY(i);

            boolean drawable = rowVisible(i);

            if (row.widget != null) {

                row.widget.setY(y);

                row.widget.setVisible(
                        drawable &&
                                y >= listTop &&
                                y + ROW_H <= listBottom
                );
            }

            if (!drawable) {
                continue;
            }

            float rowAlpha = rowAlpha(i);

            if (rowAlpha <= 0.01f) {
                continue;
            }

            // rows are widgets, so only the backdrop is drawn here
            Ui.drawCard(
                    context,
                    rowLeft + ROW_PAD,
                    y,
                    rowWidth - ROW_PAD * 2,
                    ROW_H,
                    alpha * rowAlpha
            );
        }
    }
}
