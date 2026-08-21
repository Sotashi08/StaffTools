package com.uwdie.stafftools.client.config;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.macro.Macro;

import java.util.ArrayList;
import java.util.List;

public class StaffToolsConfig {
    private boolean chatMentionsEnabled = true;
    private boolean clickToCopyEnabled = true;
    private boolean playerActionsEnabled = true;

    private boolean dangerousMacroConfirmation = true;

    private boolean toastsEnabled = true;

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

    public boolean isToastsEnabled() {
        return toastsEnabled;
    }

    public void setToastsEnabled(boolean enabled) {
        this.toastsEnabled = enabled;
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
