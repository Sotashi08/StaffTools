package com.uwdie.stafftools.client.i18n;

import com.google.gson.annotations.SerializedName;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight localization for the mod.
 * Two supported languages: Russian and English.
 */
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

        // Chat hover
        public static final String HOVER_CLICK = "hover.click";
        public static final String HOVER_SHIFT_CLICK = "hover.shiftClick";

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
    }

    private static Language current = Language.RU;

    /** Rotating tagline of the main menu (one is picked per open). */
    private static final String[] TAGLINES_RU = {
            "Ты снова здесь?",
            "Чудесный день, не правда ли?",
            "Сколько раз ты здесь был?",
            "Ну привет",
            "Как жизнь?",
            "Опять работать",
            "Ты был прав, это баг",
            "Зуб даю, ты вернёшься",
            "Здесь снова пахнет приключениями",
            "Не бойся, я просто меню",
            "С этого места всё началось",
            "А помнишь первый раз?",
            "Ты зашёл, а мир изменился",
            "Время — иллюзия, меню — нет",
            "Ого, кто это к нам пришёл",
            "Кто-то явно скучал",
            "Случайно или нарочно?",
            "Привет, странник",
            "Снова мы и снова ты",
            "Давай без внезапных решений"
    };

    private static final String[] TAGLINES_EN = {
            "You here again?",
            "Lovely day, isn't it?",
            "How many times have you been here?",
            "Well, hello",
            "How's life?",
            "Back to work again",
            "You were right, it is a bug",
            "Bet you will be back",
            "Smells like adventure in here",
            "Don't worry, I am just a menu",
            "This is where it all began",
            "Remember the first time?",
            "You came, the world changed",
            "Time is an illusion, menus are not",
            "Look who is here",
            "Somebody clearly missed this place",
            "On purpose or by accident?",
            "Hello, wanderer",
            "Me again, you again",
            "Let us keep sudden decisions low"
    };

    /** Random slogan for the menu header. */
    public static String tagline() {

        String[] pool = current == Language.RU
                ? TAGLINES_RU
                : TAGLINES_EN;

        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }

    private static final Map<Language, Map<String, String>> TEXTS =
            new HashMap<>();

    private Lang() {
    }

    static {
        Map<String, String> ru = new HashMap<>();
        ru.put(Key.APP_TITLE, "StaffTools");
        ru.put(Key.APP_TAGLINE, "Утилиты и макросы для модерации");

        ru.put(Key.BTN_MACROS, "Макросы");
        ru.put(Key.BTN_CREATE_MACRO, "Создать макрос");
        ru.put(Key.BTN_PLAYER_ACTIONS, "Действия игрока");
        ru.put(Key.BTN_HISTORY, "История");
        ru.put(Key.BTN_CLOSE, "Закрыть");
        ru.put(Key.BTN_BACK, "Назад");
        ru.put(Key.BTN_SAVE, "Сохранить");
        ru.put(Key.BTN_CANCEL, "Отмена");
        ru.put(Key.BTN_EDIT, "Изменить");
        ru.put(Key.BTN_EDIT_MACRO, "Изменить макрос");
        ru.put(Key.BTN_EDIT_ACTION, "Изменить действие");
        ru.put(Key.BTN_DELETE, "Удал");
        ru.put(Key.BTN_ADD, "Добавить");
        ru.put(Key.BTN_ADD_ACTION, "Добавить действие");
        ru.put(Key.BTN_CLEAR, "Очистить");
        ru.put(Key.BTN_ON, "Вкл");
        ru.put(Key.BTN_OFF, "Выкл");
        ru.put(Key.BTN_THEMES, "Темы");
        ru.put(Key.BTN_DETECTION, "Детект ника");
        ru.put(Key.BTN_PROFILES, "Профили");

        ru.put(Key.TITLE_DETECTION, "Детект ника игрока");
        ru.put(Key.TITLE_PROFILES, "Профили");
        ru.put(Key.TITLE_PROFILE_EDIT, "Редактор профиля");
        ru.put(Key.TITLE_PROFILE_IMPORT, "Импорт профиля");
        ru.put(Key.MSG_DETECT_HINT,
                "Способы, которыми мод находит ник в чате");
        ru.put(Key.MSG_DETECT_EMPTY, "Все способы выключены");
        ru.put(Key.MSG_PROFILES_HINT,
                "Отдельные наборы макросов, действий и настроек");
        ru.put(Key.MSG_PROFILES_EMPTY, "Профилей пока нет");
        ru.put(Key.MSG_PROFILE_MACROS, "Макросы");
        ru.put(Key.MSG_PROFILE_ACTIONS, "Действия");
        ru.put(Key.MSG_PROFILE_NONE, "пусто");
        ru.put(Key.MSG_PROFILE_ACTIVE, "активен");

        ru.put(Key.THEME_OCEAN, "Океан");
        ru.put(Key.THEME_EMERALD, "Изумруд");
        ru.put(Key.THEME_AMETHYST, "Аметист");
        ru.put(Key.THEME_CRIMSON, "Кримзон");
        ru.put(Key.THEME_DAWN, "Рассвет");
        ru.put(Key.THEME_AURORA, "Аврора");
        ru.put(Key.THEME_NEON, "Неон");
        ru.put(Key.THEME_SUNSET, "Закат");

        ru.put(Key.TOGGLE_CHAT_MENTIONS, "Упоминания в чате");
        ru.put(Key.TOGGLE_COPY_NAME, "Копировать имя");
        ru.put(Key.TOGGLE_ACTIONS_POPUP, "Попап действий");
        ru.put(Key.TOGGLE_CONFIRM_DANGER, "Подтв. опасных");
        ru.put(Key.TOGGLE_ENABLED, "Включено");
        ru.put(Key.TOGGLE_DANGEROUS, "Опасно");
        ru.put(Key.TOGGLE_CONFIRMATION, "Подтверждать");
        ru.put(Key.TOGGLE_LANGUAGE, "Язык");
        ru.put(Key.TOGGLE_TOASTS, "Уведомления");
        ru.put(Key.TOGGLE_CHAT_INPUT, "Ник из поля ввода");
        ru.put(Key.TOGGLE_SAVE_POPUP_SIZE, "Сохранять размер попапа");
        ru.put(Key.TOGGLE_SAVE_CHAT_SIZE, "Сохранять размер чата");
        ru.put(Key.TOGGLE_CHAT_FRAME, "Рамка вокруг чата");
        ru.put(Key.TOGGLE_MOD_HUD, "HUD модератора в чате");
        ru.put(Key.TOGGLE_MOD_HUD_PHRASES, "Фразы в HUD модератора");
        ru.put(Key.BTN_SETTINGS, "Настройки");
        ru.put(Key.TOGGLE_MENU_ANIMATION, "Анимация рамок в меню");
        ru.put(Key.TOGGLE_CHAT_RESIZE, "Изменение размера чата");

        ru.put(Key.TOGGLE_DETECT_PLAIN, "Ник в тексте сообщения");
        ru.put(Key.TOGGLE_DETECT_DISPLAY, "Отображаемое имя (TAB)");
        ru.put(Key.TOGGLE_DETECT_CLICK, "Клик-событие команды");
        ru.put(Key.TOGGLE_DETECT_HOVER, "Текст при наведении");
        ru.put(Key.TOGGLE_DETECT_ALIASES, "Выученные алиасы");
        ru.put(Key.TOGGLE_DETECT_UNICODE, "Юникод-замена (AV A)");
        ru.put(Key.TOGGLE_DETECT_DECOR, "Знаки вокруг ника");
        ru.put(Key.TOGGLE_DETECT_LINE, "Клик по нику в чате");
        ru.put(Key.TOGGLE_DETECT_CACHE, "Кэш видевших ников");
        ru.put(Key.TOGGLE_DETECT_FUZZY, "Поиск с опечатками");

        ru.put(Key.LABEL_NAME, "Название");
        ru.put(Key.LABEL_DESCRIPTION, "Описание");
        ru.put(Key.LABEL_COMMAND, "Команда");
        ru.put(Key.LABEL_COMMAND_HINT, "(поддерживается <player>)");
        ru.put(Key.LABEL_PLACEHOLDERS, "Плейсхолдеры");
        ru.put(Key.LABEL_ALIASES, "Алиасы");
        ru.put(Key.LABEL_ICON, "Иконка");
        ru.put(Key.LABEL_ACTION, "Действие");
        ru.put(Key.LABEL_SETTINGS, "Настройки");

        ru.put(Key.MSG_SELECT_MACRO, "Выберите макрос для изменения");
        ru.put(Key.MSG_NO_MACROS, "Макросов пока нет. Нажмите «Создать макрос».");
        ru.put(Key.MSG_NO_DESCRIPTION, "Без описания");
        ru.put(Key.MSG_UNNAMED, "Макрос без названия");
        ru.put(Key.MSG_ACTIONS_HINT, "Пункты попапа действий в чате");
        ru.put(Key.MSG_EMOJI_HINT, "Смайлик вставляется в активное поле");
        ru.put(Key.MSG_NO_ACTIONS, "Действий нет. Нажмите «Добавить действие».");
        ru.put(Key.MSG_NO_HISTORY, "История пока пуста");
        ru.put(Key.MSG_HISTORY_HINT, "Все ваши наказания и ответы сервера");

        ru.put(Key.OVERLAY_NO_ACTIONS, "Нет действий");
        ru.put(Key.OVERLAY_CUSTOM_MACROS, "Свои макросы");
        ru.put(Key.OVERLAY_CONFIRM, "Ещё клик: %s");
        ru.put(Key.OVERLAY_HINT_CLOSE, "ESC — закрыть");
        ru.put(Key.OVERLAY_HINT_SCROLL, "колесо — навигация");
        ru.put(Key.OVERLAY_HINT_DRAG,
                "\u2197 \u043F\u0435\u0440\u0435\u0442\u0430\u0449\u0438\u0442\u044C \u2022 \u2198 \u0440\u0430\u0437\u043C\u0435\u0440");
        ru.put(Key.OVERLAY_NP, "NP: %s");
        ru.put(Key.OVERLAY_COPY_NAME, "Копировать имя");
        ru.put(Key.OVERLAY_DUP_CHAT, "Дублировать в чат");
        ru.put(Key.OVERLAY_ALIAS_TITLE, "Выберите пункт");

        ru.put(Key.MSG_ALIAS_HELP, String.join("\n",
                "<alias> — список возможных вариантов одного и того же действия.",
                "",
                "Пример:",
                "Команда: /mute <player> <alias>",
                "Алиасы: «30m Капс», «Флуд»",
                "Результат: /mute Notch 30m Капс",
                "",
                "Алиас — просто текст: время и причина пишутся в нём.",
                "Если список пуст — <alias> заменится пустотой."));

        ru.put(Key.PH_PLAYER, "Ник игрока-цели.\nПример: /mute <player> 10m → /mute Notch 10m");
        ru.put(Key.PH_STAFF, "Ваш текущий ник.\nПример: /warn <player> проверял <staff>");
        ru.put(Key.PH_X, "Ваша координата X (блоки).\nПример: /tp <player> <x> <y> <z>");
        ru.put(Key.PH_Y, "Ваша координата Y (блоки).\nПример: /tp <player> <x> <y> <z>");
        ru.put(Key.PH_Z, "Ваша координата Z (блоки).\nПример: /tp <player> <x> <y> <z>");
        ru.put(Key.PH_PING, "Пинг цели в миллисекундах.\nПример: /warn <player> лаги (<ping> ms)");
        ru.put(Key.PH_SERVER, "Адрес текущего сервера.\nПолезно на сетях с лобби.");

        ru.put(Key.HOVER_CLICK, "Клик: действия");
        ru.put(Key.HOVER_SHIFT_CLICK, "Shift+клик: скопировать имя");

        ru.put(Key.HISTORY_TITLE, "История действий");
        ru.put(Key.HIST_COL_TIME, "Время");
        ru.put(Key.HIST_COL_ACTION, "Действие");
        ru.put(Key.HIST_COL_PLAYER, "Игрок");
        ru.put(Key.PUN_TYPE_BAN, "Бан");
        ru.put(Key.PUN_TYPE_MUTE, "Мут");
        ru.put(Key.PUN_TYPE_KICK, "Кик");
        ru.put(Key.PUN_TYPE_WARN, "Пред");
        ru.put(Key.PUN_TYPE_FREEZE, "Заморозка");
        ru.put(Key.PUN_TYPE_OTHER, "Прочее");
        ru.put(Key.PUN_TYPE_UNMUTE, "Размут");
        ru.put(Key.PUN_TYPE_UNBAN, "Разбан");
        ru.put(Key.PUN_STATUS_PENDING, "Ожидает ответа");
        ru.put(Key.PUN_STATUS_DONE, "Подтверждён");
        ru.put(Key.PUN_STATUS_NO_RESPONSE, "Нет ответа");
        ru.put(Key.PUN_STATUS_FAILED, "Отклонён");
        ru.put(Key.PUN_PLAYER, "Игрок: %s");
        ru.put(Key.PUN_NO_RESPONSE_TEXT, "Ответ сервера не получен");

        ru.put(Key.STAT_TOTAL, "Всего");
        ru.put(Key.STAT_CONFIRMED, "Подтверждено");
        ru.put(Key.STAT_FAILED, "Отклонено");
        ru.put(Key.STAT_NO_RESPONSE, "Без ответа");
        ru.put(Key.STAT_AWAITING, "Ожидают");
        ru.put(Key.STAT_PLAYERS, "Игроков");
        ru.put(Key.STAT_TOP_PLAYER, "Чаще всех");
        ru.put(Key.STAT_MODERATORS, "Модераторов");
        ru.put(Key.STAT_FROM_MACROS, "Из макросов");
        ru.put(Key.STAT_BY_TYPE, "По типам");
        ru.put(Key.STAT_FROM_ACTIONS, "Из действий");
        ru.put(Key.HIST_COL_STATUS, "Статус");
        ru.put(Key.HIST_COL_SERVER, "Сервер");
        ru.put(Key.HIST_SERVERS, "Серверы");
        ru.put(Key.HIST_ALL_SERVERS, "Все серверы");
        ru.put(Key.HIST_FILTER_TYPE, "Тип");
        ru.put(Key.HIST_FILTER_STATUS, "Статус");
        ru.put(Key.HIST_FILTER_ALL, "все");
        ru.put(Key.HIST_FILTER_RESET, "Сброс");
        ru.put(Key.HIST_FILTER_NICK_HINT, "ник игрока…");
        ru.put(Key.BTN_HIST_CLEAR_ONE, "Очистить этот");
        ru.put(Key.TITLE_SETTINGS, "Настройки");
        ru.put(Key.TITLE_SERVER_RENAME, "Название сервера");
        ru.put(Key.LABEL_SERVER_NAME, "Название");
        ru.put(Key.LABEL_SERVER_KEY, "Адрес");
        ru.put(Key.HUD_TITLE, "Модератор");
        ru.put(Key.HUD_MODERATOR, "Модератор");
        ru.put(Key.HUD_SERVER, "Сервер");
        ru.put(Key.HUD_SESSION, "В сессии");
        ru.put(Key.HUD_PUNISHED, "Наказаний");
        ru.put(Key.HUD_OFFLINE, "не в игре");
        ru.put(Key.HIST_HINT_CLICK, "Нажмите на запись — подробности");


        ru.put(Key.DETAIL_TITLE, "Детали наказания");
        ru.put(Key.DETAIL_PLAYER, "Нарушитель");
        ru.put(Key.DETAIL_TIME, "Время наказания");
        ru.put(Key.DETAIL_MODERATOR, "Модератор");
        ru.put(Key.DETAIL_TYPE, "Вид наказания");
        ru.put(Key.DETAIL_MACRO, "Макрос");
        ru.put(Key.DETAIL_ACTION, "Действие");
        ru.put(Key.DETAIL_COMMAND, "Команда");
        ru.put(Key.DETAIL_STATUS, "Статус");
        ru.put(Key.DETAIL_SERVER_REPLY, "Ответ сервера");
        ru.put(Key.DETAIL_UNKNOWN, "неизвестно");
        ru.put(Key.DETAIL_NONE, "—");

        ru.put(Key.BTN_PROFILE_CREATE, "Создать");
        ru.put(Key.BTN_PROFILE_SAVE, "Сохранить");
        ru.put(Key.BTN_PROFILE_APPLY, "Применить");
        ru.put(Key.BTN_PROFILE_EXPORT, "Экспорт");
        ru.put(Key.BTN_PROFILE_IMPORT, "Импорт");
        ru.put(Key.BTN_PROFILE_DELETE, "Удал");
        ru.put(Key.BTN_PROFILE_OPEN, "Открыть");
        ru.put(Key.LABEL_PROFILE_NAME, "Название профиля");
        ru.put(Key.LABEL_PROFILE_THEME, "Тема профиля");

        ru.put(Key.BTN_PROFILE_OPEN_FOLDER, "Открыть папку");
        ru.put(Key.BTN_REFRESH, "Обновить");
        ru.put(Key.MSG_PROFILE_NO_FILES, "В папке пока нет профилей");
        ru.put(Key.MSG_PROFILE_IMPORT_HINT,
                "Выберите файл профиля из папки");
        ru.put(Key.MSG_PROFILE_FOLDER, "Папка профилей: %s");
        ru.put(Key.TOAST_PROFILE_EXPORT_FAILED, "Не удалось сохранить файл");
        ru.put(Key.TOAST_PROFILE_FOLDER_OPENED, "Папка профилей открыта");
        ru.put(Key.TOAST_COPIED, "Скопировано: %s");
        ru.put(Key.TOAST_DUP_CHAT, "Добавлено в чат: %s");
        ru.put(Key.TOAST_EXECUTED, "Выполнено: %s");
        ru.put(Key.TOAST_PROFILE_SAVED, "Профиль сохранён: %s");
        ru.put(Key.TOAST_PROFILE_APPLIED, "Профиль применён: %s");
        ru.put(Key.TOAST_PROFILE_CREATED, "Профиль создан: %s");
        ru.put(Key.TOAST_PROFILE_DELETED, "Профиль удалён: %s");
        ru.put(Key.TOAST_PROFILE_EXPORTED, "Профиль сохранён: %s");
        ru.put(Key.TOAST_PROFILE_IMPORTED, "Профиль импортирован: %s");
        ru.put(Key.TOAST_PROFILE_BAD_JSON, "Не удалось разобрать профиль");
        ru.put(Key.TOAST_PROFILE_LAST, "Нельзя удалить последний профиль");
        ru.put(Key.TOAST_THEME_CHANGED, "Тема: %s");

        Map<String, String> en = new HashMap<>();
        en.put(Key.APP_TITLE, "StaffTools");
        en.put(Key.APP_TAGLINE, "Staff utilities & macros");

        en.put(Key.BTN_MACROS, "Macros");
        en.put(Key.BTN_CREATE_MACRO, "Create Macro");
        en.put(Key.BTN_PLAYER_ACTIONS, "Player Actions");
        en.put(Key.BTN_HISTORY, "History");
        en.put(Key.BTN_CLOSE, "Close");
        en.put(Key.BTN_BACK, "Back");
        en.put(Key.BTN_SAVE, "Save");
        en.put(Key.BTN_CANCEL, "Cancel");
        en.put(Key.BTN_EDIT, "Edit");
        en.put(Key.BTN_EDIT_MACRO, "Edit Macro");
        en.put(Key.BTN_EDIT_ACTION, "Edit action");
        en.put(Key.BTN_DELETE, "Del");
        en.put(Key.BTN_ADD, "Add");
        en.put(Key.BTN_ADD_ACTION, "Add action");
        en.put(Key.BTN_CLEAR, "Clear");
        en.put(Key.BTN_ON, "On");
        en.put(Key.BTN_OFF, "Off");
        en.put(Key.BTN_THEMES, "Themes");
        en.put(Key.BTN_DETECTION, "Nick detection");
        en.put(Key.BTN_PROFILES, "Profiles");

        en.put(Key.TITLE_DETECTION, "Nick detection");
        en.put(Key.TITLE_PROFILES, "Profiles");
        en.put(Key.TITLE_PROFILE_EDIT, "Profile editor");
        en.put(Key.TITLE_PROFILE_IMPORT, "Import profile");
        en.put(Key.MSG_DETECT_HINT,
                "How the mod finds the nick in chat");
        en.put(Key.MSG_DETECT_EMPTY, "All methods are disabled");
        en.put(Key.MSG_PROFILES_HINT,
                "Separate sets of macros, actions and settings");
        en.put(Key.MSG_PROFILES_EMPTY, "No profiles yet");
        en.put(Key.MSG_PROFILE_MACROS, "Macros");
        en.put(Key.MSG_PROFILE_ACTIONS, "Actions");
        en.put(Key.MSG_PROFILE_NONE, "empty");
        en.put(Key.MSG_PROFILE_ACTIVE, "active");

        en.put(Key.THEME_OCEAN, "Ocean");
        en.put(Key.THEME_EMERALD, "Emerald");
        en.put(Key.THEME_AMETHYST, "Amethyst");
        en.put(Key.THEME_CRIMSON, "Crimson");
        en.put(Key.THEME_DAWN, "Dawn");
        en.put(Key.THEME_AURORA, "Aurora");
        en.put(Key.THEME_NEON, "Neon");
        en.put(Key.THEME_SUNSET, "Sunset");

        en.put(Key.TOGGLE_CHAT_MENTIONS, "Chat mentions");
        en.put(Key.TOGGLE_COPY_NAME, "Copy name");
        en.put(Key.TOGGLE_ACTIONS_POPUP, "Actions popup");
        en.put(Key.TOGGLE_CONFIRM_DANGER, "Confirm danger");
        en.put(Key.TOGGLE_ENABLED, "Enabled");
        en.put(Key.TOGGLE_DANGEROUS, "Dangerous");
        en.put(Key.TOGGLE_CONFIRMATION, "Confirm");
        en.put(Key.TOGGLE_LANGUAGE, "Language");
        en.put(Key.TOGGLE_TOASTS, "Notifications");
        en.put(Key.TOGGLE_CHAT_INPUT, "Chat-input nick");
        en.put(Key.TOGGLE_SAVE_POPUP_SIZE, "Save popup size");
        en.put(Key.TOGGLE_SAVE_CHAT_SIZE, "Save chat size");
        en.put(Key.TOGGLE_CHAT_FRAME, "Frame around the chat");
        en.put(Key.TOGGLE_MOD_HUD, "Moderator HUD in chat");
        en.put(Key.TOGGLE_MOD_HUD_PHRASES, "Phrases in the moderator HUD");
        en.put(Key.BTN_SETTINGS, "Settings");
        en.put(Key.TOGGLE_MENU_ANIMATION, "Menu frame animation");
        en.put(Key.TOGGLE_CHAT_RESIZE, "Chat resize handle");

        en.put(Key.TOGGLE_DETECT_PLAIN, "Nick in message text");
        en.put(Key.TOGGLE_DETECT_DISPLAY, "Tab-list display name");
        en.put(Key.TOGGLE_DETECT_CLICK, "Command click event");
        en.put(Key.TOGGLE_DETECT_HOVER, "Hover tooltip text");
        en.put(Key.TOGGLE_DETECT_ALIASES, "Learned aliases");
        en.put(Key.TOGGLE_DETECT_UNICODE, "Unicode lookup (AV A)");
        en.put(Key.TOGGLE_DETECT_DECOR, "Signs around the nick");
        en.put(Key.TOGGLE_DETECT_LINE, "Click a nick in chat");
        en.put(Key.TOGGLE_DETECT_CACHE, "Cache of seen nicks");
        en.put(Key.TOGGLE_DETECT_FUZZY, "Typo-tolerant search");

        en.put(Key.LABEL_NAME, "Name");
        en.put(Key.LABEL_DESCRIPTION, "Description");
        en.put(Key.LABEL_COMMAND, "Command");
        en.put(Key.LABEL_COMMAND_HINT, "(supports <player>)");
        en.put(Key.LABEL_PLACEHOLDERS, "Placeholders");
        en.put(Key.LABEL_ALIASES, "Aliases");
        en.put(Key.LABEL_ICON, "Icon");
        en.put(Key.LABEL_ACTION, "Action");
        en.put(Key.LABEL_SETTINGS, "Settings");

        en.put(Key.MSG_SELECT_MACRO, "Select a macro to edit it");
        en.put(Key.MSG_NO_MACROS, "No macros yet. Press \"Create Macro\".");
        en.put(Key.MSG_NO_DESCRIPTION, "No description");
        en.put(Key.MSG_UNNAMED, "Unnamed Macro");
        en.put(Key.MSG_ACTIONS_HINT, "Items shown in the in-chat actions popup");
        en.put(Key.MSG_EMOJI_HINT, "Emoji is inserted into the focused field");
        en.put(Key.MSG_NO_ACTIONS, "No actions. Press \"Add action\".");
        en.put(Key.MSG_NO_HISTORY, "History is empty");
        en.put(Key.MSG_HISTORY_HINT, "All your punishments and server replies");

        en.put(Key.OVERLAY_NO_ACTIONS, "No actions");
        en.put(Key.OVERLAY_CUSTOM_MACROS, "Custom macros");
        en.put(Key.OVERLAY_CONFIRM, "Click again: %s");
        en.put(Key.OVERLAY_HINT_CLOSE, "ESC to close");
        en.put(Key.OVERLAY_HINT_SCROLL, "scroll to navigate");
        en.put(Key.OVERLAY_HINT_DRAG,
                "\u2197 drag \u2022 \u2198 resize");
        en.put(Key.OVERLAY_NP, "NP: %s");
        en.put(Key.OVERLAY_COPY_NAME, "Copy name");
        en.put(Key.OVERLAY_DUP_CHAT, "Duplicate to chat");
        en.put(Key.OVERLAY_ALIAS_TITLE, "Pick an option");

        en.put(Key.MSG_ALIAS_HELP, String.join("\n",
                "<alias> inserts the selected punishment variant.",
                "",
                "Example:",
                "Command: /mute <player> <alias>",
                "Aliases: \"30m Caps\", \"Flood\"",
                "Result: /mute Notch 30m Caps",
                "",
                "An alias is plain text: put duration and reason in it.",
                "If the list is empty — <alias> becomes empty."));

        en.put(Key.PH_PLAYER, "Target player's name.\nExample: /mute <player> 10m → /mute Notch 10m");
        en.put(Key.PH_STAFF, "Your current nick.\nExample: /warn <player> checked by <staff>");
        en.put(Key.PH_X, "Your X coordinate (blocks).\nExample: /tp <player> <x> <y> <z>");
        en.put(Key.PH_Y, "Your Y coordinate (blocks).\nExample: /tp <player> <x> <y> <z>");
        en.put(Key.PH_Z, "Your Z coordinate (blocks).\nExample: /tp <player> <x> <y> <z>");
        en.put(Key.PH_PING, "Target latency in ms (live).\nExample: /warn <player> lagging (<ping> ms)");
        en.put(Key.PH_SERVER, "Current server address.\nUseful on BungeeCord-style networks.");

        en.put(Key.HOVER_CLICK, "Click: actions");
        en.put(Key.HOVER_SHIFT_CLICK, "Shift-click: copy name");

        en.put(Key.HISTORY_TITLE, "Action History");
        en.put(Key.HIST_COL_TIME, "Time");
        en.put(Key.HIST_COL_ACTION, "Action");
        en.put(Key.HIST_COL_PLAYER, "Player");
        en.put(Key.PUN_TYPE_BAN, "Ban");
        en.put(Key.PUN_TYPE_MUTE, "Mute");
        en.put(Key.PUN_TYPE_KICK, "Kick");
        en.put(Key.PUN_TYPE_WARN, "Warn");
        en.put(Key.PUN_TYPE_FREEZE, "Freeze");
        en.put(Key.PUN_TYPE_OTHER, "Command");
        en.put(Key.PUN_TYPE_UNMUTE, "Unmute");
        en.put(Key.PUN_TYPE_UNBAN, "Unban");
        en.put(Key.PUN_STATUS_PENDING, "awaiting response");
        en.put(Key.PUN_STATUS_DONE, "confirmed");
        en.put(Key.PUN_STATUS_NO_RESPONSE, "no response");
        en.put(Key.PUN_STATUS_FAILED, "rejected");
        en.put(Key.PUN_PLAYER, "Player: %s");
        en.put(Key.PUN_NO_RESPONSE_TEXT, "No server response received");

        en.put(Key.STAT_TOTAL, "Total");
        en.put(Key.STAT_CONFIRMED, "Confirmed");
        en.put(Key.STAT_FAILED, "Rejected");
        en.put(Key.STAT_NO_RESPONSE, "No reply");
        en.put(Key.STAT_AWAITING, "Pending");
        en.put(Key.STAT_PLAYERS, "Players");
        en.put(Key.STAT_TOP_PLAYER, "Most punished");
        en.put(Key.STAT_MODERATORS, "Moderators");
        en.put(Key.STAT_FROM_MACROS, "From macros");
        en.put(Key.STAT_BY_TYPE, "By type");
        en.put(Key.STAT_FROM_ACTIONS, "From actions");
        en.put(Key.HIST_COL_STATUS, "Status");
        en.put(Key.HIST_COL_SERVER, "Server");
        en.put(Key.HIST_SERVERS, "Servers");
        en.put(Key.HIST_ALL_SERVERS, "All servers");
        en.put(Key.HIST_FILTER_TYPE, "Type");
        en.put(Key.HIST_FILTER_STATUS, "Status");
        en.put(Key.HIST_FILTER_ALL, "all");
        en.put(Key.HIST_FILTER_RESET, "Reset");
        en.put(Key.HIST_FILTER_NICK_HINT, "player nick…");
        en.put(Key.BTN_HIST_CLEAR_ONE, "Clear this");
        en.put(Key.TITLE_SETTINGS, "Settings");
        en.put(Key.TITLE_SERVER_RENAME, "Server name");
        en.put(Key.LABEL_SERVER_NAME, "Name");
        en.put(Key.LABEL_SERVER_KEY, "Address");
        en.put(Key.HUD_TITLE, "Moderator");
        en.put(Key.HUD_MODERATOR, "Moderator");
        en.put(Key.HUD_SERVER, "Server");
        en.put(Key.HUD_SESSION, "Session");
        en.put(Key.HUD_PUNISHED, "Punishments");
        en.put(Key.HUD_OFFLINE, "not in game");
        en.put(Key.HIST_HINT_CLICK, "Click an entry for details");

        en.put(Key.DETAIL_TITLE, "Punishment details");
        en.put(Key.DETAIL_PLAYER, "Player");
        en.put(Key.DETAIL_TIME, "Time");
        en.put(Key.DETAIL_MODERATOR, "Moderator");
        en.put(Key.DETAIL_TYPE, "Type");
        en.put(Key.DETAIL_MACRO, "Macro");
        en.put(Key.DETAIL_ACTION, "Action");
        en.put(Key.DETAIL_COMMAND, "Command");
        en.put(Key.DETAIL_STATUS, "Status");
        en.put(Key.DETAIL_SERVER_REPLY, "Server reply");
        en.put(Key.DETAIL_UNKNOWN, "unknown");
        en.put(Key.DETAIL_NONE, "—");

        en.put(Key.BTN_PROFILE_CREATE, "Create");
        en.put(Key.BTN_PROFILE_SAVE, "Save");
        en.put(Key.BTN_PROFILE_APPLY, "Apply");
        en.put(Key.BTN_PROFILE_EXPORT, "Export");
        en.put(Key.BTN_PROFILE_IMPORT, "Import");
        en.put(Key.BTN_PROFILE_DELETE, "Del");
        en.put(Key.BTN_PROFILE_OPEN, "Open");
        en.put(Key.LABEL_PROFILE_NAME, "Profile name");
        en.put(Key.LABEL_PROFILE_THEME, "Profile theme");

        en.put(Key.BTN_PROFILE_OPEN_FOLDER, "Open folder");
        en.put(Key.BTN_REFRESH, "Refresh");
        en.put(Key.MSG_PROFILE_NO_FILES, "No profile files yet");
        en.put(Key.MSG_PROFILE_IMPORT_HINT,
                "Pick a profile file from the folder");
        en.put(Key.MSG_PROFILE_FOLDER, "Profile folder: %s");
        en.put(Key.TOAST_PROFILE_EXPORT_FAILED, "Could not write the file");
        en.put(Key.TOAST_PROFILE_FOLDER_OPENED, "Profile folder opened");
        en.put(Key.TOAST_COPIED, "Copied: %s");
        en.put(Key.TOAST_DUP_CHAT, "Inserted into chat: %s");
        en.put(Key.TOAST_EXECUTED, "Executed: %s");
        en.put(Key.TOAST_PROFILE_SAVED, "Profile saved: %s");
        en.put(Key.TOAST_PROFILE_APPLIED, "Profile applied: %s");
        en.put(Key.TOAST_PROFILE_CREATED, "Profile created: %s");
        en.put(Key.TOAST_PROFILE_DELETED, "Profile deleted: %s");
        en.put(Key.TOAST_PROFILE_EXPORTED, "Profile saved: %s");
        en.put(Key.TOAST_PROFILE_IMPORTED, "Profile imported: %s");
        en.put(Key.TOAST_PROFILE_BAD_JSON, "Could not parse the profile");
        en.put(Key.TOAST_PROFILE_LAST, "Cannot delete the last profile");
        en.put(Key.TOAST_THEME_CHANGED, "Theme: %s");

        TEXTS.put(Language.RU, ru);
        TEXTS.put(Language.EN, en);
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
        String value = TEXTS.get(current).get(key);
        return value != null ? value : key;
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
