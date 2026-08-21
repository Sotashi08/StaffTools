package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Base screen for the mod UI: entrance animation, responsive centered form
 * and helpers to create custom themed widgets.
 */
public abstract class StScreen extends Screen {

    private static final int MAX_FORM_WIDTH = 380;

    // not final: screens that can be re-entered (via parent links)
    // reset it in init() so the entrance animation replays
    protected long openTime =
            System.currentTimeMillis();

    private int slotCounter;

    protected StScreen(Text title) {
        super(title);
    }

    protected int nextSlot() {
        return slotCounter++;
    }

    protected float entrance() {
        return Ui.easeOutCubic(Ui.clamp01(
                (System.currentTimeMillis() - openTime) / 260f
        ));
    }

    protected int formWidth() {
        return Math.min(MAX_FORM_WIDTH, width - 32);
    }

    protected int formLeft() {
        return (width - formWidth()) / 2;
    }

    protected StButton button(
            int x,
            int y,
            int width,
            int height,
            String key,
            ButtonWidget.PressAction onPress
    ) {

        return button(x, y, width, height, key, onPress, Ui.ACCENT);
    }

    protected StButton button(
            int x,
            int y,
            int width,
            int height,
            String key,
            ButtonWidget.PressAction onPress,
            int accent
    ) {

        StButton widget =
                new StButton(
                        x,
                        y,
                        width,
                        height,
                        Lang.text(key),
                        onPress,
                        nextSlot(),
                        accent
                );

        addDrawableChild(widget);

        return widget;
    }

    protected StToggle toggle(
            int x,
            int y,
            int width,
            int height,
            String labelKey,
            boolean value,
            Consumer<Boolean> onChange
    ) {

        StToggle widget =
                new StToggle(
                        x,
                        y,
                        width,
                        height,
                        labelKey,
                        value,
                        onChange,
                        nextSlot()
                );

        addDrawableChild(widget);

        return widget;
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        float alpha = entrance();

        context.getMatrices().push();

        context.getMatrices().translate(
                0,
                (1 - alpha) * 14,
                0
        );

        renderBackground(
                context,
                mouseX,
                mouseY,
                delta
        );

        renderTheme(
                context,
                mouseX,
                mouseY,
                delta,
                alpha
        );

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        renderOverlay(
                context,
                mouseX,
                mouseY,
                delta,
                alpha
        );

        context.getMatrices().pop();
    }

    /** Draws titles, labels, rows and other custom content. */
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {
    }

    /** Draws content on top of widgets (e.g. popups). */
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {
    }

    protected void drawTitle(
            DrawContext context,
            String key,
            float alpha
    ) {

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(key),
                width / 2,
                16,
                Ui.argb(Ui.TEXT, alpha)
        );

        Ui.drawHeader(
                context,
                width / 2 - 55,
                27,
                110,
                alpha
        );
    }

    protected void drawHint(
            DrawContext context,
            String key,
            int y,
            float alpha
    ) {

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(key),
                width / 2,
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );
    }
}
