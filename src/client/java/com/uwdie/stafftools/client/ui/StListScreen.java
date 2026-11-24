package com.uwdie.stafftools.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Base class for the mod's scrollable list screens.
 *
 * <p>Macro list, actions, profiles and detection settings all share the
 * same behaviour: a framed viewport, spring scrolling with a rubber band
 * at the ends, edge fades, a slim scrollbar and rows that fade in with a
 * stagger. That behaviour lives here once, so every list feels identical
 * and a fix applies everywhere.
 *
 * <p>Subclasses provide the row count and draw their own rows; this class
 * owns the viewport geometry, scrolling and chrome.
 */
public abstract class StListScreen extends StScreen {

    /** Spring tuning shared by every list. */
    protected static final float SCROLL_STIFF = 240f;
    protected static final float SCROLL_DAMP = 19f;
    protected static final float SCROLL_RUBBER = 14f;
    protected static final float EDGE_STIFF = 420f;
    protected static final float EDGE_DAMP = 30f;

    /** Height of the fade at the top and bottom of the viewport. */
    protected static final int FADE_H = 12;

    /** Rows live inside the frame, never flush with its border. */
    protected static final int ROW_PAD = 9;

    protected float scroll;
    protected float scrollPos;
    protected float scrollVel;

    protected int listTop;
    protected int listBottom;
    protected int rowLeft;
    protected int rowWidth;

    protected StListScreen(Text title) {
        super(title);
    }

    /** Number of rows currently in the list. */
    protected abstract int rowCount();

    /** Height of one row including the gap to the next. */
    protected abstract int rowStep();

    /** Draws the rows; the viewport is already clipped. */
    protected abstract void renderRows(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    );

    /** Total scrollable height of the content. */
    protected int contentHeight() {

        int count = rowCount();

        if (count <= 0) {
            return 0;
        }

        return count * rowStep() - (rowStep() - rowHeight());
    }

    /** Height of a single row without the gap. */
    protected int rowHeight() {
        return rowStep();
    }

    protected int maxScroll() {

        return Math.max(
                0,
                contentHeight() - (listBottom - listTop)
        );
    }

    /** Resets the scroll position, e.g. after a rebuild. */
    protected void resetScroll() {

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        scroll += verticalAmount > 0
                ? -rowStep()
                : rowStep();

        return true;
    }

    /**
     * Spring integration: slightly underdamped, so every notch settles
     * with a small overshoot, plus a rubber band at the ends.
     */
    protected void updateScroll(float delta) {

        float max = maxScroll();
        if (!Ui.animationsEnabled()) {
            scroll = Math.clamp(scroll, 0f, max);
            scrollPos = scroll;
            scrollVel = 0f;
            return;
        }

        float step = Ui.clamp01(delta) * 0.05f;

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

    /** Y of a row's top edge, in screen space. */
    protected int rowY(int index) {

        return listTop + index * rowStep()
                - Math.round(scrollPos);
    }

    /** True when the row is at least partially inside the viewport. */
    protected boolean rowVisible(int index) {

        int y = rowY(index);

        return y + rowHeight() > listTop && y < listBottom;
    }

    /** Row index under the cursor, or -1. */
    protected int rowIndexAt(double mouseY) {

        if (mouseY < listTop || mouseY > listBottom) {
            return -1;
        }

        int index = (int) Math.floor(
                (mouseY - listTop + scrollPos) / rowStep()
        );

        return index >= 0 && index < rowCount()
                ? index
                : -1;
    }

    /** True when the cursor is inside the list viewport. */
    protected boolean insideList(double mouseX, double mouseY) {

        return mouseX >= rowLeft + ROW_PAD &&
                mouseX <= rowLeft + rowWidth - ROW_PAD &&
                mouseY >= listTop &&
                mouseY <= listBottom;
    }

    /**
     * Draws the framed viewport, the clipped rows, the edge fades and the
     * scrollbar. Subclasses call this from {@code renderTheme}.
     */
    protected void renderList(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        Ui.drawPanel(
                context,
                rowLeft,
                listTop - 8,
                rowWidth,
                listBottom - listTop + 16,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                rowLeft,
                listTop - 8,
                rowWidth,
                listBottom - listTop + 16,
                alpha
        );

        if (rowCount() == 0) {
            return;
        }

        updateScroll(delta);

        context.enableScissor(
                rowLeft + ROW_PAD,
                listTop,
                rowLeft + rowWidth - ROW_PAD,
                listBottom
        );

        renderRows(context, mouseX, mouseY, delta, alpha);

        context.disableScissor();

        drawScrollChrome(context, alpha);
    }

    /** Edge fades plus the scrollbar, drawn above the rows. */
    protected void drawScrollChrome(
            DrawContext context,
            float alpha
    ) {

        int max = maxScroll();

        float top = Ui.clamp01(scrollPos / FADE_H);
        float bottom = Ui.clamp01((max - scrollPos) / FADE_H);

        Ui.drawScrollFade(
                context,
                rowLeft + ROW_PAD,
                listTop,
                rowWidth - ROW_PAD * 2,
                listBottom,
                top,
                bottom,
                alpha
        );

        if (max <= 0) {
            return;
        }

        int trackH = listBottom - listTop;

        float visible = trackH / (float) (trackH + max);

        Ui.drawScrollbar(
                context,
                rowLeft + rowWidth - 5,
                listTop,
                trackH,
                scrollPos / (float) max,
                visible,
                alpha
        );
    }

    /** Staggered fade-in alpha of a row. */
    protected float rowAlpha(int index) {

        return Ui.entranceProgress(openTime, index * 14L, 210L);
    }
}
