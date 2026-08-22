package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Custom themed button with entrance animation and smooth hover lerp.
 */
public class StButton extends ButtonWidget {

    private final long appear;
    private final int slot;
    private final int accent;
    private final TextRenderer textRenderer =
            MinecraftClient.getInstance().textRenderer;
    private float hoverAmt;
    private boolean hoverSoundPlayed;

    public StButton(
            int x,
            int y,
            int width,
            int height,
            Text message,
            PressAction onPress,
            int slot,
            int accent
    ) {

        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);

        this.appear = System.currentTimeMillis();
        this.slot = slot;
        this.accent = accent;
    }

    @Override
    protected void renderWidget(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        long now = System.currentTimeMillis();

        float alpha = Ui.clamp01(
                (now - appear - slot * 28L) / 190f
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

        hoverAmt += (target - hoverAmt) *
                Math.min(1f, delta * 13f);

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bg = Ui.mix(
                0xFF171B22,
                0xFF23334B,
                hoverAmt
        );

        int border = Ui.mix(
                0xFF2C3B55,
                accent,
                hoverAmt
        );

        int textColor = active
                ? 0xFFFFFFFF
                : 0xFF888888;

        context.getMatrices().push();

        context.getMatrices().translate(
                0,
                (1 - alpha) * 5,
                0
        );

        Ui.drawRoundRect(
                context,
                x,
                y,
                w,
                h,
                3,
                Ui.argb(bg, alpha)
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                w,
                h,
                3,
                Ui.argb(border, alpha)
        );

        if (active) {

            context.fill(
                    x + 3,
                    y,
                    x + w - 3,
                    y + 1,
                    Ui.argb(accent, alpha)
            );
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                getMessage(),
                x + w / 2,
                y + (h - 9) / 2,
                Ui.argb(textColor, alpha)
        );

        context.getMatrices().pop();
    }
}
