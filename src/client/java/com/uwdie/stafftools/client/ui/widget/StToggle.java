package com.uwdie.stafftools.client.ui.widget;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Toggle that looks exactly like a regular StButton: same background,
 * border and label, with a small round color indicator on the right
 * (accent = on, muted gray = off).
 */
public class StToggle extends ButtonWidget {

    private static final int DOT = 8;

    private final long appear;
    private final int slot;
    private final Consumer<Boolean> onChange;

    private final String labelKey;
    private final TextRenderer textRenderer =
            MinecraftClient.getInstance().textRenderer;
    private boolean value;
    private float hoverAmt;
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

        super(x, y, width, height, Text.empty(), b -> {
        }, DEFAULT_NARRATION_SUPPLIER);

        this.appear = System.currentTimeMillis();
        this.slot = slot;
        this.onChange = onChange;
        this.labelKey = labelKey;
        this.value = initial;

        updateMessage();
    }

    private void updateMessage() {

        setMessage(
                Text.literal(
                        Lang.t(labelKey) +
                                ": " +
                                (value
                                        ? Lang.t(Key.BTN_ON)
                                        : Lang.t(Key.BTN_OFF))
                )
        );
    }

    @Override
    public void onPress() {

        value = !value;
        updateMessage();

        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public boolean isOn() {
        return value;
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

        float hoverTarget = isHovered() ? 1f : 0f;

        if (hoverTarget > 0f && !hoverSoundPlayed && active) {
            Ui.playHover();
            hoverSoundPlayed = true;
        } else if (hoverTarget <= 0f) {
            hoverSoundPlayed = false;
        }

        hoverAmt += (hoverTarget - hoverAmt) *
                Math.min(1f, delta * 13f);

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // identical to StButton
        int bg = Ui.mix(
                0xFF171B22,
                0xFF23334B,
                hoverAmt
        );

        int border = Ui.mix(
                0xFF2C3B55,
                Ui.ACCENT,
                hoverAmt
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
                Text.literal(Lang.t(labelKey)),
                x + 10,
                y + (h - 9) / 2,
                Ui.argb(labelColor, alpha)
        );

        // round color indicator instead of a slider
        // (drawn manually: drawRoundRect breaks when radius == size/2)
        int dx = x + w - DOT - 10;
        int dy = y + (h - DOT) / 2;

        int dotColor = value
                ? Ui.mix(Ui.SUCCESS, Ui.ACCENT_SOFT, hoverAmt)
                : Ui.mix(0xFF4A5568, 0xFF5A6A80, hoverAmt);

        // dark outline
        context.fill(
                dx - 1,
                dy + 1,
                dx + DOT + 1,
                dy + DOT - 1,
                Ui.argb(0xFF0D1015, alpha)
        );

        context.fill(
                dx + 1,
                dy - 1,
                dx + DOT - 1,
                dy + DOT + 1,
                Ui.argb(0xFF0D1015, alpha)
        );

        // filled body: vertical + horizontal bars overlap into a circle
        context.fill(
                dx + 2,
                dy,
                dx + DOT - 2,
                dy + DOT,
                Ui.argb(dotColor, alpha)
        );

        context.fill(
                dx,
                dy + 2,
                dx + DOT,
                dy + DOT - 2,
                Ui.argb(dotColor, alpha)
        );

        // corner pixels to round it off
        context.fill(dx + 1, dy + 1, dx + 2, dy + 2, Ui.argb(dotColor, alpha));
        context.fill(dx + DOT - 2, dy + 1, dx + DOT - 1, dy + 2, Ui.argb(dotColor, alpha));
        context.fill(dx + 1, dy + DOT - 2, dx + 2, dy + DOT - 1, Ui.argb(dotColor, alpha));
        context.fill(dx + DOT - 2, dy + DOT - 2, dx + DOT - 1, dy + DOT - 1, Ui.argb(dotColor, alpha));

        // top highlight
        context.fill(
                dx + 2,
                dy + 1,
                dx + DOT - 2,
                dy + 2,
                Ui.argb(0x66FFFFFF, alpha)
        );

        context.getMatrices().pop();
    }
}
