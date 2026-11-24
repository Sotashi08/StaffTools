package com.uwdie.stafftools.client.ui;

/**
 * Reusable drag-to-reorder state machine for the manual list screens.
 *
 * <p>The list screens draw their rows themselves (they are not widgets),
 * so the drag has to be tracked by hand. This helper keeps the state and
 * the geometry maths in one place: a press arms a row, movement past a
 * small threshold starts the drag, and the release reports the target
 * index through a callback.
 *
 * <p>Only the index maths lives here; the screens own the drawing.
 */
public final class DragReorder {

    /** Pixels the cursor must travel before a press becomes a drag. */
    private static final double THRESHOLD = 4.0;

    private int armedIndex = -1;
    private double pressY;
    private boolean dragging;
    private int fromIndex = -1;
    private int targetIndex = -1;

    /** True while a row is actually being dragged. */
    public boolean isDragging() {
        return dragging;
    }

    /** Index the dragged row came from, or -1. */
    public int fromIndex() {
        return fromIndex;
    }

    /** Index the row would be dropped at, or -1. */
    public int targetIndex() {
        return targetIndex;
    }

    /** True while a row is armed but the drag has not started yet. */
    public boolean isArmed() {
        return armedIndex >= 0;
    }

    /**
     * Arms a row on press.
     *
     * @param index row index under the cursor
     * @param mouseY press position
     */
    public void press(int index, double mouseY) {

        armedIndex = index;
        pressY = mouseY;
        dragging = false;
        fromIndex = -1;
        targetIndex = -1;
    }

    /**
     * Updates the drag.
     *
     * @param mouseY current cursor position
     * @param listTop top of the list viewport
     * @param rowStep height of one row including the gap
     * @param count number of rows
     * @param scroll current scroll offset
     * @return true when a drag is in progress
     */
    public boolean drag(
            double mouseY,
            int listTop,
            int rowStep,
            int count,
            float scroll
    ) {

        if (armedIndex < 0) {
            return false;
        }

        if (!dragging) {

            if (Math.abs(mouseY - pressY) < THRESHOLD) {
                return false;
            }

            dragging = true;
            fromIndex = armedIndex;
        }

        int slot = (int) Math.floor(
                (mouseY - listTop + scroll) / rowStep
        );

        targetIndex = Math.clamp(slot, 0, Math.max(0, count - 1));

        return true;
    }

    /**
     * Ends the drag.
     *
     * @return the source index when a real reorder happened, else -1
     */
    public int release() {

        int result = -1;

        if (dragging &&
                fromIndex >= 0 &&
                targetIndex >= 0 &&
                fromIndex != targetIndex) {

            result = fromIndex;
        }

        reset();

        return result;
    }

    /** Drops the state without reporting a reorder. */
    public void cancel() {
        reset();
    }

    private void reset() {
        armedIndex = -1;
        dragging = false;
        fromIndex = -1;
        targetIndex = -1;
    }
}
