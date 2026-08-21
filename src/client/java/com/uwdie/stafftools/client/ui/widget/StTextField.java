package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Custom themed text field with smooth focus highlight and entrance animation.
 */
public class StTextField extends TextFieldWidget {

    private final long appear;
    private final int slot;
    private float focusAmt;
    private Runnable onFocusGain;

    public StTextField(
            TextRenderer textRenderer,
            int x,
            int y,
            int width,
            int height,
            Text placeholder,
            int slot
    ) {

        super(textRenderer, x, y, width, height, placeholder);

        this.appear = System.currentTimeMillis();
        this.slot = slot;

        setDrawsBackground(false);
        setEditableColor(0xFFFFFFFF);
        setUneditableColor(0xFFAAAAAA);
        setMaxLength(256);
    }

    /** Notified whenever this field gains keyboard focus. */
    public void setOnFocusGain(Runnable callback) {
        this.onFocusGain = callback;
    }

    @Override
    public void setFocused(boolean focused) {

        boolean was = isFocused();

        super.setFocused(focused);

        if (focused && !was && onFocusGain != null) {
            onFocusGain.run();
        }
    }

    @Override
    public void renderWidget(
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

        float target = isFocused() ? 1f : 0f;

        focusAmt += (target - focusAmt) *
                Math.min(1f, delta * 12f);

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bg = Ui.mix(
                0xFF12151B,
                0xFF1C2536,
                focusAmt * 0.8f
        );

        int border = Ui.mix(
                0xFF2C3B55,
                Ui.ACCENT,
                focusAmt
        );

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

        context.getMatrices().translate(4, 5, 0);

        super.renderWidget(
                context,
                mouseX,
                mouseY,
                delta
        );

        context.getMatrices().pop();
    }
}
