package com.uwdie.stafftools.client.config;

/**
 * The portable part of a profile: every toggle / flag the user may
 * want to switch together with their macros.
 *
 * This class only holds data. Copying it in and out of the live
 * {@link StaffToolsConfig} is done explicitly (see readFrom/writeTo),
 * so a renamed field can never silently lose a setting.
 */
public class ProfileSettings {

    private boolean chatMentionsEnabled = true;
    private boolean clickToCopyEnabled = true;
    private boolean playerActionsEnabled = true;

    private boolean dangerousMacroConfirmation = true;

    private boolean toastsEnabled = true;

    private boolean chatInputDetectEnabled = true;

    private boolean savePopupSize = true;
    private int popupWidth = StaffToolsConfig.DEFAULT_POPUP_WIDTH;
    private int popupHeight = StaffToolsConfig.DEFAULT_POPUP_HEIGHT;

    // nick detection methods
    private boolean detectPlain = true;
    private boolean detectDisplay = true;
    private boolean detectClick = true;
    private boolean detectHover = true;
    private boolean detectAliases = true;
    private boolean detectUnicode = true;
    private boolean detectDecor = true;
    private boolean detectLine = true;
    private boolean detectCache = true;
    private boolean detectFuzzy = false;

    /** Snapshot of the current live settings. */
    public void readFrom(StaffToolsConfig config) {

        this.chatMentionsEnabled =
                config.isChatMentionsEnabled();
        this.clickToCopyEnabled =
                config.isClickToCopyEnabled();
        this.playerActionsEnabled =
                config.isPlayerActionsEnabled();
        this.dangerousMacroConfirmation =
                config.isDangerousMacroConfirmation();
        this.toastsEnabled =
                config.isToastsEnabled();
        this.chatInputDetectEnabled =
                config.isChatInputDetectEnabled();
        this.savePopupSize =
                config.isSavePopupSize();
        this.popupWidth =
                config.getPopupWidth();
        this.popupHeight =
                config.getPopupHeight();
        this.detectPlain =
                config.isDetectPlain();
        this.detectDisplay =
                config.isDetectDisplay();
        this.detectClick =
                config.isDetectClick();
        this.detectHover =
                config.isDetectHover();
        this.detectAliases =
                config.isDetectAliases();
        this.detectUnicode =
                config.isDetectUnicode();
        this.detectDecor =
                config.isDetectDecor();
        this.detectLine =
                config.isDetectLine();
        this.detectCache =
                config.isDetectCache();
        this.detectFuzzy =
                config.isDetectFuzzy();
    }

    /** Writes this snapshot into the live settings. */
    public void writeTo(StaffToolsConfig config) {

        config.setChatMentionsEnabled(chatMentionsEnabled);
        config.setClickToCopyEnabled(clickToCopyEnabled);
        config.setPlayerActionsEnabled(playerActionsEnabled);
        config.setDangerousMacroConfirmation(dangerousMacroConfirmation);
        config.setToastsEnabled(toastsEnabled);
        config.setChatInputDetectEnabled(chatInputDetectEnabled);
        config.setSavePopupSize(savePopupSize);
        config.setPopupWidth(popupWidth);
        config.setPopupHeight(popupHeight);
        config.setDetectPlain(detectPlain);
        config.setDetectDisplay(detectDisplay);
        config.setDetectClick(detectClick);
        config.setDetectHover(detectHover);
        config.setDetectAliases(detectAliases);
        config.setDetectUnicode(detectUnicode);
        config.setDetectDecor(detectDecor);
        config.setDetectLine(detectLine);
        config.setDetectCache(detectCache);
        config.setDetectFuzzy(detectFuzzy);
    }
}
