package com.uwdie.stafftools.client.i18n;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Lang {

    public enum Language {
        @SerializedName("ru")
        RU("Русский"),
        @SerializedName("en")
        EN("English");

        private final String displayName;

        Language(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final class Key {

        private Key() {
        }

        // General
        public static final String APP_TITLE = "app.title";
        public static final String APP_TAGLINE = "app.tagline";

        // Buttons
        public static final String BTN_MACROS = "btn.macros";
        public static final String BTN_CREATE_MACRO = "btn.createMacro";
        public static final String BTN_PLAYER_ACTIONS = "btn.playerActions";
        public static final String BTN_HISTORY = "btn.history";
        public static final String BTN_CLOSE = "btn.close";
        public static final String BTN_BACK = "btn.back";
        public static final String BTN_SAVE = "btn.save";
        public static final String BTN_CANCEL = "btn.cancel";
        public static final String BTN_EDIT = "btn.edit";
        public static final String BTN_EDIT_MACRO = "btn.editMacro";
        public static final String BTN_EDIT_ACTION = "btn.editAction";
        public static final String BTN_DELETE = "btn.delete";
        public static final String BTN_ADD = "btn.add";
        public static final String BTN_ADD_ACTION = "btn.addAction";
        public static final String BTN_CLEAR = "btn.clear";
        public static final String BTN_ON = "btn.on";
        public static final String BTN_OFF = "btn.off";
        public static final String BTN_THEMES = "btn.themes";
        public static final String BTN_DETECTION = "btn.detection";
        public static final String BTN_PROFILES = "btn.profiles";
        public static final String BTN_SETTINGS = "btn.settings";
        public static final String BTN_GUIDEBOOK = "btn.guidebook";
        public static final String BTN_ANALYSIS = "btn.analysis";
        public static final String TITLE_ANALYSIS = "title.analysis";
        public static final String HINT_RAIL_ANALYSIS = "hint.rail.analysis";
        public static final String THEME_NICK_COLOR =
                "theme.nickColor";
        public static final String THEME_NICK_RESET =
                "theme.nickReset";
        public static final String THEME_NICK_AUTO =
                "theme.nickAuto";

        /** Label of the interface accent colour picker. */
        public static final String LABEL_ACCENT_COLOR =
                "label.accentColor";

        // Titles / hints
        public static final String TITLE_DETECTION = "title.detection";
        public static final String TITLE_PROFILES = "title.profiles";
        public static final String TITLE_PROFILE_EDIT = "title.profileEdit";
        public static final String TITLE_PROFILE_IMPORT = "title.profileImport";
        public static final String MSG_DETECT_HINT = "msg.detectHint";
        public static final String MSG_DETECT_EMPTY = "msg.detectEmpty";
        public static final String MSG_PROFILES_HINT = "msg.profilesHint";
        public static final String MSG_PROFILES_EMPTY = "msg.profilesEmpty";
        public static final String MSG_PROFILE_MACROS = "msg.profileMacros";
        public static final String MSG_PROFILE_ACTIONS = "msg.profileActions";
        public static final String MSG_PROFILE_NONE = "msg.profileNone";
        public static final String MSG_PROFILE_ACTIVE = "msg.profileActive";

        // Themes
        public static final String THEME_OCEAN = "theme.ocean";
        public static final String THEME_EMERALD = "theme.emerald";
        public static final String THEME_AMETHYST = "theme.amethyst";
        public static final String THEME_CRIMSON = "theme.crimson";
        public static final String THEME_DAWN = "theme.dawn";
        public static final String THEME_AURORA = "theme.aurora";
        public static final String THEME_NEON = "theme.neon";
        public static final String THEME_SUNSET = "theme.sunset";
        public static final String THEME_TWILIGHT = "theme.twilight";
        public static final String THEME_SAKURA = "theme.sakura";
        public static final String THEME_GLACIER = "theme.glacier";
        public static final String THEME_MINT = "theme.mint";
        public static final String THEME_PLUM = "theme.plum";

        // Toggles
        public static final String TOGGLE_CHAT_MENTIONS = "toggle.chatMentions";
        public static final String TOGGLE_COPY_NAME = "toggle.copyName";
        public static final String TOGGLE_ACTIONS_POPUP = "toggle.actionsPopup";
        public static final String TOGGLE_CONFIRM_DANGER = "toggle.confirmDanger";
        public static final String TOGGLE_ENABLED = "toggle.enabled";
        public static final String TOGGLE_DANGEROUS = "toggle.dangerous";
        public static final String TOGGLE_CONFIRMATION = "toggle.confirmation";
        public static final String TOGGLE_LANGUAGE = "toggle.language";
        public static final String TOGGLE_TOASTS = "toggle.toasts";
        public static final String TOGGLE_CHAT_INPUT = "toggle.chatInput";
        public static final String TOGGLE_SAVE_POPUP_SIZE =
                "toggle.savePopupSize";
        public static final String TOGGLE_SAVE_CHAT_SIZE =
                "toggle.saveChatSize";
        public static final String TOGGLE_CHAT_FRAME =
                "toggle.chatFrame";
        public static final String TOGGLE_MOD_HUD =
                "toggle.modHud";
        public static final String TOGGLE_MOD_HUD_PHRASES =
                "toggle.modHudPhrases";
        public static final String TOGGLE_MENU_ANIMATION =
                "toggle.menuAnimation";
        public static final String TITLE_ANIMATION_LABEL =
                "title.animation.label";
        public static final String TITLE_ANIMATION_STATIC =
                "title.animation.static";
        public static final String TITLE_ANIMATION_AURORA =
                "title.animation.aurora";
        public static final String TITLE_ANIMATION_PULSE =
                "title.animation.pulse";
        public static final String TITLE_ANIMATION_SWEEP =
                "title.animation.sweep";
        public static final String ANIMATION_SPEED =
                "settings.animationSpeed";
        public static final String GLOW_INTENSITY =
                "settings.glowIntensity";
        public static final String TOGGLE_CHAT_RESIZE =
                "toggle.chatResize";

        // Detection methods (all shown in the detection settings)
        public static final String TOGGLE_DETECT_PLAIN =
                "toggle.detectPlain";
        public static final String TOGGLE_DETECT_DISPLAY =
                "toggle.detectDisplay";
        public static final String TOGGLE_DETECT_CLICK =
                "toggle.detectClick";
        public static final String TOGGLE_DETECT_HOVER =
                "toggle.detectHover";
        public static final String TOGGLE_DETECT_ALIASES =
                "toggle.detectAliases";
        public static final String TOGGLE_NICK_HIGHLIGHT_DISABLED =
                "toggle.nickHighlightDisabled";
        public static final String TOGGLE_DETECT_UNICODE =
                "toggle.detectUnicode";
        public static final String TOGGLE_DETECT_DECOR =
                "toggle.detectDecor";
        public static final String TOGGLE_DETECT_LINE =
                "toggle.detectLine";
        public static final String TOGGLE_DETECT_CACHE =
                "toggle.detectCache";
        public static final String TOGGLE_DETECT_FUZZY =
                "toggle.detectFuzzy";

        // Field labels
        public static final String LABEL_NAME = "label.name";
        public static final String LABEL_DESCRIPTION = "label.description";
        public static final String LABEL_COMMAND = "label.command";
        public static final String LABEL_COMMAND_HINT = "label.commandHint";
        public static final String LABEL_PLACEHOLDERS = "label.placeholders";
        public static final String LABEL_ALIASES = "label.aliases";
        public static final String LABEL_ICON = "label.icon";
        public static final String LABEL_ACTION = "label.action";
        public static final String LABEL_SETTINGS = "label.settings";

        // Placeholder hints
        public static final String PH_PLAYER = "ph.player";
        public static final String PH_STAFF = "ph.staff";
        public static final String PH_X = "ph.x";
        public static final String PH_Y = "ph.y";
        public static final String PH_Z = "ph.z";
        public static final String PH_PING = "ph.ping";
        public static final String PH_SERVER = "ph.server";

        // Messages
        public static final String MSG_SELECT_MACRO = "msg.selectMacro";
        public static final String MSG_NO_MACROS = "msg.noMacros";
        public static final String MSG_NO_DESCRIPTION = "msg.noDescription";
        public static final String MSG_UNNAMED = "msg.unnamed";
        public static final String MSG_ACTIONS_HINT = "msg.actionsHint";
        public static final String MSG_EMOJI_HINT = "msg.emojiHint";
        public static final String MSG_ALIAS_HELP = "msg.aliasHelp";
        public static final String MSG_NO_ACTIONS = "msg.noActions";
        public static final String MSG_NO_HISTORY = "msg.noHistory";
        public static final String MSG_HISTORY_HINT = "msg.historyHint";

        // Overlay
        public static final String OVERLAY_NO_ACTIONS = "overlay.noActions";
        public static final String OVERLAY_CUSTOM_MACROS = "overlay.customMacros";
        public static final String OVERLAY_CONFIRM = "overlay.confirm";
        public static final String OVERLAY_HINT_CLOSE = "overlay.hintClose";
        public static final String OVERLAY_HINT_SCROLL = "overlay.hintScroll";
        public static final String OVERLAY_HINT_DRAG = "overlay.hintDrag";
        public static final String OVERLAY_NP = "overlay.np";
        public static final String OVERLAY_COPY_NAME = "overlay.copyName";
        public static final String OVERLAY_DUP_CHAT = "overlay.dupChat";
        public static final String OVERLAY_ALIAS_TITLE = "overlay.aliasTitle";

        // Reason input modal
        public static final String REASON_TITLE = "reason.title";
        public static final String REASON_PLACEHOLDER = "reason.placeholder";
        public static final String REASON_CONFIRM = "reason.confirm";
        public static final String MSG_REASON_HELP = "msg.reasonHelp";

        // Settings screen
        public static final String TITLE_SETTINGS = "title.settings";
        public static final String TITLE_SERVER_RENAME =
                "title.serverRename";
        public static final String LABEL_SERVER_NAME = "label.serverName";
        public static final String LABEL_SERVER_KEY = "label.serverKey";

        // History sidebar + filters
        public static final String HIST_SERVERS = "hist.servers";
        public static final String HIST_ALL_SERVERS = "hist.allServers";
        public static final String HIST_COL_SERVER = "hist.colServer";
        public static final String HIST_FILTER_TYPE = "hist.filterType";
        public static final String HIST_FILTER_STATUS = "hist.filterStatus";
        public static final String HIST_FILTER_ALL = "hist.filterAll";
        public static final String HIST_FILTER_RESET = "hist.filterReset";
        public static final String HIST_FILTER_NICK_HINT =
                "hist.filterNick";
        public static final String BTN_HIST_CLEAR_ONE =
                "btn.histClearOne";

        // Moderator HUD
        public static final String HUD_TITLE = "hud.title";
        public static final String HUD_MODERATOR = "hud.moderator";
        public static final String HUD_SERVER = "hud.server";
        public static final String HUD_SESSION = "hud.session";
        public static final String HUD_PUNISHED = "hud.punished";
        public static final String HUD_OFFLINE = "hud.offline";

        // profile actions
        public static final String BTN_PROFILE_CREATE = "btn.profileCreate";
        public static final String BTN_PROFILE_SAVE = "btn.profileSave";
        public static final String BTN_PROFILE_APPLY = "btn.profileApply";
        public static final String BTN_PROFILE_EXPORT = "btn.profileExport";
        public static final String BTN_PROFILE_IMPORT = "btn.profileImport";
        public static final String BTN_PROFILE_DELETE = "btn.profileDelete";
        public static final String BTN_PROFILE_OPEN = "btn.profileOpen";
        public static final String BTN_PROFILE_OPEN_FOLDER =
                "btn.profileOpenFolder";
        public static final String BTN_REFRESH = "btn.refresh";
        public static final String LABEL_PROFILE_NAME = "label.profileName";
        public static final String LABEL_PROFILE_THEME = "label.profileTheme";

        public static final String MSG_PROFILE_NO_FILES =
                "msg.profileNoFiles";
        public static final String MSG_PROFILE_IMPORT_HINT =
                "msg.profileImportHint";
        public static final String MSG_PROFILE_FOLDER =
                "msg.profileFolder";

        // Toasts
        public static final String TOAST_COPIED = "toast.copied";
        public static final String TOAST_DUP_CHAT = "toast.dupChat";
        public static final String TOAST_EXECUTED = "toast.executed";
        public static final String TOAST_PROFILE_SAVED = "toast.profileSaved";
        public static final String TOAST_PROFILE_APPLIED = "toast.profileApplied";
        public static final String TOAST_PROFILE_CREATED = "toast.profileCreated";
        public static final String TOAST_PROFILE_DELETED = "toast.profileDeleted";
        public static final String TOAST_PROFILE_EXPORTED = "toast.profileExported";
        public static final String TOAST_PROFILE_IMPORTED = "toast.profileImported";
        public static final String TOAST_PROFILE_BAD_JSON =
                "toast.profileBadJson";
        public static final String TOAST_PROFILE_LAST =
                "toast.profileLast";
        public static final String TOAST_PROFILE_EXPORT_FAILED =
                "toast.profileExportFailed";
        public static final String TOAST_PROFILE_FOLDER_OPENED =
                "toast.profileFolderOpened";
        public static final String TOAST_THEME_CHANGED =
                "toast.themeChanged";

        // Punishments
        public static final String HISTORY_TITLE = "history.title";
        public static final String HIST_COL_TIME = "hist.colTime";
        public static final String HIST_COL_ACTION = "hist.colAction";
        public static final String HIST_COL_PLAYER = "hist.colPlayer";
        public static final String PUN_TYPE_BAN = "pun.type.ban";
        public static final String PUN_TYPE_MUTE = "pun.type.mute";
        public static final String PUN_TYPE_KICK = "pun.type.kick";
        public static final String PUN_TYPE_WARN = "pun.type.warn";
        public static final String PUN_TYPE_FREEZE = "pun.type.freeze";
        public static final String PUN_TYPE_OTHER = "pun.type.other";
        public static final String PUN_TYPE_UNMUTE = "pun.type.unmute";
        public static final String PUN_TYPE_UNBAN = "pun.type.unban";
        public static final String PUN_STATUS_PENDING = "pun.status.pending";
        public static final String PUN_STATUS_DONE = "pun.status.done";
        public static final String PUN_STATUS_NO_RESPONSE = "pun.status.noResponse";
        public static final String PUN_STATUS_FAILED = "pun.status.failed";
        public static final String PUN_PLAYER = "pun.player";
        public static final String PUN_NO_RESPONSE_TEXT = "pun.noResponseText";

        // History summary
        public static final String STAT_TOTAL = "stat.total";
        public static final String STAT_CONFIRMED = "stat.confirmed";
        public static final String STAT_FAILED = "stat.failed";
        public static final String STAT_NO_RESPONSE = "stat.noResponse";
        public static final String STAT_AWAITING = "stat.awaiting";
        public static final String STAT_PLAYERS = "stat.players";
        public static final String STAT_TOP_PLAYER = "stat.topPlayer";
        public static final String STAT_MODERATORS = "stat.moderators";
        public static final String STAT_FROM_MACROS = "stat.fromMacros";
        public static final String STAT_BY_TYPE = "stat.byType";
        public static final String STAT_FROM_ACTIONS = "stat.fromActions";
        public static final String HIST_COL_STATUS = "hist.colStatus";
        public static final String HIST_HINT_CLICK = "hist.hintClick";

        // Punishment details
        public static final String DETAIL_TITLE = "detail.title";
        public static final String DETAIL_PLAYER = "detail.player";
        public static final String DETAIL_TIME = "detail.time";
        public static final String DETAIL_MODERATOR = "detail.moderator";
        public static final String DETAIL_TYPE = "detail.type";
        public static final String DETAIL_MACRO = "detail.macro";
        public static final String DETAIL_ACTION = "detail.action";
        public static final String DETAIL_COMMAND = "detail.command";
        public static final String DETAIL_STATUS = "detail.status";
        public static final String DETAIL_SERVER_REPLY = "detail.serverReply";
        public static final String DETAIL_UNKNOWN = "detail.unknown";
        public static final String DETAIL_NONE = "detail.none";

        // Manual punishment resolution
        public static final String BTN_MARK_DONE = "btn.markDone";
        public static final String BTN_MARK_FAILED = "btn.markFailed";
        public static final String BTN_MARK_PENDING = "btn.markPending";
        public static final String TOAST_MARKED_DONE = "toast.markedDone";
        public static final String TOAST_MARKED_FAILED = "toast.markedFailed";
        public static final String TOAST_MARKED_PENDING = "toast.markedPending";

        // Audit log + profiler
        public static final String BTN_AUDIT = "btn.audit";
        public static final String TITLE_AUDIT = "title.audit";
        public static final String MSG_AUDIT_HINT = "msg.auditHint";
        public static final String MSG_AUDIT_EMPTY = "msg.auditEmpty";
        public static final String AUDIT_TAB_LOG = "audit.tabLog";
        public static final String AUDIT_TAB_PROFILE = "audit.tabProfile";
        public static final String AUDIT_FILTER_ALL = "audit.filterAll";
        public static final String AUDIT_CLEAR = "audit.clear";
        public static final String AUDIT_CLEARED = "audit.cleared";
        public static final String AUDIT_CAT_MACRO = "audit.cat.macro";
        public static final String AUDIT_CAT_DETECT = "audit.cat.detect";
        public static final String AUDIT_CAT_PUNISH = "audit.cat.punish";
        public static final String AUDIT_CAT_CONFIG = "audit.cat.config";
        public static final String AUDIT_CAT_CHAT = "audit.cat.chat";
        public static final String AUDIT_CAT_ERROR = "audit.cat.error";
        public static final String AUDIT_CAT_DB = "audit.cat.db";
        public static final String PROF_SECTION = "prof.section";
        public static final String PROF_CALLS = "prof.calls";
        public static final String PROF_AVG = "prof.avg";
        public static final String PROF_MAX = "prof.max";
        public static final String PROF_TOTAL = "prof.total";
        public static final String PROF_TRACE = "prof.trace";
        public static final String PROF_RESET = "prof.reset";
        public static final String PROF_EMPTY = "prof.empty";
        public static final String TOGGLE_AUDIT = "toggle.audit";
        public static final String TOGGLE_PROFILER = "toggle.profiler";

        // Command preview
        public static final String PREVIEW_TITLE = "preview.title";
        public static final String PREVIEW_EXAMPLE = "preview.example";
        public static final String PREVIEW_EMPTY = "preview.empty";

        // Nick hover statistics
        public static final String HOVER_STATS_ONLINE = "hover.stats.online";
        public static final String HOVER_STATS_OFFLINE = "hover.stats.offline";
        public static final String HOVER_STATS_PING = "hover.stats.ping";
        public static final String HOVER_STATS_GAMEMODE = "hover.stats.gamemode";
        public static final String HOVER_STATS_PUNISH = "hover.stats.punish";
        public static final String HOVER_STATS_SERVER = "hover.stats.server";
        public static final String HOVER_STATS_OPEN = "hover.stats.open";

        // Drag and drop
        public static final String MSG_DRAG_HINT = "msg.dragHint";

        // Main menu dashboard
        public static final String MENU_SECTION_TOOLS = "menu.sectionTools";
        public static final String MENU_SECTION_SYSTEM = "menu.sectionSystem";
        public static final String MENU_STAT_MACROS = "menu.statMacros";
        public static final String MENU_STAT_ACTIONS = "menu.statActions";
        public static final String MENU_STAT_PUNISH = "menu.statPunish";
        public static final String MENU_STAT_SESSION = "menu.statSession";
        public static final String MENU_PROFILE = "menu.profile";

        // Launcher-style hints
        public static final String MSG_LANGUAGE_HINT = "msg.languageHint";
        public static final String MSG_ACCENT_HINT = "msg.accentHint";
        public static final String MSG_INTERFACE_HINT = "msg.interfaceHint";
        public static final String MSG_SYSTEM_HINT = "msg.systemHint";
        public static final String MSG_EDITOR_HINT = "msg.editorHint";

        // Rail / tab tooltips
        public static final String HINT_RAIL_MACROS = "hint.rail.macros";
        public static final String HINT_RAIL_ACTIONS = "hint.rail.actions";
        public static final String HINT_RAIL_HISTORY = "hint.rail.history";
        public static final String HINT_RAIL_PROFILES = "hint.rail.profiles";
        public static final String HINT_RAIL_AUDIT = "hint.rail.audit";
        public static final String HINT_RAIL_SETTINGS = "hint.rail.settings";
        public static final String HINT_RAIL_GUIDEBOOK = "hint.rail.guidebook";
        public static final String TITLE_GUIDEBOOK = "title.guidebook";
        public static final String HINT_TAB_GENERAL = "hint.tab.general";
        public static final String HINT_TAB_INTERFACE = "hint.tab.interface";
        public static final String HINT_TAB_CHAT = "hint.tab.chat";
        public static final String HINT_TAB_SYSTEM = "hint.tab.system";
    }

        private static Language current = Language.RU;

    /** Loaded string tables, keyed by language. */
    private static final Map<Language, Map<String, String>> TEXTS =
            new EnumMap<>(Language.class);

    private static boolean loaded;

    private Lang() {
    }

    /**
     * Loads the language files from
     * {@code assets/stafftools/lang/<code>.json}. Missing or broken
     * files fall back to an empty table, so {@link #t(String)} still
     * returns the key instead of crashing.
     */
    private static synchronized void ensureLoaded() {

        if (loaded) {
            return;
        }

        loaded = true;

        for (Language language : Language.values()) {
            TEXTS.put(language, load(language));
        }
    }

    private static Map<String, String> load(Language language) {

        String code = language == Language.EN ? "en" : "ru";

        Identifier id = Identifier.of(
                "stafftools",
                "lang/" + code + ".json"
        );

        Map<String, String> table = new HashMap<>();

        try {
            var resource = MinecraftClient.getInstance()
                    .getResourceManager()
                    .getResource(id);

            if (resource.isEmpty()) {
                return table;
            }

            String json;
            try (var reader = resource.get().getReader()) {
                json = reader.lines()
                        .collect(java.util.stream.Collectors.joining("\n"));
            }

            Type type = new TypeToken<Map<String, String>>() {
            }.getType();

            Map<String, String> parsed =
                    new Gson().fromJson(json, type);

            if (parsed != null) {
                table.putAll(parsed);
            }

        } catch (Exception e) {
            System.err.println(
                    "[StaffTools] Failed to load lang " + code + "."
            );
        }

        return table;
    }

    public static void setLanguage(Language language) {
        if (language != null) {
            current = language;
        }
    }

    public static Language getLanguage() {
        return current;
    }

    public static String t(String key) {

        ensureLoaded();

        Map<String, String> table = TEXTS.get(current);
        String value = table == null ? null : table.get(key);

        if (value != null) {
            return value;
        }

        if (current != Language.EN) {
            Map<String, String> fallback = TEXTS.get(Language.EN);
            String fb = fallback == null ? null : fallback.get(key);
            if (fb != null) {
                return fb;
            }
        }

        return key;
    }

    public static String t(String key, Object... args) {
        return String.format(t(key), args);
    }

    public static Text text(String key) {
        return Text.literal(t(key));
    }

    public static Text text(String key, Object... args) {
        return Text.literal(t(key, args));
    }
}
