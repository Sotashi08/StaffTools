package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * Settings screen: every switch of the mod in a two-column grid.
 *
 * The main menu only navigates now; the toggles it used to carry live
 * here, next to the chat-related ones that were added afterwards.
 */
public class SettingsScreen extends StScreen {

    private static final int ROW_STEP = 22;
    private static final int WIDGET_H = 20;

    private static final int TOP = 46;
    private static final int GAP = 8;

    private final Screen parent;

    private int colWidth;
    private int left;
    private int rows;

    public SettingsScreen(Screen parent) {

        super(Lang.text(Key.TITLE_SETTINGS));

        this.parent = parent;
    }

    @Override
    protected void init() {

        resetSlots();

        colWidth = (Math.min(formWidth() - GAP, 420) - GAP) / 2;
        left = (width - colWidth * 2 - GAP) / 2;

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        int row = 0;

        addToggle(row++, Key.TOGGLE_ACTIONS_POPUP,
                config.isPlayerActionsEnabled(),
                config::setPlayerActionsEnabled);

        addToggle(row++, Key.TOGGLE_CONFIRM_DANGER,
                config.isDangerousMacroConfirmation(),
                config::setDangerousMacroConfirmation);

        addToggle(row++, Key.TOGGLE_TOASTS,
                config.isToastsEnabled(),
                config::setToastsEnabled);

        addToggle(row++, Key.TOGGLE_SAVE_POPUP_SIZE,
                config.isSavePopupSize(),
                config::setSavePopupSize);

        addToggle(row++, Key.TOGGLE_SAVE_CHAT_SIZE,
                config.isSaveChatSize(),
                config::setSaveChatSize);

        addToggle(row++, Key.TOGGLE_CHAT_FRAME,
                config.isChatFrameEnabled(),
                config::setChatFrameEnabled);

        addToggle(row++, Key.TOGGLE_CHAT_RESIZE,
                config.isChatResizeEnabled(),
                config::setChatResizeEnabled);

        addToggle(row++, Key.TOGGLE_MOD_HUD,
                config.isModHudEnabled(),
                config::setModHudEnabled);

        addToggle(row++, Key.TOGGLE_MOD_HUD_PHRASES,
                config.isModHudPhrases(),
                config::setModHudPhrases);

        addToggle(row++, Key.TOGGLE_MENU_ANIMATION,
                config.isMenuAnimation(),
                config::setMenuAnimation);

        rows = row;

        panelW = colWidth * 2 + GAP + 16;
        panelX = (width - panelW) / 2;
        panelBottom = TOP + (rows + 1) / 2 * ROW_STEP + 6;

        button(
                panelX + 8,
                panelBottom + 8,
                panelW - 16,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(
                        parent == null
                                ? new StafftoolsScreen()
                                : parent
                )
        );
    }

    private int panelW;
    private int panelX;
    private int panelBottom;

    private void addToggle(
            int index,
            String key,
            boolean initial,
            java.util.function.Consumer<Boolean> setter
    ) {

        int column = index % 2;
        int row = index / 2;

        toggle(
                left + column * (colWidth + GAP),
                TOP + row * ROW_STEP,
                colWidth,
                WIDGET_H,
                key,
                initial,
                v -> {
                    setter.accept(v);
                    StafftoolsClient.saveConfig();
                }
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

        drawTitle(context, Key.TITLE_SETTINGS, alpha);

        Ui.drawPanel(
                context,
                panelX,
                TOP - 10,
                panelW,
                panelBottom - TOP + 10,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                panelX,
                TOP - 10,
                panelW,
                panelBottom - TOP + 10,
                alpha
        );
    }
}
