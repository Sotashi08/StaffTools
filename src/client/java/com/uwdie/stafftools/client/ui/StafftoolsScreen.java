package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Main menu: a single centered column — animated text logo on top,
 * navigation buttons, then settings toggles, all inside one panel.
 */
public class StafftoolsScreen extends StScreen {

    private static final String LOGO_TEXT = "StaffTools";

    private static final int COL_W = 220;
    private static final int WIDGET_H = 20;
    private static final int ROW_STEP = 24;

    private int colX;
    private int contentTop;
    private int panelBottom;

    private final int logoVariant =
            java.util.concurrent.ThreadLocalRandom
                    .current()
                    .nextInt(4);

    public StafftoolsScreen() {
        super(Lang.text(Key.APP_TITLE));
    }

    @Override
    protected void init() {

        colX = (width - COL_W) / 2;

        contentTop = 96;

        int row = 0;

        addNavButton(row++, "⚙ ", Key.BTN_MACROS, () ->
                client.setScreen(new MacroListScreen(this)));

        addNavButton(row++, "+ ", Key.BTN_CREATE_MACRO, () ->
                client.setScreen(new MacroEditorScreen(this, null)));

        addNavButton(row++, "📋 ", Key.BTN_PLAYER_ACTIONS, () ->
                client.setScreen(new ActionsConfigScreen(this)));

        addNavButton(row++, "🕘 ", Key.BTN_HISTORY, () ->
                client.setScreen(new ActionHistoryScreen(this)));

        row++;

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        addToggle(row++, Key.TOGGLE_ACTIONS_POPUP,
                config.isPlayerActionsEnabled(),
                v -> {
                    config.setPlayerActionsEnabled(v);
                    StafftoolsClient.saveConfig();
                });

        addToggle(row++, Key.TOGGLE_CONFIRM_DANGER,
                config.isDangerousMacroConfirmation(),
                v -> {
                    config.setDangerousMacroConfirmation(v);
                    StafftoolsClient.saveConfig();
                });

        addToggle(row++, Key.TOGGLE_TOASTS,
                config.isToastsEnabled(),
                v -> {
                    config.setToastsEnabled(v);
                    StafftoolsClient.saveConfig();
                });

        button(
                colX,
                contentTop + row * ROW_STEP + 6,
                COL_W,
                WIDGET_H,
                Key.BTN_CLOSE,
                b -> close()
        );

        panelBottom =
                contentTop + (row + 1) * ROW_STEP + 6 + WIDGET_H + 10;
    }

    private void addNavButton(
            int row,
            String icon,
            String key,
            Runnable action
    ) {

        addDrawableChild(
                new StButton(
                        colX,
                        contentTop + row * ROW_STEP,
                        COL_W,
                        WIDGET_H,
                        Text.literal(icon)
                                .append(Lang.text(key)),
                        b -> action.run(),
                        nextSlot(),
                        Ui.ACCENT
                )
        );
    }

    private void addToggle(
            int row,
            String key,
            boolean initial,
            java.util.function.Consumer<Boolean> onChange
    ) {

        toggle(
                colX,
                contentTop + row * ROW_STEP,
                COL_W,
                WIDGET_H,
                key,
                initial,
                onChange
        );
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

        drawLogo(context, width / 2, 58, alpha);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.APP_TAGLINE),
                width / 2,
                76,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        long now = System.currentTimeMillis();

        // tied to the ENTRANCE PROGRESS itself (not wall-clock),
        // so the lines always fade in together with the panel:
        // line 1 during the first half of the entrance,
        // line 2 during the second half.
        float line1A =
                Ui.clamp01(
                        (alpha - 0.30f) / 0.45f
                );

        float line2A =
                Ui.clamp01(
                        (alpha - 0.55f) / 0.45f
                );

        if (line1A > 0.01f) {

            context.fill(
                    colX,
                    contentTop - 8,
                    colX + COL_W,
                    contentTop - 7,
                    Ui.argb(0xFF2C3B55, line1A)
            );

            drawShimmerLine(
                    context, now,
                    contentTop - 8, line1A
            );
        }

        if (line2A > 0.01f) {

            context.fill(
                    colX,
                    contentTop + 4 * ROW_STEP,
                    colX + COL_W,
                    contentTop + 4 * ROW_STEP + 1,
                    Ui.argb(0xFF2C3B55, line2A)
            );

            drawShimmerLine(
                    context, now + 1600,
                    contentTop + 4 * ROW_STEP, line2A
            );
        }
    }

    /**
     * A soft accent segment gliding along a separator line
     * with a ping-pong motion (no wrap-around jump).
     */
    private void drawShimmerLine(
            DrawContext context,
            long time,
            int y,
            float alpha
    ) {

        // modulo in LONG space: float loses all precision
        // at epoch-scale timestamps (~131s steps)
        float raw =
                (time % 4200L) / 4200f;

        float t =
                1f - Math.abs(raw * 2f - 1f);

        int segW = Math.min(48, COL_W / 4);

        int travel = COL_W - segW;

        int gx = colX + (int) (travel * t);

        int gxEnd = gx + segW;

        int midStart = Math.max(gx, colX);
        int midEnd = Math.min(gxEnd, colX + COL_W);

        if (midEnd > midStart) {

            context.fill(
                    midStart,
                    y,
                    midEnd,
                    y + 1,
                    Ui.argb(Ui.ACCENT, alpha * 0.65f)
            );
        }

        int edge = gx < colX ? colX : gxEnd > colX + COL_W ? colX + COL_W - 1 : -1;

        if (edge >= 0) {

            context.fill(
                    edge,
                    y,
                    edge + 1,
                    y + 1,
                    Ui.argb(Ui.ACCENT, alpha * 0.35f)
            );
        }
    }

    /**
     * Animated text logo with 4 random variants picked on screen open:
     * 0 = wave, 1 = comet, 2 = float, 3 = ripple.
     * All motion is sub-pixel smooth via matrix translation.
     */
    private void drawLogo(
            DrawContext context,
            int cx,
            int cy,
            float alpha
    ) {

        long now = System.currentTimeMillis();

        var matrices = context.getMatrices();

        int total =
                textRenderer.getWidth(LOGO_TEXT);

        int x = cx - total / 2;

        int len = LOGO_TEXT.length();

        int center = (len - 1) / 2;

        // ping-pong 0 -> 1 -> 0, no wrap-around jump
        // (modulo in long space — see drawShimmerLine)
        float cometRaw =
                (now % 3200L) / 3200f;

        float cometPos =
                1f - Math.abs(cometRaw * 2f - 1f);

        for (int i = 0; i < len; i++) {

            char c = LOGO_TEXT.charAt(i);

            int cw = textRenderer.getWidth(
                    String.valueOf(c)
            );

            float offY;
            float mixT;

            switch (logoVariant) {

                case 1 -> { // comet: glow traveling there and back
                    float p = i / (float) (len - 1);

                    float d = Math.abs(p - cometPos);

                    mixT = Math.max(0f, 1f - d * 4f);

                    offY = -mixT * 1.2f;
                }

                case 2 -> { // float: whole word bobbing
                    offY =
                            (float) Math.sin(now / 650.0)
                                    * 1.3f;

                    mixT =
                            0.5f + 0.5f *
                                    (float) Math.sin(now / 1000.0);
                }

                case 3 -> { // ripple from the center out
                    int dist = Math.abs(i - center);

                    offY =
                            (float) Math.sin(
                                    now / 480.0 - dist * 0.6
                            ) * 1.1f;

                    mixT =
                            0.5f + 0.5f *
                                    (float) Math.sin(
                                            now / 950.0 - dist * 0.5
                                    );
                }

                default -> { // wave
                    offY =
                            (float) Math.sin(
                                    now / 520.0 + i * 0.5
                            ) * 1.1f;

                    mixT =
                            0.5f + 0.5f *
                                    (float) Math.sin(
                                            now / 850.0 + i * 0.45
                                    );
                }
            }

            int color = Ui.mix(
                    Ui.TEXT,
                    Ui.ACCENT_SOFT,
                    mixT * 0.75f
            );

            matrices.push();

            matrices.translate(x, cy + offY, 0);

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(String.valueOf(c)),
                    0,
                    0,
                    Ui.argb(color, alpha)
            );

            matrices.pop();

            x += cw;
        }

        float breathe =
                0.5f + 0.5f *
                        (float) Math.sin(now / 1100.0);

        int lineW =
                (int) (total * (0.6f + 0.4f * breathe));

        context.fill(
                cx - lineW / 2,
                cy + 13,
                cx + lineW / 2,
                cy + 14,
                Ui.argb(
                        Ui.ACCENT,
                        alpha * (0.5f + 0.35f * breathe)
                )
        );

        if (logoVariant == 1) {

            int hx = cx - total / 2 +
                    (int) (total * cometPos);

            context.fill(
                    hx - 8,
                    cy + 13,
                    hx + 8,
                    cy + 14,
                    Ui.argb(Ui.ACCENT_SOFT, alpha * 0.9f)
            );
        }
    }
}
