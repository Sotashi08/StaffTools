package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.Profile;
import com.uwdie.stafftools.client.config.ProfileIO;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Profile manager: create / save / apply / export / import / delete
 * staff setups. Rows follow the same visual language as the other
 * lists (framed panel, entrance stagger, spring scrolling).
 */
public class ProfilesScreen extends StScreen {

    private static final int ROW_H = 38;
    private static final int ROW_GAP = 4;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int BTN_H = 18;
    private static final int FADE_H = 12;

    /** Rows live inside the frame, never flush with its border. */
    private static final int ROW_PAD = 9;

    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;

    private final Screen parent;

    private float scroll;
    private float scrollPos;
    private float scrollVel;
    private boolean needsRebuild;

    private int listBottom;
    private int rowLeft;
    private int rowWidth;

    private final List<Row> rows = new ArrayList<>();

    public ProfilesScreen(Screen parent) {

        super(Lang.text(Key.TITLE_PROFILES));
        this.parent = parent;
    }

    private static final class Row {

        private final Profile profile;
        private final boolean active;
        private final int baseY;
        private StButton btnOpen;
        private StButton btnExport;
        private StButton btnDelete;

        private Row(Profile profile, boolean active, int baseY) {
            this.profile = profile;
            this.active = active;
            this.baseY = baseY;
        }
    }

    @Override
    public void tick() {

        if (needsRebuild) {
            needsRebuild = false;
            init();
        }
    }

    @Override
    protected void init() {

        rows.clear();
        clearChildren();

        StaffToolsConfig config = StafftoolsClient.getConfig();

        String activeId = config.getActiveProfile().getId();

        rowWidth = Math.min(300, formWidth());
        rowLeft = (width - rowWidth) / 2;

        listBottom = height - BOTTOM_PAD;

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < config.getProfiles().size(); i++) {

            Profile profile = config.getProfiles().get(i);

            Row row = new Row(
                    profile,
                    profile.getId().equals(activeId),
                    LIST_TOP + i * step
            );

            rows.add(row);
            addRowButtons(row);
        }

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;

        int gap = 6;
        int avail = rowWidth - gap * 2;

        int w1 = Math.max(36, avail * 36 / 100);
        int w2 = Math.max(36, avail * 32 / 100);
        int w3 = Math.max(36, avail - w1 - w2);

        button(
                rowLeft,
                height - 32,
                w1,
                22,
                Key.BTN_PROFILE_CREATE,
                b -> create()
        );

        button(
                rowLeft + w1 + gap,
                height - 32,
                w2,
                22,
                Key.BTN_PROFILE_IMPORT,
                b -> client.setScreen(
                        new ProfileImportScreen(this)
                )
        );

        button(
                rowLeft + w1 + w2 + gap * 2,
                height - 32,
                w3,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    private int maxScroll() {

        if (rows.isEmpty()) {
            return 0;
        }

        int content =
                rows.size() * ROW_H +
                        (rows.size() - 1) * ROW_GAP;

        return Math.max(
                0,
                content - (listBottom - LIST_TOP)
        );
    }

    private void addRowButtons(Row row) {

        int y = row.baseY + (ROW_H - BTN_H) / 2;

        int right = rowLeft + rowWidth - ROW_PAD - 5;

        row.btnDelete = button(
                right - 40,
                y,
                40,
                BTN_H,
                Key.BTN_PROFILE_DELETE,
                b -> remove(row.profile),
                Ui.DANGER
        );

        row.btnExport = button(
                right - 40 - 3 - 46,
                y,
                46,
                BTN_H,
                Key.BTN_PROFILE_EXPORT,
                b -> export(row.profile)
        );

        row.btnOpen = button(
                right - 40 - 3 - 46 - 3 - 46,
                y,
                46,
                BTN_H,
                Key.BTN_PROFILE_OPEN,
                b -> open(row.profile)
        );
    }

    /** Buttons follow their row while scrolling; hidden when off-screen. */
    private void positionRowButtons(Row row, int buttonY, boolean visible) {

        row.btnOpen.setY(buttonY);
        row.btnExport.setY(buttonY);
        row.btnDelete.setY(buttonY);

        row.btnOpen.setVisible(visible);
        row.btnExport.setVisible(visible);
        row.btnDelete.setVisible(visible);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (button == 0) {

            Row row = rowAt(mouseX, mouseY);

            // a click on the row itself applies the profile right away
            if (row != null && !overRowButtons(row, mouseX, mouseY)) {
                apply(row.profile);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Row under the cursor, or null. */
    private Row rowAt(double mouseX, double mouseY) {

        if (mouseX < rowLeft + ROW_PAD ||
                mouseX > rowLeft + rowWidth - ROW_PAD) {

            return null;
        }

        if (mouseY < LIST_TOP || mouseY >= listBottom) {
            return null;
        }

        int step = ROW_H + ROW_GAP;

        int index = (int) ((mouseY - LIST_TOP + scrollPos) / step);

        if (index < 0 || index >= rows.size()) {
            return null;
        }

        return rows.get(index);
    }

    /** True when the cursor sits on one of the row's own buttons. */
    private boolean overRowButtons(Row row, double mouseX, double mouseY) {

        int index = rows.indexOf(row);

        int top = LIST_TOP + index * (ROW_H + ROW_GAP)
                - Math.round(scrollPos);

        int buttonY = top + (ROW_H - BTN_H) / 2;

        if (mouseY < buttonY || mouseY >= buttonY + BTN_H) {
            return false;
        }

        int right = rowLeft + rowWidth - ROW_PAD - 5;

        int deleteX = right - 40;
        int exportX = right - 40 - 3 - 46;
        int openX = right - 40 - 3 - 46 - 3 - 46;

        return mouseX >= openX && mouseX <= right;
    }

    /** Applies a profile (and its theme) without opening the editor. */
    private void apply(Profile profile) {

        StaffToolsConfig config = StafftoolsClient.getConfig();

        config.switchProfile(profile.getId());

        // the theme belongs to the profile: apply it right away
        Theme.apply(config.getThemeIndex());

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_APPLIED, profile.getName())
        );

        needsRebuild = true;
    }

    private void create() {

        StaffToolsConfig config = StafftoolsClient.getConfig();

        // the new profile starts as a snapshot of what is live right now
        Profile profile = config.createProfile(null);

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_CREATED, profile.getName())
        );

        needsRebuild = true;
    }

    private void open(Profile profile) {

        // make sure the live settings match what the editor shows
        apply(profile);

        client.setScreen(new ProfileEditScreen(this, profile.getId()));
    }

    private void export(Profile profile) {

        Optional<Path> file = ProfileIO.export(profile);

        if (file.isEmpty()) {

            Toasts.INSTANCE.warn(
                    Lang.t(Key.TOAST_PROFILE_EXPORT_FAILED)
            );

            return;
        }

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_EXPORTED,
                        file.get().getFileName().toString())
        );
    }

    private void remove(Profile profile) {

        StaffToolsConfig config = StafftoolsClient.getConfig();

        if (!config.deleteProfile(profile.getId())) {

            Toasts.INSTANCE.warn(
                    Lang.t(Key.TOAST_PROFILE_LAST)
            );

            return;
        }

        // deleting the active profile activates another one:
        // its theme must follow
        Theme.apply(config.getThemeIndex());

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_DELETED, profile.getName())
        );

        needsRebuild = true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        float step = ROW_H + ROW_GAP;

        scroll += verticalAmount > 0 ? -step : step;

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

        drawTitle(context, Key.TITLE_PROFILES, alpha);

        drawHint(context, Key.MSG_PROFILES_HINT, 30, alpha);

        Ui.drawPanel(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                rowLeft,
                LIST_TOP - 8,
                rowWidth,
                listBottom - LIST_TOP + 16,
                alpha
        );

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_PROFILES_EMPTY),
                    width / 2,
                    LIST_TOP + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        long now = System.currentTimeMillis();

        updateScroll(delta);

        int shift = (int) ((1f - alpha) * 14f);

        context.enableScissor(
                rowLeft + ROW_PAD,
                LIST_TOP + shift,
                rowLeft + rowWidth - ROW_PAD,
                listBottom + shift
        );

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            int y = LIST_TOP + i * (ROW_H + ROW_GAP)
                    - Math.round(scrollPos);

            boolean drawable =
                    y + ROW_H > LIST_TOP && y < listBottom;

            int buttonY = y + (ROW_H - BTN_H) / 2;

            positionRowButtons(
                    row,
                    buttonY,
                    drawable &&
                            buttonY >= LIST_TOP &&
                            buttonY + BTN_H <= listBottom
            );

            if (!drawable) {
                continue;
            }

            float rowAlpha = Ui.clamp01(
                    (now - openTime - i * 22L) / 180f
            );

            if (rowAlpha <= 0.01f) {
                continue;
            }

            drawRow(context, row, y, mouseX, mouseY, alpha * rowAlpha);
        }

        context.disableScissor();

        drawScrollMasks(context, alpha);
    }

    private void drawRow(
            DrawContext context,
            Row row,
            int y,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        Profile profile = row.profile;

        boolean hovered =
                mouseX >= rowLeft + ROW_PAD &&
                        mouseX <= rowLeft + rowWidth - ROW_PAD &&
                        mouseY >= y &&
                        mouseY <= y + ROW_H - 2;

        int bg = hovered
                ? Ui.argb(0x403F8AE0, alpha)
                : Ui.argb(Ui.PANEL_BG_SOFT, alpha * 0.85f);

        Ui.drawRoundRect(
                context,
                rowLeft + ROW_PAD,
                y,
                rowWidth - ROW_PAD * 2,
                ROW_H - 2,
                3,
                bg
        );

        Ui.drawRoundBorder(
                context,
                rowLeft + ROW_PAD,
                y,
                rowWidth - ROW_PAD * 2,
                ROW_H - 2,
                3,
                Ui.argb(
                        hovered
                                ? Ui.ACCENT
                                : row.active
                                ? Ui.ACCENT_DARK
                                : 0xFF2C3B55,
                        alpha * (hovered || row.active ? 1f : 0.55f)
                )
        );

        String name =
                textRenderer.trimToWidth(
                        profile.getName(),
                        rowWidth - ROW_PAD * 2 - 156
                );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                rowLeft + ROW_PAD + 10,
                y + 5,
                Ui.argb(
                        row.active ? Ui.ACCENT_SOFT : Ui.TEXT,
                        alpha
                )
        );

        String details =
                Lang.t(Key.MSG_PROFILE_MACROS) + ": " +
                        profile.getMacros().size() +
                        "  •  " +
                        Lang.t(Key.MSG_PROFILE_ACTIONS) + ": " +
                        profile.getActionEntries().size() +
                        "  •  " +
                        Lang.t(Theme.nameKey(profile.getThemeIndex()));

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(
                                details,
                                rowWidth - ROW_PAD * 2 - 156
                        )
                ),
                rowLeft + ROW_PAD + 10,
                y + 17,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        if (row.active) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(Lang.t(Key.MSG_PROFILE_ACTIVE)),
                    rowLeft + rowWidth - ROW_PAD - 5 -
                            textRenderer.getWidth(
                                    Lang.t(Key.MSG_PROFILE_ACTIVE)
                            ),
                    y + 5,
                    Ui.argb(Ui.SUCCESS, alpha)
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
                    rowLeft + ROW_PAD,
                    LIST_TOP,
                    rowLeft + rowWidth - ROW_PAD,
                    LIST_TOP + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    rowLeft + ROW_PAD,
                    listBottom - FADE_H,
                    rowLeft + rowWidth - ROW_PAD,
                    listBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }
}
