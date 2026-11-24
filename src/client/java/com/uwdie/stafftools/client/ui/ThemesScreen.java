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
    private static final int GRID_GAP = 6;
    private static final int COMPACT_GRID_GAP = 4;

    private int gridX;
    private int gridW;
    private int cardW = 90;
    private int cardH = 42;
    private int gridGap = GRID_GAP;
    private int headingTop;
    private int gridTop;
    private int gridViewportBottom;
    private int gridScroll;
    private int gridScrollMax;

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

        configureShell();

        // widgets must be created HERE, once per screen open —
        // building them in renderTheme stacked copies every frame
        clearChildren();

        int rows = (Theme.count() + GRID_COLS - 1) / GRID_COLS;
        int contentHeight = contentBottom() - contentTop();
        int availW = Math.min(contentWidth(), 560);

        headingTop = contentTop();
        cardH = contentHeight < 260 ? 28 : 42;
        gridGap = cardH < 36 ? COMPACT_GRID_GAP : GRID_GAP;
        gridW = Math.max(0, availW - 8);
        cardW = Math.max(
                48,
                (gridW - (GRID_COLS - 1) * gridGap)
                        / GRID_COLS
        );

        gridX = contentLeft();
        gridTop = headingTop + 32;

        // Keep all swatches on one row so the expanded theme grid can
        // share the compact shell with the accent-color picker.
        nickW = Math.min(contentWidth(), 560);
        nickX = contentLeft();
        nickTop = contentBottom()
                - 26
                - StColorPicker.heightOf(nickW)
                - 14;

        gridViewportBottom = Math.max(
                gridTop,
                nickTop - 13
        );

        int gridContentHeight =
                rows * (cardH + gridGap) - gridGap;

        gridScrollMax = Math.max(
                0,
                gridContentHeight
                        - (gridViewportBottom - gridTop)
        );
        gridScroll = Math.clamp(gridScroll, 0, gridScrollMax);

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
                contentLeft(),
                contentBottom() - 26,
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
        return gridX + col * (cardW + gridGap);
    }

    private int cardY(int row) {
        return gridTop + row * (cardH + gridGap) - gridScroll;
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
                headingTop,
                "\uD83C\uDFA8",
                Lang.t(Key.BTN_THEMES),
                Lang.t(Key.MSG_ACCENT_HINT),
                alpha
        );

        if (gridViewportBottom > gridTop) {

            context.enableScissor(
                    gridX,
                    gridTop,
                    gridX + gridW,
                    gridViewportBottom
            );

            for (int i = 0; i < Theme.count(); i++) {

                drawThemeCard(
                        context,
                        i,
                        mouseX,
                        mouseY,
                        alpha
                );
            }

            context.disableScissor();
        }

        drawGridScrollbar(context, alpha);

        drawNickCaption(context, mouseX, mouseY, alpha);
    }

    private void drawGridScrollbar(
            DrawContext context,
            float alpha
    ) {

        int viewportHeight = gridViewportBottom - gridTop;

        if (gridScrollMax <= 0 || viewportHeight <= 0) {
            return;
        }

        int rows = (Theme.count() + GRID_COLS - 1) / GRID_COLS;
        int totalHeight = rows * (cardH + gridGap) - gridGap;
        int thumbHeight = Math.min(
                viewportHeight,
                Math.max(
                        14,
                        viewportHeight * viewportHeight / totalHeight
                )
        );
        int thumbY = gridTop
                + (viewportHeight - thumbHeight)
                * gridScroll / gridScrollMax;
        int x = gridX + gridW + 3;

        Ui.drawRoundRect(
                context,
                x,
                gridTop,
                2,
                viewportHeight,
                1,
                Ui.argb(Ui.BORDER, alpha * 0.7f)
        );
        Ui.drawRoundRect(
                context,
                x,
                thumbY,
                2,
                thumbHeight,
                1,
                Ui.argb(Ui.ACCENT, alpha * 0.85f)
        );
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

        boolean selected = index == Theme.getIndex();

        boolean hovered = isHovered(index, mouseX, mouseY);

        int lift = hovered && !selected ? -1 : 0;

        int x = cardX(col);
        int y = cardY(rowIdx) + lift;

        if (selected) {

            Ui.drawCardAccent(
                    context, x, y, cardW, cardH,
                    Ui.ACCENT, alpha
            );

        } else if (hovered) {

            Ui.drawRoundRect(
                    context, x, y, cardW, cardH, Design.RADIUS_MD,
                    Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.14f), alpha)
            );

            Ui.drawRoundBorder(
                    context, x, y, cardW, cardH, Design.RADIUS_MD,
                    Ui.argb(Ui.ACCENT, alpha * 0.85f)
            );

        } else {

            Ui.drawCard(context, x, y, cardW, cardH, alpha);
        }

        // gradient preview strip on top of the card
        int pw = cardW - 12;
        int px = x + 6;
        boolean compact = cardH < 36;
        int py = y + (compact ? 4 : 7);

        Ui.drawGradientH(
                context,
                px,
                py,
                pw,
                compact ? 6 : 10,
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
                Text.literal(textRenderer.trimToWidth(name, cardW - 12)),
                x + 6,
                compact ? y + 14 : py + 16,
                Ui.argb(nameColor, alpha)
        );

        if (selected) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("✓"),
                    x + cardW - 16,
                    compact ? y + 14 : py + 16,
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
                mouseY >= gridTop &&
                mouseY < gridViewportBottom &&
                mouseY >= y &&
                mouseY < y + cardH;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (gridScrollMax > 0
                && mouseX >= gridX
                && mouseX < gridX + gridW
                && mouseY >= gridTop
                && mouseY < gridViewportBottom) {

            int step = cardH + gridGap;

            gridScroll = Math.clamp(
                    gridScroll + (verticalAmount > 0 ? -step : step),
                    0,
                    gridScrollMax
            );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
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

                    com.uwdie.stafftools.client.audit.AuditLog.get()
                            .info(
                                    com.uwdie.stafftools.client.audit
                                            .AuditCategory.CONFIG,
                                    "Theme changed: "
                                            + Lang.t(Theme.nameKey(i))
                            );

                    // rebuild so everything repaints in new colors
                    client.setScreen(new ThemesScreen());

                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }
}
