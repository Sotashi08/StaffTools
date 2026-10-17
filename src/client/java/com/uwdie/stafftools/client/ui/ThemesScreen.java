package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StColorPicker;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Theme picker: a grid of stylized cards with a gradient preview block,
 * name and checkmark for the active theme. Click applies instantly.
 */
public class ThemesScreen extends StScreen {

    private static final int GRID_COLS = 4;
    private static final int GRID_GAP = 8;

    private int gridX;
    private int cardW = 90;
    private int cardH = 52;
    private int contentTop;
    private int panelBottom;

    /** Caption and swatch grid of the nick highlight picker. */
    private int nickTop;
    private int nickW;
    private int nickX;

    public ThemesScreen() {
        super(Lang.text(Key.BTN_THEMES));
    }

    @Override
    protected void init() {

        resetSlots();

        // widgets must be created HERE, once per screen open —
        // building them in renderTheme stacked copies every frame
        clearChildren();

        int rows = (Theme.count() + GRID_COLS - 1) / GRID_COLS;

        int availW = formWidth() - 24;

        cardW = Math.max(
                80,
                (availW - (GRID_COLS - 1) * GRID_GAP) / GRID_COLS
        );

        cardH = 52;

        int gridW = GRID_COLS * cardW
                + (GRID_COLS - 1) * GRID_GAP;

        gridX = (width - gridW) / 2;

        contentTop = 74;

        // the nick picker sits under the theme grid
        nickW = Math.min(availW, 232);
        nickX = (width - nickW) / 2;
        nickTop = contentTop + rows * (cardH + GRID_GAP) + 10;

        panelBottom = nickTop
                + StColorPicker.heightOf(nickW)
                + 44;

        addDrawableChild(
                new StColorPicker(
                        nickX,
                        nickTop,
                        nickW,
                        currentNick(),
                        color -> {

                            StafftoolsClient.getConfig()
                                    .setNickHighlight(color);

                            Ui.NICK_HIGHLIGHT = color;

                            StafftoolsClient.saveConfig();
                        }
                )
        );

        button(
                width / 2 - 60,
                panelBottom - 32,
                120,
                20,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    /**
     * The color currently in effect: the manual pick when there is one,
     * otherwise the default the active theme ships with.
     */
    private static int currentNick() {

        int manual =
                StafftoolsClient.getConfig()
                        .getNickHighlight();

        return manual != 0
                ? manual
                : Theme.nickOf(Theme.getIndex());
    }

    private int cardX(int col) {
        return gridX + col * (cardW + GRID_GAP);
    }

    private int cardY(int row) {
        return contentTop + row * (cardH + GRID_GAP);
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        Ui.drawPanel(
                context,
                formLeft(),
                34,
                formWidth(),
                panelBottom - 34,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                formLeft(),
                34,
                formWidth(),
                panelBottom - 34,
                alpha
        );

        drawTitle(context, Key.BTN_THEMES, alpha);

        for (int i = 0; i < Theme.count(); i++) {

            drawThemeCard(
                    context,
                    i,
                    mouseX,
                    mouseY,
                    alpha
            );
        }

        drawNickCaption(context, mouseX, mouseY, alpha);
    }

    /** Caption above the nick swatches plus the reset link. */
    private void drawNickCaption(
            DrawContext context,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.THEME_NICK_COLOR),
                nickX,
                nickTop - 12,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        boolean manual =
                StafftoolsClient.getConfig()
                        .isNickHighlightAuto()
                        == false;

        String reset =
                manual
                        ? Lang.t(Key.THEME_NICK_RESET)
                        : Lang.t(Key.THEME_NICK_AUTO);

        int w = textRenderer.getWidth(reset);

        boolean over =
                mouseX >= nickX + nickW - w &&
                        mouseX <= nickX + nickW &&
                        mouseY >= nickTop - 13 &&
                        mouseY <= nickTop - 4;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(reset),
                nickX + nickW - w,
                nickTop - 12,
                Ui.argb(
                        manual
                                ? over ? Ui.DANGER : Ui.TEXT_MUTED
                                : Ui.TEXT_MUTED,
                        alpha
                )
        );
    }

    private void drawThemeCard(
            DrawContext context,
            int index,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        int col = index % GRID_COLS;
        int rowIdx = index / GRID_COLS;

        boolean selected =
                index == Theme.getIndex();

        boolean hovered =
                isHovered(index, mouseX, mouseY);

        int lift =
                hovered && !selected ? -1 : 0;

        int x = cardX(col);
        int y = cardY(rowIdx) + lift;

        int bg = selected
                ? 0x303F8AE0
                : hovered ? 0x26000000 : 0x18000000;

        context.fill(
                x,
                y,
                x + cardW,
                y + cardH,
                Ui.argb(bg, alpha)
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                cardW,
                cardH,
                3,
                Ui.argb(
                        selected ? Ui.ACCENT
                                : hovered ? 0xFF3A4A66
                                : 0xFF2C3B55,
                        alpha * (selected || hovered ? 1f : 0.7f)
                )
        );

        // gradient preview strip on top of the card
        int pw = cardW - 12;
        int px = x + 6;
        int py = y + 7;

        Ui.drawGradientH(
                context,
                px,
                py,
                pw,
                10,
                Theme.accentOf(index),
                Theme.accent2Of(index),
                alpha
        );

        String name = Lang.t(Theme.nameKey(index));

        int nameColor = selected
                ? Ui.TEXT
                : hovered ? Ui.TEXT_DIM : Ui.TEXT_MUTED;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                x + 6,
                py + 16,
                Ui.argb(nameColor, alpha)
        );

        if (selected) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("✓"),
                    x + cardW - 16,
                    py + 16,
                    Ui.argb(Ui.SUCCESS, alpha)
            );
        }
    }

    private boolean isHovered(
            int index,
            double mouseX,
            double mouseY
    ) {

        int col = index % GRID_COLS;
        int rowIdx = index / GRID_COLS;

        int x = cardX(col);
        int y = cardY(rowIdx);

        return mouseX >= x &&
                mouseX < x + cardW &&
                mouseY >= y &&
                mouseY < y + cardH;
    }

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {

            // the "back to theme default" link
            if (StafftoolsClient.getConfig()
                    .isNickHighlightAuto() == false &&
                    mouseY >= nickTop - 13 &&
                    mouseY <= nickTop - 4 &&
                    mouseX >= nickX &&
                    mouseX <= nickX + nickW) {

                StafftoolsClient.getConfig()
                        .setNickHighlight(0);

                Ui.NICK_HIGHLIGHT =
                        Theme.nickOf(Theme.getIndex());

                StafftoolsClient.saveConfig();

                client.setScreen(new ThemesScreen());

                return true;
            }

            for (int i = 0; i < Theme.count(); i++) {

                if (isHovered(i, mouseX, mouseY)) {

                    StafftoolsClient.getConfig()
                            .setThemeIndex(i);

                    Theme.apply(i);

                    StafftoolsClient.saveConfig();

                    Ui.playHover();

                    // rebuild so everything repaints in new colors
                    client.setScreen(new ThemesScreen());

                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }
}
