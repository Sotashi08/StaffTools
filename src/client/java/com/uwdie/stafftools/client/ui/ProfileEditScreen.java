package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.config.Profile;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor of a single profile.
 *
 * Layout (top to bottom):
 *   - name field
 *   - theme swatch strip + active theme name
 *   - two independent columns: macros on the left, actions on the right
 *   - save / back
 *
 * Both columns scroll together and are clipped to the panel, so nothing
 * ever overlaps the frame or each other.
 */
public class ProfileEditScreen extends StScreen {

    private static final int PAD = 10;
    private static final int NAME_Y = 62;
    private static final int THEME_LABEL_Y = 92;
    private static final int THEME_Y = 104;
    private static final int HDR_Y = 134;
    private static final int LIST_TOP = 146;
    private static final int ENTRY_H = 14;
    private static final int ENTRY_GAP = 2;
    private static final int COL_GAP = 10;
    private static final int FADE_H = 12;

    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;

    private final Screen parent;
    private final String profileId;

    private StTextField nameField;

    private float scroll;
    private float scrollPos;
    private float scrollVel;

    private int panelLeft;
    private int panelWidth;
    private int listBottom;
    private int colW;
    private int col2X;

    private final List<String> macroLines = new ArrayList<>();
    private final List<Boolean> macroDims = new ArrayList<>();
    private final List<String> actionLines = new ArrayList<>();
    private final List<Boolean> actionDims = new ArrayList<>();

    public ProfileEditScreen(Screen parent, String profileId) {

        super(Lang.text(Key.TITLE_PROFILE_EDIT));
        this.parent = parent;
        this.profileId = profileId;
    }

    @Override
    protected void init() {

        clearChildren();

        StaffToolsConfig config = StafftoolsClient.getConfig();
        Profile profile = config.findProfile(profileId);

        panelWidth = Math.min(320, formWidth());
        panelLeft = (width - panelWidth) / 2;

        int inner = panelWidth - PAD * 2;

        colW = (inner - COL_GAP) / 2;
        col2X = panelLeft + PAD + colW + COL_GAP;

        listBottom = height - 44;

        nameField = new StTextField(
                textRenderer,
                panelLeft + PAD,
                NAME_Y,
                inner,
                20,
                Lang.text(Key.LABEL_PROFILE_NAME),
                nextSlot()
        );

        nameField.setMaxLength(32);

        if (profile != null) {
            nameField.setText(profile.getName());
        }

        addDrawableChild(nameField);

        buildColumns(profile);

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;

        int half = (inner - 8) / 2;

        button(
                panelLeft + PAD,
                height - 32,
                half,
                22,
                Key.BTN_PROFILE_SAVE,
                b -> save()
        );

        button(
                panelLeft + PAD + half + 8,
                height - 32,
                half,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(new ProfilesScreen(parent))
        );
    }

    private void buildColumns(Profile profile) {

        macroLines.clear();
        macroDims.clear();
        actionLines.clear();
        actionDims.clear();

        if (profile == null) {
            return;
        }

        for (Macro macro : profile.getMacros()) {

            macroLines.add("📌 " + macro.getName());
            macroDims.add(!macro.isEnabled());
        }

        for (ActionEntry entry : profile.getActionEntries()) {

            if (entry.isCopyName()) {
                continue;
            }

            String icon =
                    entry.getIcon() == null
                            ? ""
                            : entry.getIcon() + " ";

            actionLines.add(icon + entry.getLabel());
            actionDims.add(!entry.isEnabled());
        }
    }

    /** Longest column drives the scroll range. */
    private int maxScroll() {

        int rows = Math.max(
                macroLines.size(),
                actionLines.size()
        );

        if (rows == 0) {
            return 0;
        }

        int content = rows * (ENTRY_H + ENTRY_GAP);

        return Math.max(0, content - (listBottom - LIST_TOP));
    }

    private void applyTheme(int index) {

        StaffToolsConfig config = StafftoolsClient.getConfig();
        Profile profile = config.findProfile(profileId);

        if (profile != null) {
            profile.setThemeIndex(index);
        }

        // the live config must follow: saving re-snapshots the active
        // profile from it, and would otherwise roll the theme back
        config.setThemeIndex(index);

        Theme.apply(index);

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_THEME_CHANGED,
                        Lang.t(Theme.nameKey(index)))
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


if (button == 0 &&
                mouseY >= THEME_Y &&
                mouseY <= THEME_Y + 16) {

            if (clickTheme(mouseX)) {
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    /** True when the click landed on a theme swatch. */
    private boolean clickTheme(double mouseX) {

        int themes = Theme.count();
        int gap = 2;

        int swW = swatchWidth();

        int stripW = themes * swW + (themes - 1) * gap;

        int stripX = panelLeft + (panelWidth - stripW) / 2;

        int rel = (int) mouseX - stripX;

        if (rel < 0 || rel >= stripW) {
            return false;
        }

        int index = rel / (swW + gap);

        if (index < 0 || index >= themes) {
            return false;
        }

        applyTheme(index);

        return true;
    }

    private int swatchWidth() {

        int themes = Theme.count();
        int gap = 2;

        return Math.max(
                12,
                (panelWidth - PAD * 2 - (themes - 1) * gap) / themes
        );
    }

    private void save() {

        StaffToolsConfig config = StafftoolsClient.getConfig();
        Profile profile = config.findProfile(profileId);

        if (profile != null) {

            profile.setName(nameField.getText());

            // keep the snapshot in sync when this is the active profile
            if (profile.getId().equals(
                    config.getActiveProfile().getId())) {

                profile.capture(config);
            }
        }

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_SAVED,
                        profile != null ? profile.getName() : "")
        );

        client.setScreen(new ProfilesScreen(parent));
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        scroll += verticalAmount > 0
                ? -(ENTRY_H + ENTRY_GAP)
                : (ENTRY_H + ENTRY_GAP);

        return true;
    }

    private void updateScroll(float delta) {

        float step = Ui.clamp01(delta) * 0.05f;
        float max = maxScroll();

        float target = Math.clamp(
                scroll,
                -SCROLL_RUBBER,
                max + SCROLL_RUBBER
        );

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scrollPos) * SCROLL_STIFF
                            - scrollVel * SCROLL_DAMP;

            float over = scrollPos < 0f
                    ? scrollPos
                    : scrollPos > max
                    ? scrollPos - max
                    : 0f;

            if (over != 0f) {
                accel -= over * EDGE_STIFF;
                accel -= scrollVel * EDGE_DAMP;
            }

            scrollVel += accel * s;
            scrollPos += scrollVel * s;
        }

        if (Math.abs(scrollPos - target) < 0.05f &&
                Math.abs(scrollVel) < 0.05f) {

            scrollPos = target;
            scrollVel = 0f;
        }
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawTitle(context, Key.TITLE_PROFILE_EDIT, alpha);

        StaffToolsConfig config = StafftoolsClient.getConfig();
        Profile profile = config.findProfile(profileId);

        // one panel behind everything
        Ui.drawPanel(
                context,
                panelLeft,
                34,
                panelWidth,
                height - 34 - 26,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                panelLeft,
                34,
                panelWidth,
                height - 34 - 26,
                alpha
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_PROFILE_NAME),
                panelLeft + PAD,
                NAME_Y - 12,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_PROFILE_THEME),
                panelLeft + PAD,
                THEME_LABEL_Y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        int activeTheme = profile == null
                ? 0
                : profile.getThemeIndex();

        // theme swatch strip
        int themes = Theme.count();
        int gap = 2;

        int swW = swatchWidth();

        int stripW = themes * swW + (themes - 1) * gap;
        int stripX = panelLeft + (panelWidth - stripW) / 2;

        for (int i = 0; i < themes; i++) {

            int x = stripX + i * (swW + gap);

            boolean hovered =
                    mouseX >= x && mouseX <= x + swW &&
                            mouseY >= THEME_Y &&
                            mouseY <= THEME_Y + 16;

            boolean active = i == activeTheme;

            Ui.drawRoundRect(
                    context,
                    x,
                    THEME_Y,
                    swW,
                    16,
                    3,
                    Ui.argb(
                            Ui.mix(
                                    Ui.PANEL_BG,
                                    Theme.accentOf(i),
                                    hovered || active ? 0.85f : 0.55f
                            ),
                            alpha
                    )
            );

            Ui.drawRoundBorder(
                    context,
                    x,
                    THEME_Y,
                    swW,
                    16,
                    3,
                    Ui.argb(
                            active
                                    ? Ui.TEXT
                                    : hovered
                                            ? Theme.accentOf(i)
                                            : Ui.PANEL_BORDER,
                            alpha
                    )
            );

            if (hovered) {

                context.drawCenteredTextWithShadow(
                        textRenderer,
                        Text.literal("✓"),
                        x + swW / 2,
                        THEME_Y + 4,
                        Ui.argb(Ui.TEXT, alpha)
                );
            }
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(Lang.t(Theme.nameKey(activeTheme))),
                width / 2,
                THEME_Y + 22,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        // column headers
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.MSG_PROFILE_MACROS) +
                                " (" + macroLines.size() + ")"
                ),
                panelLeft + PAD,
                HDR_Y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.MSG_PROFILE_ACTIONS) +
                                " (" + actionLines.size() + ")"
                ),
                col2X,
                HDR_Y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.fill(
                panelLeft + PAD,
                HDR_Y + 10,
                panelLeft + panelWidth - PAD,
                HDR_Y + 11,
                Ui.argb(0x2C3B55, alpha)
        );

        if (macroLines.isEmpty() && actionLines.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_PROFILE_NONE),
                    width / 2,
                    LIST_TOP + 20,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        updateScroll(delta);

        int shift = (int) ((1f - alpha) * 14f);

        // one clip box for both columns
        context.enableScissor(
                panelLeft + PAD,
                LIST_TOP,
                panelLeft + panelWidth - PAD,
                listBottom
        );

        long now = System.currentTimeMillis();

        drawColumn(
                context,
                now,
                panelLeft + PAD,
                macroLines,
                macroDims,
                alpha,
                -1
        );

        drawColumn(
                context,
                now,
                col2X,
                actionLines,
                actionDims,
                alpha,
                macroLines.size()
        );

        context.disableScissor();

        drawScrollMasks(context, alpha);
    }

    private void drawColumn(
            DrawContext context,
            long now,
            int x,
            List<String> lines,
            List<Boolean> dims,
            float alpha,
            int staggerOffset
    ) {

        for (int i = 0; i < lines.size(); i++) {

            int y = LIST_TOP + i * (ENTRY_H + ENTRY_GAP)
                    - Math.round(scrollPos);

            if (y + ENTRY_H <= LIST_TOP || y >= listBottom) {
                continue;
            }

            float appear = Ui.clamp01(
                    (now - openTime
                            - (i + staggerOffset) * 14L) / 160f
            );

            if (appear <= 0.01f) {
                continue;
            }

            if (i % 2 == 0) {

                context.fill(
                        x,
                        y,
                        x + colW,
                        y + ENTRY_H,
                        Ui.argb(
                                Ui.PANEL_BG_SOFT,
                                alpha * appear * 0.9f
                        )
                );
            }

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(
                                    lines.get(i),
                                    colW - 4
                            )
                    ),
                    x + 2,
                    y + 3,
                    Ui.argb(
                            dims.get(i) ? Ui.TEXT_MUTED : Ui.TEXT,
                            alpha * appear
                    )
            );
        }
    }

    private void drawScrollMasks(
            DrawContext context,
            float alpha
    ) {

        float top = Ui.clamp01(scrollPos / FADE_H);

        float bottom = Ui.clamp01(
                (maxScroll() - scrollPos) / FADE_H
        );

        if (top > 0.01f) {

            context.fillGradient(
                    panelLeft + PAD,
                    LIST_TOP,
                    panelLeft + panelWidth - PAD,
                    LIST_TOP + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    panelLeft + PAD,
                    listBottom - FADE_H,
                    panelLeft + panelWidth - PAD,
                    listBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }
}
