package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.Profile;
import com.uwdie.stafftools.client.config.ProfileIO;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.ui.widget.StButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Import screen: shows every {@code .json} profile file found in the mod
 * profile folder and adds the picked one to the local profile list.
 * The folder itself can be opened directly in the OS file manager.
 */
public class ProfileImportScreen extends StScreen {

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 3;
    private static final int LIST_TOP = 46;
    private static final int FADE_H = 12;

    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;

    private final Screen parent;

    private float scroll;
    private float scrollPos;
    private float scrollVel;

    private int listBottom;
    private int rowLeft;
    private int rowWidth;

    private final List<StButton> rowWidgets = new ArrayList<>();
    private int fileCount;

    public ProfileImportScreen(Screen parent) {

        super(Lang.text(Key.TITLE_PROFILE_IMPORT));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    /** Re-reads the folder and recreates every widget. */
    private void rebuild() {

        clearChildren();

        rowWidgets.clear();

        List<String> files = ProfileIO.listFiles();

        fileCount = files.size();

        rowWidth = Math.min(280, formWidth());
        rowLeft = (width - rowWidth) / 2;

        listBottom = height - 66;

        scroll = Math.min(scroll, maxScroll());
        scrollPos = scroll;
        scrollVel = 0f;

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < files.size(); i++) {

            final String file = files.get(i);

            StButton row = new StButton(
                    rowLeft,
                    LIST_TOP + i * step,
                    rowWidth,
                    ROW_H,
                    Text.literal(file),
                    b -> importFile(file),
                    openTime,
                    nextSlot(),
                    Ui.ACCENT
            );

            addDrawableChild(row);
            rowWidgets.add(row);
        }

        int half = (rowWidth - 8) / 2;

        button(
                rowLeft,
                height - 32,
                half,
                22,
                Key.BTN_PROFILE_OPEN_FOLDER,
                b -> openFolder()
        );

        button(
                rowLeft + half + 8,
                height - 32,
                half,
                22,
                Key.BTN_REFRESH,
                b -> rebuild()
        );

        button(
                rowLeft,
                height - 58,
                rowWidth,
                20,
                Key.BTN_BACK,
                b -> client.setScreen(new ProfilesScreen(parent))
        );
    }

    private int maxScroll() {

        if (fileCount == 0) {
            return 0;
        }

        int content = fileCount * (ROW_H + ROW_GAP);

        return Math.max(0, content - (listBottom - LIST_TOP));
    }

    private void openFolder() {

        if (ProfileIO.openProfilesFolder()) {

            Toasts.INSTANCE.success(
                    Lang.t(Key.TOAST_PROFILE_FOLDER_OPENED)
            );

            return;
        }

        Toasts.INSTANCE.info(
                Lang.t(Key.MSG_PROFILE_FOLDER, ProfileIO.folderName())
        );
    }

    private void importFile(String file) {

        Profile imported = ProfileIO.importFromFile(file);

        if (imported == null) {

            Toasts.INSTANCE.warn(
                    Lang.t(Key.TOAST_PROFILE_BAD_JSON)
            );

            return;
        }

        StafftoolsClient.getConfig()
                .getProfiles()
                .add(imported);

        StafftoolsClient.saveConfig();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_PROFILE_IMPORTED, imported.getName())
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

    /** File rows are widgets: keep them glued to the scroll position. */
    private void positionRows() {

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < rowWidgets.size(); i++) {

            StButton row = rowWidgets.get(i);

            int y = LIST_TOP + i * step - Math.round(scrollPos);

            boolean visible =
                    y + ROW_H > LIST_TOP && y < listBottom;

            row.setY(y);
            row.setVisible(visible);
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

        drawTitle(context, Key.TITLE_PROFILE_IMPORT, alpha);

        drawHint(context, Key.MSG_PROFILE_IMPORT_HINT, 30, alpha);

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

        if (fileCount == 0) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_PROFILE_NO_FILES),
                    width / 2,
                    LIST_TOP + 24,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        updateScroll(delta);

        positionRows();

        int shift = (int) ((1f - alpha) * 14f);

        context.enableScissor(
                rowLeft + 9,
                LIST_TOP + shift,
                rowLeft + rowWidth - 9,
                listBottom + shift
        );

        long now = System.currentTimeMillis();

        int step = ROW_H + ROW_GAP;

        for (int i = 0; i < fileCount; i++) {

            int y = LIST_TOP + i * step - Math.round(scrollPos);

            if (y + ROW_H <= LIST_TOP || y >= listBottom) {
                continue;
            }

            float rowAlpha = Ui.clamp01(
                    (now - openTime - i * 20L) / 170f
            );

            if (rowAlpha <= 0.01f) {
                continue;
            }

            context.fill(
                    rowLeft + 9,
                    y,
                    rowLeft + rowWidth - 9,
                    y + ROW_H,
                    Ui.argb(Ui.PANEL_BG_SOFT, alpha * rowAlpha * 0.8f)
            );
        }

        context.disableScissor();

        drawScrollMasks(context, alpha);
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
                    rowLeft + 9,
                    LIST_TOP,
                    rowLeft + rowWidth - 9,
                    LIST_TOP + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    rowLeft + 9,
                    listBottom - FADE_H,
                    rowLeft + rowWidth - 9,
                    listBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }
}
