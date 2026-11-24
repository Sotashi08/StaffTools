package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.ui.Design;
import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Custom themed button with entrance animation, smooth hover lerp and a
 * pressed state.
 *
 * <p>Visual language: a dark surface, a hairline border that lights up
 * with the accent on hover, a thin accent bar along the top and a subtle
 * press-down offset. All geometry comes from {@link Design}.
 *
 * {@code Text} is inherited from {@link ButtonWidget} and names its own
 * nested class, so the message type is spelled out in full here.
 */
public class StButton extends ButtonWidget.Text {

    private final long appear;
    private final int slot;
    private final int accent;
    private final TextRenderer textRenderer =
            MinecraftClient.getInstance().textRenderer;

    private float hoverAmt;
    private float pressAmt;
    private boolean hoverSoundPlayed;

    /** When the button was last pressed, for the press-down animation. */
    private long pressAt;

    public StButton(
            int x,
            int y,
            int width,
            int height,
            net.minecraft.text.Text message,
            PressAction onPress,
            int slot,
            int accent
    ) {

        this(
                x, y, width, height, message, onPress,
                System.currentTimeMillis(),
                slot,
                accent
        );
    }

    /**
     * @param appear when the button starts fading in. Screens pass their
     *                own open-time, so a list rebuild never replays the
     *                entrance animation for the widgets that stay.
     */
    public StButton(
            int x,
            int y,
            int width,
            int height,
            net.minecraft.text.Text message,
            PressAction onPress,
            long appear,
            int slot,
            int accent
    ) {

        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);

        this.appear = appear;
        this.slot = slot;
        this.accent = accent;
    }

    /**
     * Draw + hit-test toggle. Used by the list screens to hide the
     * buttons of rows that are currently scrolled out of view.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void onPress(net.minecraft.client.input.AbstractInput input) {

        pressAt = System.currentTimeMillis();

        super.onPress(input);
    }

    @Override
    protected void drawIcon(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        long now = System.currentTimeMillis();

        float alpha = Ui.entranceProgress(
                appear,
                slot * 16L,
                210L
        );

        if (alpha <= 0.001f) {
            return;
        }

        float target = isHovered() ? 1f : 0f;

        if (target > 0f && !hoverSoundPlayed && active) {
            Ui.playHover();
            hoverSoundPlayed = true;
        } else if (target <= 0f) {
            hoverSoundPlayed = false;
        }

        hoverAmt = Ui.animateTowards(
                hoverAmt,
                target,
                delta,
                13f
        );

        // pressed state: a short sink right after the click
        float pressTarget =
                Ui.animationsEnabled() && now - pressAt < 110L
                        ? 1f
                        : 0f;

        pressAmt = Ui.animateTowards(
                pressAmt,
                pressTarget,
                delta,
                22f
        );

        int x = getX();
        float hoverLift = Ui.animationsEnabled()
                ? hoverAmt
                : 0f;
        int y = getY() + Math.round(pressAmt - hoverLift);
        int w = getWidth();
        int h = getHeight();

        int bg = Ui.mix(
                Ui.mix(Ui.PANEL_BG, Ui.TEXT, 0.05f),
                Ui.mix(Ui.PANEL_BG, accent, 0.22f),
                hoverAmt
        );

        int border = Ui.mix(
                Ui.mix(Ui.PANEL_BORDER, Ui.TEXT, 0.08f),
                accent,
                hoverAmt
        );

        int textColor = active
                ? 0xFFFFFFFF
                : 0xFF888888;

        context.getMatrices().pushMatrix();

        context.getMatrices().translate(
                0,
                (1 - alpha) * 5f
        );
        float entryScale = 0.965f + alpha * 0.035f;
        context.getMatrices().translate(
                x + w / 2f,
                y + h / 2f
        );
        context.getMatrices().scale(entryScale, entryScale);
        context.getMatrices().translate(
                -(x + w / 2f),
                -(y + h / 2f)
        );

        // soft shadow, stronger while hovered so the button lifts
        Ui.drawShadow(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                3,
                0.16f + hoverAmt * 0.14f,
                alpha
        );

        Ui.drawHoverGlow(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                accent,
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

            // accent bar along the top, brighter on hover
            context.fill(
                    x + 3,
                    y,
                    x + w - 3,
                    y + 1,
                    Ui.argb(
                            Ui.mix(accent, Ui.TEXT, hoverAmt * 0.25f),
                            alpha
                    )
            );
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                trim(getMessage(), w - 8),
                x + w / 2,
                y + (h - 9) / 2,
                Ui.argb(textColor, alpha)
        );

        context.getMatrices().popMatrix();
    }

    /** Trims a message so it can never spill outside the button. */
    private net.minecraft.text.Text trim(
            net.minecraft.text.Text message,
            int maxWidth
    ) {

        if (textRenderer.getWidth(message) <= maxWidth) {
            return message;
        }

        return net.minecraft.text.Text.literal(
                textRenderer.trimToWidth(
                        message.getString(),
                        maxWidth
                )
        );
    }
}
