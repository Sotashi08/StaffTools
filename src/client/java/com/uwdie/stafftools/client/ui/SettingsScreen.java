package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Settings screen in the launcher layout: a single column of toggle rows
 * on the left and an information panel on the right.
 *
 * <p>Rows are drawn by the screen itself (not as widgets) so the pill
 * toggles can animate their knob and the whole row can react to hover as
 * one surface.
 */
public class SettingsScreen extends StScreen {

    private static final int ROW_H = 30;
    private static final int ROW_GAP = 8;
    private static final int COL_GAP = 14;

    private final Screen parent;

    /** One setting row. */
    private record Row(
            String id,
            String labelKey,
            java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
    }

    private final List<Row> rows = new ArrayList<>();

    /** Animated knob positions, keyed by setting id. */
    private final java.util.Map<String, Float> knobs =
            new java.util.HashMap<>();
    private final java.util.Map<String, Float> hoverAmounts =
            new java.util.HashMap<>();

    /** Hit boxes of the rows, rebuilt every frame. */
    private final List<int[]> hits = new ArrayList<>();
    private int[] animationHit;
    private int[] speedHit;
    private int[] glowHit;
    private String draggingSlider;

    private int leftX;
    private int leftW;
    private int rightX;
    private int rightW;

    /** Scroll state of the settings column. */
    private float scroll;
    private float scrollPos;
    private float scrollVel;

    /** Viewport of the scrolling column. */
    private int viewTop;
    private int viewBottom;

    public SettingsScreen(Screen parent) {

        super(Lang.text(Key.TITLE_SETTINGS));

        this.parent = parent;
    }

    @Override
    protected int shellTitleAnimation() {
        return StafftoolsClient.getConfig().getTitleAnimation();
    }

    @Override
    protected void init() {

        resetSlots();

        clearChildren();

        configureShell();

        rows.clear();

        StaffToolsConfig config = StafftoolsClient.getConfig();

        add("actions", Key.TOGGLE_ACTIONS_POPUP,
                config::isPlayerActionsEnabled,
                config::setPlayerActionsEnabled);

        add("confirm", Key.TOGGLE_CONFIRM_DANGER,
                config::isDangerousMacroConfirmation,
                config::setDangerousMacroConfirmation);

        add("toasts", Key.TOGGLE_TOASTS,
                config::isToastsEnabled,
                config::setToastsEnabled);

        add("anim", Key.TOGGLE_MENU_ANIMATION,
                config::isMenuAnimation,
                config::setMenuAnimation);

        add("savePopup", Key.TOGGLE_SAVE_POPUP_SIZE,
                config::isSavePopupSize,
                config::setSavePopupSize);

        add("chatFrame", Key.TOGGLE_CHAT_FRAME,
                config::isChatFrameEnabled,
                config::setChatFrameEnabled);

        add("chatResize", Key.TOGGLE_CHAT_RESIZE,
                config::isChatResizeEnabled,
                config::setChatResizeEnabled);

        add("nickHighlight",
                Key.TOGGLE_NICK_HIGHLIGHT_DISABLED,
                config::isNickHighlightDisabled,
                config::setNickHighlightDisabled);

        add("saveChat", Key.TOGGLE_SAVE_CHAT_SIZE,
                config::isSaveChatSize,
                config::setSaveChatSize);

        add("modHud", Key.TOGGLE_MOD_HUD,
                config::isModHudEnabled,
                config::setModHudEnabled);

        add("modHudPhrases", Key.TOGGLE_MOD_HUD_PHRASES,
                config::isModHudPhrases,
                config::setModHudPhrases);

        add("audit", Key.TOGGLE_AUDIT,
                config::isAuditLogEnabled,
                config::setAuditLogEnabled);

        add("profiler", Key.TOGGLE_PROFILER,
                config::isProfilerEnabled,
                config::setProfilerEnabled);

        // two columns: settings on the left, info panel on the right
        int total = contentWidth();

        leftW = Math.min(360, (int) (total * 0.58));
        rightW = total - leftW - COL_GAP;

        leftX = contentLeft();
        rightX = leftX + leftW + COL_GAP;

        // the column scrolls inside the area above the pinned back button;
        // the top offset leaves room for the section title, which is drawn
        // above the viewport so rows never cover it
        viewTop = contentTop() + 34;
        viewBottom = contentBottom() - 34;

        scroll = 0;
        scrollPos = 0;
        scrollVel = 0;
        draggingSlider = null;

        button(
                leftX,
                contentBottom() - 26,
                120,
                Design.H_BUTTON_LG,
                Key.BTN_BACK,
                b -> client.setScreen(
                        parent == null
                                ? new StafftoolsScreen()
                                : parent
                )
        );
    }

    /** Total height of the settings column. */
    private int contentHeight() {

        return rows.size() * (ROW_H + ROW_GAP);
    }

    private int maxScroll() {

        return Math.max(0, contentHeight() - (viewBottom - viewTop));
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (mouseX >= leftX && mouseX <= leftX + leftW &&
                mouseY >= viewTop && mouseY <= viewBottom) {

            scroll += verticalAmount > 0
                    ? -(ROW_H + ROW_GAP)
                    : (ROW_H + ROW_GAP);

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    /** Spring integration, identical to the list screens. */
    private void updateScroll(float delta) {

        float max = maxScroll();
        if (!Ui.animationsEnabled()) {
            scroll = Math.clamp(scroll, 0f, max);
            scrollPos = scroll;
            scrollVel = 0f;
            return;
        }

        float step = Ui.clamp01(delta) * 0.05f;

        float target = Math.clamp(scroll, 0, max);

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scrollPos) * 240f
                            - scrollVel * 19f;

            scrollVel += accel * s;
            scrollPos += scrollVel * s;
        }

        if (Math.abs(scrollPos - target) < 0.05f &&
                Math.abs(scrollVel) < 0.05f) {

            scrollPos = target;
            scrollVel = 0f;
        }
    }

    private void add(
            String id,
            String labelKey,
            java.util.function.Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {

        rows.add(new Row(id, labelKey, getter, setter));
    }

    private float hoverAmount(
            String id,
            boolean hovered,
            float delta
    ) {

        float current = hoverAmounts.getOrDefault(id, 0f);
        float next = Ui.animateTowards(
                current,
                hovered ? 1f : 0f,
                delta,
                14f
        );
        hoverAmounts.put(id, next);
        return next;
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        hits.clear();
        animationHit = null;
        speedHit = null;
        glowHit = null;

        Ui.drawSectionTitle(
                context,
                textRenderer,
                contentLeft(),
                contentTop(),
                "\u2699",
                Lang.t(Key.TITLE_SETTINGS),
                Lang.t(Key.MSG_SYSTEM_HINT),
                alpha
        );

        updateScroll(delta);
        int y = viewTop - Math.round(scrollPos);

        // rows are clipped to the viewport so they can never spill over
        // the header or the pinned back button
        context.enableScissor(
                leftX,
                viewTop,
                leftX + leftW,
                viewBottom
        );

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            Row row = rows.get(rowIndex);

            boolean value = row.getter().get();

            float target = value ? 1f : 0f;

            float current = knobs.getOrDefault(row.id(), target);

            current = Ui.animateTowards(
                    current,
                    target,
                    delta,
                    12f
            );

            knobs.put(row.id(), current);

            boolean visible =
                    y + ROW_H > viewTop && y < viewBottom;

            boolean hovered =
                    visible &&
                            mouseX >= leftX && mouseX <= leftX + leftW &&
                            mouseY >= y && mouseY <= y + ROW_H;
            float hover = hoverAmount(row.id(), hovered, delta);

            if (visible) {

                Ui.drawToggleRow(
                        context,
                        textRenderer,
                        leftX, y, leftW, ROW_H,
                        Lang.t(row.labelKey()),
                        current,
                        hover,
                        alpha
                );

                hits.add(new int[]{
                        leftX, y, leftW, ROW_H, rowIndex
                });
            }

            y += ROW_H + ROW_GAP;
        }

        context.disableScissor();

        // edge fades + scrollbar
        int max = maxScroll();

        Ui.drawScrollFade(
                context,
                leftX,
                viewTop,
                leftW,
                viewBottom,
                Ui.clamp01(scrollPos / 12f),
                Ui.clamp01((max - scrollPos) / 12f),
                alpha
        );

        if (max > 0) {

            int trackH = viewBottom - viewTop;

            Ui.drawScrollbar(
                    context,
                    leftX + leftW - 4,
                    viewTop,
                    trackH,
                    scrollPos / (float) max,
                    trackH / (float) (trackH + max),
                    alpha
            );
        }

        drawInfoPanel(context, mouseX, mouseY, delta, alpha);
    }

    /** Right-hand panel: what the switches do, in plain language. */
    private void drawInfoPanel(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        int top = contentTop() + 34;
        int bottom = contentBottom() - 34;

        Ui.drawPanel(
                context,
                rightX,
                top,
                rightW,
                bottom - top,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                rightX,
                top,
                rightW,
                bottom - top,
                alpha
        );

        int x = rightX + 12;
        int y = top + 12;

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.TITLE_SETTINGS),
                x,
                y,
                Ui.argb(Ui.TEXT, alpha)
        );

        y += 16;

        Ui.drawDivider(context, x, y, rightW - 24, alpha);

        y += 10;
        int selectorY = bottom - 36;
        int speedY = selectorY - 74;
        int glowY = selectorY - 41;
        int controlWidth = Math.max(1, rightW - 24);

        String[] lines = {
                Lang.t(Key.MSG_INTERFACE_HINT),
                "",
                Lang.t(Key.TOGGLE_ACTIONS_POPUP) + " — " +
                        Lang.t(Key.MSG_ACTIONS_HINT),
                Lang.t(Key.TOGGLE_TOASTS) + " — " +
                        Lang.t(Key.MSG_AUDIT_HINT),
                Lang.t(Key.TOGGLE_MOD_HUD) + " — " +
                        Lang.t(Key.HUD_TITLE),
                Lang.t(Key.TOGGLE_AUDIT) + " — " +
                        Lang.t(Key.MSG_AUDIT_HINT)
        };

        int bodyTop = y;
        int bodyBottom = Math.max(bodyTop + 1, speedY - 8);
        context.enableScissor(
                rightX + 8,
                bodyTop,
                rightX + rightW - 8,
                bodyBottom
        );

        for (String line : lines) {

            if (line.isEmpty()) {
                y += 6;
                continue;
            }

            for (String wrapped : wrap(line, Math.max(1, rightW - 24))) {

                context.drawTextWithShadow(
                        textRenderer,
                        net.minecraft.text.Text.literal(wrapped),
                        x,
                        y,
                        Ui.argb(Ui.TEXT_DIM, alpha)
                );

                y += 10;
            }

            y += 4;
        }

        context.disableScissor();

        StaffToolsConfig config = StafftoolsClient.getConfig();

        boolean speedHovered =
                mouseX >= x && mouseX <= x + controlWidth &&
                        mouseY >= speedY && mouseY <= speedY + 27;
        float speedHover = hoverAmount(
                "animation-speed",
                speedHovered,
                delta
        );

        Ui.drawSlider(
                context,
                textRenderer,
                x,
                speedY,
                controlWidth,
                Lang.t(Key.ANIMATION_SPEED),
                config.getAnimationSpeed(),
                50,
                200,
                speedHover,
                alpha
        );

        speedHit = new int[]{x, speedY, controlWidth, 27};

        boolean glowHovered =
                mouseX >= x && mouseX <= x + controlWidth &&
                        mouseY >= glowY && mouseY <= glowY + 27;
        float glowHover = hoverAmount(
                "glow-intensity",
                glowHovered,
                delta
        );

        Ui.drawSlider(
                context,
                textRenderer,
                x,
                glowY,
                controlWidth,
                Lang.t(Key.GLOW_INTENSITY),
                config.getGlowIntensity(),
                0,
                100,
                glowHover,
                alpha
        );

        glowHit = new int[]{x, glowY, controlWidth, 27};

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.TITLE_ANIMATION_LABEL),
                x,
                selectorY - 12,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        String animationKey = switch (
                StafftoolsClient.getConfig().getTitleAnimation()
        ) {
            case 1 -> Key.TITLE_ANIMATION_AURORA;
            case 2 -> Key.TITLE_ANIMATION_PULSE;
            case 3 -> Key.TITLE_ANIMATION_SWEEP;
            default -> Key.TITLE_ANIMATION_STATIC;
        };

        boolean hovered = mouseX >= x && mouseX <= x + rightW - 24 &&
                mouseY >= selectorY && mouseY <= selectorY + 26;
        float selectorHover = hoverAmount(
                "title-animation",
                hovered,
                delta
        );

        Ui.drawSelect(
                context,
                textRenderer,
                x,
                selectorY,
                Math.max(1, rightW - 24),
                26,
                Lang.t(animationKey),
                false,
                selectorHover,
                alpha
        );

        animationHit = new int[]{
                x,
                selectorY,
                Math.max(1, rightW - 24),
                26
        };
    }

    /** Greedy word wrap into the panel width. */
    private List<String> wrap(String text, int maxWidth) {

        List<String> out = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {

            String candidate = current.length() == 0
                    ? word
                    : current + " " + word;

            if (textRenderer.getWidth(candidate) <= maxWidth) {

                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                out.add(current.toString());
                current = new StringBuilder();
            }

            while (textRenderer.getWidth(word) > maxWidth
                    && word.length() > 1) {

                word = word.substring(0, word.length() - 1);
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0) {
            out.add(current.toString());
        }

        return out;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {

        if (click.button() == 0) {

            if (contains(speedHit, click.x(), click.y())) {
                draggingSlider = "speed";
                updateSlider(draggingSlider, click.x());
                StafftoolsClient.saveConfig();
                Ui.playHover();
                return true;
            }

            if (contains(glowHit, click.x(), click.y())) {
                draggingSlider = "glow";
                updateSlider(draggingSlider, click.x());
                StafftoolsClient.saveConfig();
                Ui.playHover();
                return true;
            }

            if (animationHit != null &&
                    click.x() >= animationHit[0] &&
                    click.x() <= animationHit[0] + animationHit[2] &&
                    click.y() >= animationHit[1] &&
                    click.y() <= animationHit[1] + animationHit[3]) {

                StaffToolsConfig config = StafftoolsClient.getConfig();
                config.setTitleAnimation(
                        (config.getTitleAnimation() + 1) % 4
                );
                StafftoolsClient.saveConfig();

                Ui.playHover();
                return true;
            }

            for (int i = 0; i < hits.size(); i++) {

                int[] box = hits.get(i);

                if (click.x() < box[0] || click.x() > box[0] + box[2] ||
                        click.y() < box[1] || click.y() > box[1] + box[3]) {

                    continue;
                }

                Row row = rows.get(box[4]);

                row.setter().accept(!row.getter().get());

                StafftoolsClient.saveConfig();

                Ui.playHover();

                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(
            Click click,
            double offsetX,
            double offsetY
    ) {

        if (draggingSlider != null && click.button() == 0) {
            updateSlider(draggingSlider, click.x());
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {

        if (draggingSlider != null && click.button() == 0) {
            draggingSlider = null;
            StafftoolsClient.saveConfig();
            return true;
        }

        return super.mouseReleased(click);
    }

    private void updateSlider(String id, double mouseX) {

        int[] hit = "speed".equals(id) ? speedHit : glowHit;
        if (hit == null) {
            return;
        }

        int min = "speed".equals(id) ? 50 : 0;
        int max = "speed".equals(id) ? 200 : 100;
        int trackX = hit[0] + 3;
        int trackWidth = Math.max(1, hit[2] - 8);
        float progress = Ui.clamp01(
                (float) (mouseX - trackX) / trackWidth
        );
        int value = Math.round(min + progress * (max - min));

        StaffToolsConfig config = StafftoolsClient.getConfig();
        if ("speed".equals(id)) {
            config.setAnimationSpeed(value);
        } else {
            config.setGlowIntensity(value);
        }
    }

    private static boolean contains(int[] bounds, double x, double y) {

        return bounds != null &&
                x >= bounds[0] && x <= bounds[0] + bounds[2] &&
                y >= bounds[1] && y <= bounds[1] + bounds[3];
    }
}
