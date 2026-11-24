package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.Profile;
import com.uwdie.stafftools.client.config.ProfileIO;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Profile manager: create / save / apply / export / import / delete
 * staff setups. Rows are cards with the profile name, a summary line and
 * its own buttons; the active profile is marked with an accent edge.
 */
public class ProfilesScreen extends StListScreen {

    private static final int ROW_H = 38;
    private static final int ROW_GAP = 4;
    private static final int LIST_TOP = 46;
    private static final int BOTTOM_PAD = 44;
    private static final int BTN_H = 18;

    private final Screen parent;

    private boolean needsRebuild;

    private final List<Row> rows = new ArrayList<>();

    public ProfilesScreen(Screen parent) {

        super(Lang.text(Key.TITLE_PROFILES));
        this.parent = parent;
    }

    private static final class Row {

        private final Profile profile;
        private final boolean active;
        private StButton btnOpen;
        private StButton btnExport;
        private StButton btnDelete;

        private Row(Profile profile, boolean active) {
            this.profile = profile;
            this.active = active;
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

        resetSlots();

        rows.clear();
        clearChildren();

        configureShell();

        StaffToolsConfig config = StafftoolsClient.getConfig();

        String activeId = config.getActiveProfile().getId();

        rowWidth = Math.min(440, contentWidth());
        rowLeft = contentLeft();

        listTop = contentTop() + 40;
        listBottom = contentBottom() - 40;

        for (Profile profile : config.getProfiles()) {

            Row row = new Row(
                    profile,
                    profile.getId().equals(activeId)
            );

            rows.add(row);
            addRowButtons(row);
        }

        resetScroll();

        int gap = 6;
        int avail = rowWidth - gap * 2;

        int w1 = Math.max(36, avail * 36 / 100);
        int w2 = Math.max(36, avail * 32 / 100);
        int w3 = Math.max(36, avail - w1 - w2);

        button(
                rowLeft,
                contentBottom() - 26,
                w1,
                Design.H_BUTTON_LG,
                Key.BTN_PROFILE_CREATE,
                b -> create()
        );

        button(
                rowLeft + w1 + gap,
                contentBottom() - 26,
                w2,
                Design.H_BUTTON_LG,
                Key.BTN_PROFILE_IMPORT,
                b -> client.setScreen(
                        new ProfileImportScreen(this)
                )
        );

        button(
                rowLeft + w1 + w2 + gap * 2,
                contentBottom() - 26,
                w3,
                Design.H_BUTTON_LG,
                Key.BTN_BACK,
                b -> client.setScreen(new StafftoolsScreen())
        );
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected int rowStep() {
        return ROW_H + ROW_GAP;
    }

    @Override
    protected int rowHeight() {
        return ROW_H;
    }

    private void addRowButtons(Row row) {

        int right = rowLeft + rowWidth - ROW_PAD - 5;

        row.btnDelete = button(
                right - 40,
                0,
                40,
                BTN_H,
                Key.BTN_PROFILE_DELETE,
                b -> remove(row.profile),
                Ui.DANGER
        );

        row.btnExport = button(
                right - 40 - 3 - 46,
                0,
                46,
                BTN_H,
                Key.BTN_PROFILE_EXPORT,
                b -> export(row.profile)
        );

        row.btnOpen = button(
                right - 40 - 3 - 46 - 3 - 46,
                0,
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
            Click click,
            boolean doubled
    ) {

        if (click.button() == 0) {

            Row row = rowAt(click.x(), click.y());

            // a click on the row itself applies the profile right away
            if (row != null &&
                    !overRowButtons(row, click.x(), click.y())) {

                apply(row.profile);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    /** Row under the cursor, or null. */
    private Row rowAt(double mouseX, double mouseY) {

        if (!insideList(mouseX, mouseY)) {
            return null;
        }

        int index = rowIndexAt(mouseY);

        return index >= 0 ? rows.get(index) : null;
    }

    /** True when the cursor sits on one of the row's own buttons. */
    private boolean overRowButtons(
            Row row,
            double mouseX,
            double mouseY
    ) {

        int index = rows.indexOf(row);

        int top = rowY(index);

        int buttonY = top + (ROW_H - BTN_H) / 2;

        if (mouseY < buttonY || mouseY >= buttonY + BTN_H) {
            return false;
        }

        int right = rowLeft + rowWidth - ROW_PAD - 5;

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

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

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
                contentTop(),
                "\uD83D\uDC64",
                Lang.t(Key.TITLE_PROFILES),
                Lang.t(Key.MSG_PROFILES_HINT),
                alpha
        );

        renderList(context, mouseX, mouseY, delta, alpha);

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_PROFILES_EMPTY),
                    rowLeft + rowWidth / 2,
                    listTop + 26,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    @Override
    protected void renderRows(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            int y = rowY(i);

            boolean drawable = rowVisible(i);

            int buttonY = y + (ROW_H - BTN_H) / 2;

            positionRowButtons(
                    row,
                    buttonY,
                    drawable &&
                            buttonY >= listTop &&
                            buttonY + BTN_H <= listBottom
            );

            if (!drawable) {
                continue;
            }

            float rowAlpha = rowAlpha(i);

            if (rowAlpha <= 0.01f) {
                continue;
            }

            drawRow(
                    context,
                    row,
                    y,
                    mouseX,
                    mouseY,
                    alpha * rowAlpha
            );
        }
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

        int rx = rowLeft + ROW_PAD;
        int rw = rowWidth - ROW_PAD * 2;
        int rh = ROW_H - 2;

        boolean hovered =
                mouseX >= rx && mouseX <= rx + rw &&
                        mouseY >= y && mouseY <= y + rh;

        if (row.active) {

            Ui.drawCardAccent(
                    context, rx, y, rw, rh,
                    Ui.ACCENT, alpha
            );

        } else if (hovered) {

            Ui.drawRoundRect(
                    context, rx, y, rw, rh, Design.RADIUS_MD,
                    Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.16f), alpha)
            );

            Ui.drawRoundBorder(
                    context, rx, y, rw, rh, Design.RADIUS_MD,
                    Ui.argb(Ui.ACCENT, alpha * 0.9f)
            );

        } else {

            Ui.drawCard(context, rx, y, rw, rh, alpha);
        }

        int textW = Math.max(50, rw - 160);

        String name = textRenderer.trimToWidth(
                profile.getName(),
                textW
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                rx + 10,
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
                        textRenderer.trimToWidth(details, textW)
                ),
                rx + 10,
                y + 17,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        if (row.active) {

            String badge = Lang.t(Key.MSG_PROFILE_ACTIVE);

            Ui.drawPill(
                    context,
                    textRenderer,
                    rx + rw - 160 - Ui.pillWidth(textRenderer, badge) - 6,
                    y + 5,
                    badge,
                    Ui.SUCCESS,
                    alpha
            );
        }
    }
}
