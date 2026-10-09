package com.uwdie.stafftools.client.config;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.macro.Macro;

import java.util.ArrayList;
import java.util.List;

public class StaffToolsConfig {

    /** Default popup geometry, also used when saving is turned off. */
    public static final int DEFAULT_POPUP_WIDTH = 208;
    public static final int DEFAULT_POPUP_HEIGHT = 240;

    /** Hard limits for the manual resize handle. */
    public static final int MIN_POPUP_WIDTH = 150;
    public static final int MIN_POPUP_HEIGHT = 104;
    public static final int MAX_POPUP_WIDTH = 512;
    public static final int MAX_POPUP_HEIGHT = 720;

    private boolean chatMentionsEnabled = true;
    private boolean clickToCopyEnabled = true;
    private boolean playerActionsEnabled = true;

    private boolean dangerousMacroConfirmation = true;

    private boolean toastsEnabled = true;

    private boolean savePopupSize = true;
    private int popupWidth = DEFAULT_POPUP_WIDTH;
    private int popupHeight = DEFAULT_POPUP_HEIGHT;

    private int themeIndex = 0;

    private boolean chatInputDetectEnabled = true;

    private Lang.Language language = Lang.Language.RU;

    private List<Macro> macros = new ArrayList<>();

    private List<ActionEntry> actionEntries =
            ActionEntry.defaults();

    public StaffToolsConfig() {
    }

    public boolean isChatMentionsEnabled() {
        return chatMentionsEnabled;
    }

    public void setChatMentionsEnabled(boolean enabled) {
        this.chatMentionsEnabled = enabled;
    }

    public boolean isClickToCopyEnabled() {
        return clickToCopyEnabled;
    }

    public void setClickToCopyEnabled(boolean enabled) {
        this.clickToCopyEnabled = enabled;
    }

    public boolean isPlayerActionsEnabled() {
        return playerActionsEnabled;
    }

    public void setPlayerActionsEnabled(boolean enabled) {
        this.playerActionsEnabled = enabled;
    }

    public boolean isDangerousMacroConfirmation() {
        return dangerousMacroConfirmation;
    }

    public void setDangerousMacroConfirmation(boolean enabled) {
        this.dangerousMacroConfirmation = enabled;
    }

    public boolean isSavePopupSize() {
        return savePopupSize;
    }

    public void setSavePopupSize(boolean enabled) {
        this.savePopupSize = enabled;
    }

    /** Width of the in-chat actions popup, in GUI pixels. */
    public int getPopupWidth() {

        return Math.clamp(
                popupWidth,
                MIN_POPUP_WIDTH,
                MAX_POPUP_WIDTH
        );
    }

    public void setPopupWidth(int width) {
        this.popupWidth = Math.clamp(
                width,
                MIN_POPUP_WIDTH,
                MAX_POPUP_WIDTH
        );
    }

    /** Height of the in-chat actions popup, in GUI pixels. */
    public int getPopupHeight() {

        return Math.clamp(
                popupHeight,
                MIN_POPUP_HEIGHT,
                MAX_POPUP_HEIGHT
        );
    }

    public void setPopupHeight(int height) {
        this.popupHeight = Math.clamp(
                height,
                MIN_POPUP_HEIGHT,
                MAX_POPUP_HEIGHT
        );
    }

    public boolean isToastsEnabled() {
        return toastsEnabled;
    }

    public void setToastsEnabled(boolean enabled) {
        this.toastsEnabled = enabled;
    }

    public int getThemeIndex() {

        return themeIndex < 0 ? 0 : themeIndex;
    }

    public void setThemeIndex(int index) {
        this.themeIndex = Math.max(0, index);
    }

    public boolean isChatInputDetectEnabled() {
        return chatInputDetectEnabled;
    }

    public void setChatInputDetectEnabled(boolean enabled) {
        this.chatInputDetectEnabled = enabled;
    }

    public Lang.Language getLanguage() {
        return language != null ? language : Lang.Language.RU;
    }

    public void setLanguage(Lang.Language language) {
        this.language = language != null ? language : Lang.Language.RU;
    }

    public List<Macro> getMacros() {
        return macros != null ? macros : new ArrayList<>();
    }

    public void setMacros(List<Macro> macros) {
        this.macros = macros != null ? macros : new ArrayList<>();
    }

    public List<ActionEntry> getActionEntries() {
        return actionEntries != null ? actionEntries : ActionEntry.defaults();
    }

    public void setActionEntries(List<ActionEntry> actionEntries) {
        this.actionEntries = actionEntries != null ? actionEntries : new ArrayList<>();
    }
}
