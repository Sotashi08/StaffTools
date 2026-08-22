package com.uwdie.stafftools.client.i18n;

import com.google.gson.annotations.SerializedName;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

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
        public static final String OVERLAY_ALIAS_TITLE = "overlay.aliasTitle";

        // Chat hover
        public static final String HOVER_CLICK = "hover.click";
        public static final String HOVER_SHIFT_CLICK = "hover.shiftClick";

        // Toasts
        public static final String TOAST_COPIED = "toast.copied";
        public static final String TOAST_EXECUTED = "toast.executed";

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
        public static final String PUN_STATUS_PENDING = "pun.status.pending";
        public static final String PUN_STATUS_DONE = "pun.status.done";
        public static final String PUN_STATUS_NO_RESPONSE = "pun.status.noResponse";
        public static final String PUN_PLAYER = "pun.player";
        public static final String PUN_NO_RESPONSE_TEXT = "pun.noResponseText";
    }

    private static Language current = Language.RU;

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
        ru.put(Key.OVERLAY_HINT_DRAG, "");
        ru.put(Key.OVERLAY_NP, "NP: %s");
        ru.put(Key.OVERLAY_COPY_NAME, "Копировать имя");
        ru.put(Key.OVERLAY_ALIAS_TITLE, "Выберите пункт");

        ru.put(Key.MSG_ALIAS_HELP, String.join("\n",
                "<alias> — подстановка выбранного пункта наказания.",
                "",
                "Пример:",
                "Команда: /mute <player> <alias>",
                "Алиасы: «30m Капс», «Флуд»",
                "Результат: /mute Notch 30m Капс",
                "",
                "Алиас — просто текст: время и причина пишутся в нём.",
                "Если список пуст — <alias> заменится пустотой.",
                "(Shift — эта справка)"));

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
        ru.put(Key.PUN_TYPE_WARN, "Варн");
        ru.put(Key.PUN_TYPE_FREEZE, "Заморозка");
        ru.put(Key.PUN_TYPE_OTHER, "Команда");
        ru.put(Key.PUN_STATUS_PENDING, "ожидание ответа");
        ru.put(Key.PUN_STATUS_DONE, "ответ получен");
        ru.put(Key.PUN_STATUS_NO_RESPONSE, "нет ответа");
        ru.put(Key.PUN_PLAYER, "Игрок: %s");
        ru.put(Key.PUN_NO_RESPONSE_TEXT, "Ответ сервера не получен");

        ru.put(Key.TOAST_COPIED, "Скопировано: %s");
        ru.put(Key.TOAST_EXECUTED, "Выполнено: %s");

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
        en.put(Key.OVERLAY_HINT_DRAG, "drag by corner");
        en.put(Key.OVERLAY_NP, "NP: %s");
        en.put(Key.OVERLAY_COPY_NAME, "Copy name");
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
                "If the list is empty — <alias> becomes empty.",
                "(Shift — this help)"));

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
        en.put(Key.PUN_STATUS_PENDING, "awaiting response");
        en.put(Key.PUN_STATUS_DONE, "confirmed");
        en.put(Key.PUN_STATUS_NO_RESPONSE, "no response");
        en.put(Key.PUN_PLAYER, "Player: %s");
        en.put(Key.PUN_NO_RESPONSE_TEXT, "No server response received");

        en.put(Key.TOAST_COPIED, "Copied: %s");
        en.put(Key.TOAST_EXECUTED, "Executed: %s");

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
