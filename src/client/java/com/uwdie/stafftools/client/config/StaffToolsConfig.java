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

    /** How far the chat resize grip may push past the vanilla maximum. */
    public static final double MAX_CHAT_BOOST = 6.0;

    private boolean chatMentionsEnabled = true;
    private boolean clickToCopyEnabled = true;
    private boolean playerActionsEnabled = true;

    private boolean dangerousMacroConfirmation = true;

    private boolean toastsEnabled = true;

    private boolean savePopupSize = true;

    /** Keeps the resized chat size across game starts. */
    private boolean saveChatSize = true;

    /** Decorative frame around the vanilla chat. */
    private boolean chatFrameEnabled = true;
    private int popupWidth = DEFAULT_POPUP_WIDTH;
    private int popupHeight = DEFAULT_POPUP_HEIGHT;

    /** Animated frame around the panels (menus + popups). */
    private boolean menuAnimation = true;

    /** Grip that resizes the vanilla chat while it is open. */
    private boolean chatResizeEnabled = true;

    /**
     * Extra multipliers on top of the vanilla 320×180 maximum, used by
     * the chat resize grip to make the chat larger than vanilla allows.
     */
    private double chatWidthBoost = 1.0;
    private double chatHeightBoost = 1.0;

    // nick detection methods (see DetectionSettingsScreen)
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

    private List<Profile> profiles = new ArrayList<>();
    private String activeProfileId = "";

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

    /** Persists the chat size that the resize grip produced. */
    public boolean isSaveChatSize() {
        return saveChatSize;
    }

    public void setSaveChatSize(boolean enabled) {
        this.saveChatSize = enabled;
    }

    /** Animated frame drawn around the vanilla chat. */
    public boolean isChatFrameEnabled() {
        return chatFrameEnabled;
    }

    public void setChatFrameEnabled(boolean enabled) {
        this.chatFrameEnabled = enabled;
    }

    public boolean isMenuAnimation() {
        return menuAnimation;
    }

    public void setMenuAnimation(boolean enabled) {
        this.menuAnimation = enabled;
    }

    /** Corner grip that resizes the vanilla chat (width / height / scale). */
    public boolean isChatResizeEnabled() {
        return chatResizeEnabled;
    }

    public void setChatResizeEnabled(boolean enabled) {
        this.chatResizeEnabled = enabled;
    }

    /** Multiplier past the vanilla width maximum, 1 = untouched. */
    public double getChatWidthBoost() {
        return clampBoost(chatWidthBoost);
    }

    public void setChatWidthBoost(double boost) {
        this.chatWidthBoost = clampBoost(boost);
    }

    /** Multiplier past the vanilla height maximum, 1 = untouched. */
    public double getChatHeightBoost() {
        return clampBoost(chatHeightBoost);
    }

    public void setChatHeightBoost(double boost) {
        this.chatHeightBoost = clampBoost(boost);
    }

    private static double clampBoost(double boost) {

        if (Double.isNaN(boost)) {
            return 1.0;
        }

        return Math.clamp(boost, 1.0, MAX_CHAT_BOOST);
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

    public boolean isDetectPlain() {
        return detectPlain;
    }

    public void setDetectPlain(boolean value) {
        this.detectPlain = value;
    }

    public boolean isDetectDisplay() {
        return detectDisplay;
    }

    public void setDetectDisplay(boolean value) {
        this.detectDisplay = value;
    }

    public boolean isDetectClick() {
        return detectClick;
    }

    public void setDetectClick(boolean value) {
        this.detectClick = value;
    }

    public boolean isDetectHover() {
        return detectHover;
    }

    public void setDetectHover(boolean value) {
        this.detectHover = value;
    }

    public boolean isDetectAliases() {
        return detectAliases;
    }

    public void setDetectAliases(boolean value) {
        this.detectAliases = value;
    }

    public boolean isDetectUnicode() {
        return detectUnicode;
    }

    public void setDetectUnicode(boolean value) {
        this.detectUnicode = value;
    }

    public boolean isDetectDecor() {
        return detectDecor;
    }

    public void setDetectDecor(boolean value) {
        this.detectDecor = value;
    }

    public boolean isDetectLine() {
        return detectLine;
    }

    public void setDetectLine(boolean value) {
        this.detectLine = value;
    }

    public boolean isDetectCache() {
        return detectCache;
    }

    public void setDetectCache(boolean value) {
        this.detectCache = value;
    }

    public boolean isDetectFuzzy() {
        return detectFuzzy;
    }

    public void setDetectFuzzy(boolean value) {
        this.detectFuzzy = value;
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

    // ---------------------------------------------------------------
    // profiles
    // ---------------------------------------------------------------

    /** All saved profiles (never null, may be empty until first use). */
    public List<Profile> getProfiles() {
        return profiles != null ? profiles : new ArrayList<>();
    }

    public void setProfiles(List<Profile> value) {
        this.profiles = value != null ? value : new ArrayList<>();
    }

    /**
     * The profile the live settings currently mirror. Created lazily
     * from the current settings, so an old config file keeps working.
     */
    public Profile getActiveProfile() {

        for (Profile profile : getProfiles()) {

            if (profile.getId().equals(activeProfileId)) {
                return profile;
            }
        }

        if (getProfiles().isEmpty()) {
            return createProfile(defaultProfileName());
        }

        // stale id: fall back to the first profile
        Profile first = getProfiles().get(0);

        this.activeProfileId = first.getId();

        return first;
    }

    private String defaultProfileName() {
        return "Profile " + (getProfiles().size() + 1);
    }

    /**
     * Snapshots the current live settings + macros + actions into a new
     * profile and activates it.
     */
    public Profile createProfile(String name) {

        Profile profile = new Profile();

        profile.setName(
                name == null || name.isBlank()
                        ? defaultProfileName()
                        : name
        );

        profile.capture(this);

        getProfiles().add(profile);

        this.activeProfileId = profile.getId();

        return profile;
    }

    /** Writes the live settings back into the active profile. */
    public void saveActiveProfile() {
        getActiveProfile().capture(this);
    }

    /**
     * Activates another profile: the live settings are overwritten by
     * its snapshot. Returns false when the id is unknown.
     */
    public boolean switchProfile(String id) {

        Profile target = null;

        for (Profile profile : getProfiles()) {

            if (profile.getId().equals(id)) {
                target = profile;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        target.restore(this);

        this.activeProfileId = target.getId();

        return true;
    }

    /** Removes a profile; the last one can never be deleted. */
    public boolean deleteProfile(String id) {

        if (getProfiles().size() <= 1) {
            return false;
        }

        boolean removed =
                getProfiles().removeIf(
                        profile -> profile.getId().equals(id)
                );

        if (removed &&
                id.equals(activeProfileId)) {

            Profile next = getProfiles().get(0);

            next.restore(this);

            activeProfileId = next.getId();
        }

        return removed;
    }

    public boolean renameProfile(String id, String name) {

        for (Profile profile : getProfiles()) {

            if (profile.getId().equals(id)) {

                profile.setName(name);
                return true;
            }
        }

        return false;
    }

    public Profile findProfile(String id) {

        for (Profile profile : getProfiles()) {

            if (profile.getId().equals(id)) {
                return profile;
            }
        }

        return null;
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public void setActiveProfileId(String id) {
        this.activeProfileId = id != null ? id : "";
    }
}
