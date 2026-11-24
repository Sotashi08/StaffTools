package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.Design;
import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.AbstractInput;

import java.util.function.Consumer;

/**
 * Toggle that looks like a regular StButton: same surface, border and
 * label, with a small switch on the right whose knob slides between the
 * off and on positions.
 *
 * <p>The knob position is animated, so flipping a switch reads as a
 * physical movement rather than an instant colour swap.
 *
 * {@code Text} is inherited from {@link ButtonWidget} and names its own
 * nested class, so the message type is spelled out in full here.
 */
public class StToggle extends ButtonWidget.Text {

    private static final int TRACK_W = 18;
    private static final int TRACK_H = 8;
    private static final int KNOB = 6;

    private final long appear;
    private final int slot;
    private final Consumer<Boolean> onChange;

    private final String labelKey;
    private final TextRenderer textRenderer =
            MinecraftClient.getInstance().textRenderer;

    private boolean value;

    private float hoverAmt;
    private float knobAmt;
    private boolean hoverSoundPlayed;

    public StToggle(
            int x,
            int y,
            int width,
            int height,
            String labelKey,
            boolean initial,
            Consumer<Boolean> onChange,
            int slot
    ) {

        super(x, y, width, height,
                net.minecraft.text.Text.empty(), b -> {
        }, DEFAULT_NARRATION_SUPPLIER);

        this.appear = System.currentTimeMillis();
        this.slot = slot;
        this.onChange = onChange;
        this.labelKey = labelKey;
        this.value = initial;
        this.knobAmt = initial ? 1f : 0f;

        updateMessage();
    }

    private void updateMessage() {

        setMessage(
                net.minecraft.text.Text.literal(
                        Lang.t(labelKey) +
                                ": " +
                                (value
                                        ? Lang.t(Key.BTN_ON)
                                        : Lang.t(Key.BTN_OFF))
                )
        );
    }

    @Override
    public void onPress(AbstractInput input) {

        value = !value;
        updateMessage();

        if (onChange != null) {
            onChange.accept(value);
        }
    }

    /**
     * Draw + hit-test toggle. Used by the detection settings to hide
     * rows that are currently scrolled out of view.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isOn() {
        return value;
    }

    @Override
    protected void drawIcon(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        // A short, restrained scale-and-fade entrance, staggered by slot.
        float alpha = Ui.entranceProgress(
                appear,
                slot * 16L,
                210L
        );

        float scale = Ui.animationsEnabled()
                ? 0.965f + 0.035f * alpha
                : 1f;

        if (alpha <= 0.001f) {
            return;
        }

        float hoverTarget = isHovered() ? 1f : 0f;

        if (hoverTarget > 0f && !hoverSoundPlayed && active) {
            Ui.playHover();
            hoverSoundPlayed = true;
        } else if (hoverTarget <= 0f) {
            hoverSoundPlayed = false;
        }

        hoverAmt = Ui.animateTowards(
                hoverAmt,
                hoverTarget,
                delta,
                13f
        );

        // knob slides between the two positions
        knobAmt = Ui.animateTowards(
                knobAmt,
                value ? 1f : 0f,
                delta,
                16f
        );

        int x = getX();
        int y = getY() - (Ui.animationsEnabled()
                ? Math.round(hoverAmt)
                : 0);
        int w = getWidth();
        int h = getHeight();

        // identical surface to StButton
        int bg = Ui.mix(
                Ui.mix(Ui.PANEL_BG, Ui.TEXT, 0.05f),
                Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.22f),
                hoverAmt
        );

        int border = Ui.mix(
                Ui.mix(Ui.PANEL_BORDER, Ui.TEXT, 0.08f),
                Ui.ACCENT,
                hoverAmt
        );

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

        Ui.drawShadow(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                3,
                0.14f + hoverAmt * 0.12f,
                alpha
        );

        Ui.drawHoverGlow(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                Ui.ACCENT,
                hoverAmt,
                alpha
        );

        Ui.drawRoundRect(
                context,
                x,
                y,
                w,
                h,
                Design.RADIUS_MD,
                Ui.argb(bg, alpha)
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                w,
                h,
                Design.RADIUS_MD,
                Ui.argb(border, alpha)
        );

        if (active) {

            context.fill(
                    x + 3,
                    y,
                    x + w - 3,
                    y + 1,
                    Ui.argb(
                            value ? Ui.ACCENT : 0xFF556070,
                            alpha
                    )
            );
        }

        int labelColor = active
                ? (value
                        ? 0xFFFFFFFF
                        : Ui.mix(0xFFB0B0B0, 0xFFE6EBF2, hoverAmt))
                : 0xFF888888;

        context.drawTextWithShadow(
                textRenderer,
                net.minecraft.text.Text.literal(
                        textRenderer.trimToWidth(
                                Lang.t(labelKey),
                                w - TRACK_W - 26
                        )
                ),
                x + 10,
                y + (h - 9) / 2,
                Ui.argb(labelColor, alpha)
        );

        drawSwitch(context, x + w - TRACK_W - 10, y, alpha);

        context.getMatrices().popMatrix();
    }

    /** The switch: a rounded track with a sliding knob. */
    private void drawSwitch(
            DrawContext context,
            int trackX,
            int rowY,
            float alpha
    ) {

        int trackY = rowY + (getHeight() - TRACK_H) / 2;

        int trackColor = Ui.mix(
                0xFF3A4453,
                Ui.mix(Ui.ACCENT, Ui.SUCCESS, 0.35f),
                knobAmt
        );

        Ui.drawRoundRect(
                context,
                trackX,
                trackY,
                TRACK_W,
                TRACK_H,
                Design.RADIUS_LG,
                Ui.argb(trackColor, alpha)
        );

        Ui.drawRoundBorder(
                context,
                trackX,
                trackY,
                TRACK_W,
                TRACK_H,
                Design.RADIUS_LG,
                Ui.argb(
                        Ui.mix(0xFF0D1015, Ui.ACCENT, knobAmt * 0.5f),
                        alpha
                )
        );

        // knob travels from the left to the right end of the track
        int travel = TRACK_W - KNOB - 2;

        int knobX = trackX + 1 + (int) (travel * knobAmt);
        int knobY = trackY + (TRACK_H - KNOB) / 2;

        Ui.drawRoundRect(
                context,
                knobX,
                knobY,
                KNOB,
                KNOB,
                Design.RADIUS_MD,
                Ui.argb(0xFFF2F5FA, alpha)
        );
    }
}
