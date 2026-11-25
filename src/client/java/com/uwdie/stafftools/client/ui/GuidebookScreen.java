package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game guidebook that documents every feature of the mod.
 *
 * <p>Content is written in a tiny markdown-like format and rendered by
 * {@link #renderTheme}: {@code # } headings, {@code - } bullets,
 * {@code `code`} inline code and blank-line-separated paragraphs. The
 * whole page scrolls as one column.
 */
public class GuidebookScreen extends StScreen {

    private static final int PAD = 8;
    private static final int H1_H = 26;
    private static final int H2_H = 18;
    private static final int LINE_H = 13;
    private static final int PARA_GAP = 10;
    private static final int BULLET_GAP = 5;
    private static final int SECTION_GAP = 14;
    private static final int CODE_PAD = 5;

    private final Screen parent;

    /** One laid-out block of the document. */
    private record Block(int kind, String text, int height) {
    }

    private static final int KIND_H1 = 0;
    private static final int KIND_H2 = 1;
    private static final int KIND_P = 2;
    private static final int KIND_BULLET = 3;
    private static final int KIND_CODE = 4;

    private final List<Block> blocks = new ArrayList<>();

    private float scroll;
    private int contentHeight;

    public GuidebookScreen(Screen parent) {
        super(Lang.text(Key.TITLE_GUIDEBOOK));
        this.parent = parent;
    }

    @Override
    protected void init() {
        resetSlots();
        configureShell();
        buildBlocks();
    }

    /** Builds the document from the current language. */
    private void buildBlocks() {

        blocks.clear();

        boolean ru = Lang.getLanguage() == Lang.Language.RU;

        if (ru) {
            buildRu();
        } else {
            buildEn();
        }
    }

    private void buildRu() {

        h1("StaffTools — гайдбук");

        p("Клиентский мод для модерации: кликабельные ники в чате, "
                + "действия над игроком в два клика, макросы, история "
                + "наказаний, аудит и темы. Сервер ставить не нужно — "
                + "достаточно Fabric API.");

        h2("Быстрый старт");
        bullet("Нажмите Right Shift — откроется главное меню.");
        bullet("Наведите курсор на ник в чате и нажмите — появится окно действий.");

        h2("Главное меню");
        p("Открывается клавишей Right Shift. Слева — панель разделов: "
                + "макросы, действия, история, профили, аудит, настройки. "
                + "Внизу панели — этот гайдбук.");
        bullet("Right Shift — открыть/закрыть меню.");
        bullet("ESC — закрыть экран или всплывающее окно.");
        bullet("Настройки, темы и профили применяются сразу.");

        h2("Ники в чате");
        p("Мод находит ники игроков в сообщениях и делает их "
                + "кликабельными. Нажмите на ник — откроется окно "
                + "действий. Shift+клик копирует ник, Alt+клик копирует "
                + "всю строку чата.");
        p("При наведении на ник показывается карточка: статус, пинг, "
                + "режим игры, число наказаний и сервер.");
        bullet("Способы детекта настраиваются в «Детект ника».");
        bullet("Подсветку можно отключить — клик останется.");
        bullet("Работает даже с изменёнными никами EssentialsX.");

        h2("Действия над игроком");
        p("Окно действий открывается по клику на ник. В нём — ваши "
                + "действия (мут, бан, варн, кик и свои). Опасные "
                + "действия требуют подтверждения.");
        bullet("Действия настраиваются в разделе «Действия».");
        bullet("У каждого действия — иконка, команда и флаг опасности.");
        bullet("Опасное действие спрашивает «Ещё клик» перед запуском.");

        h2("Макросы");
        p("Макрос — это команда с подстановками. Создайте макрос, "
                + "впишите команду и используйте плейсхолдеры:");
        code("/tempmute <player> 10m <reason>");
        bullet("<player> — ник цели.");
        bullet("<staff> — ваш ник.");
        bullet("<x> <y> <z> — ваши координаты.");
        bullet("<ping> — пинг цели.");
        bullet("<server> — адрес сервера.");
        bullet("<alias> — выбор варианта из списка.");
        bullet("<reason> — причина, вводится вручную в окне.");

        h2("Плейсхолдер <reason>");
        p("Если в команде есть <reason>, перед отправкой появится окно "
                + "с полем ввода. Впишите причину и нажмите "
                + "«Подтвердить» (или Enter). Случайный Enter сразу "
                + "после открытия не сработает — это защита от "
                + "промаха.");
        code("/tempmute <player> 10m <reason>");
        p("Результат: /tempmute Notch 10m Флуд");

        h2("История наказаний");
        p("Все выданные наказания и ответы сервера сохраняются. Запись "
                + "может быть в ожидании, подтверждена, отклонена или "
                + "без ответа. Нажмите на запись — откроются детали.");
        bullet("Фильтры по типу, статусу и серверу.");
        bullet("Хранит до 300 записей.");
        bullet("Статус можно поправить вручную в деталях.");

        h2("Профили");
        p("Профиль — набор настроек, макросов и действий. Можно "
                + "создать несколько профилей и переключаться между "
                + "ними, а также экспортировать и импортировать их.");
        bullet("Удобно для разных серверов или ролей.");
        bullet("Экспорт — в файл, импорт — из папки профилей.");

        h2("Аудит и профилировщик");
        p("Аудит — журнал всего, что делает мод. Профилировщик "
                + "показывает время выполнения горячих участков: чат, "
                + "детект, макросы.");
        bullet("Фильтр журнала по категориям.");
        bullet("Замеры помогают найти медленные места.");

        h2("База данных (/stdb)");
        p("Ники, наказания и аудит хранятся в SQLite (config/stafftools.db).");
        bullet("/stdb list [page] — список ников (кликабельны).");
        bullet("/stdb search <текст> — поиск ников по вхождению.");
        bullet("/stdb hardsearch <текст> — то же, явный поиск.");
        bullet("/stdb status — размер файла и количество строк.");
        bullet("/stdb reload — перезагрузить кэш ников из БД.");
        bullet("/stdb delete <ник> — удалить ник из БД.");
        bullet("/stdb export — экспорт всей БД в CSV (config/).");

        h2("Темы и интерфейс");
        p("12 тем с градиентами, анимации, свечение, скорость "
                + "анимаций и рамка чата настраиваются в «Настройки».");
        bullet("Акцентный цвет и подсветку ников можно выбрать.");
        bullet("Анимации отключаются одним переключателем.");
    }

    private void buildEn() {

        h1("StaffTools — guidebook");

        p("A client-side moderation mod: clickable nicks in chat, "
                + "two-click player actions, macros, punishment history, "
                + "audit and themes. No server install needed — just "
                + "Fabric API.");

        h2("Quick start");
        bullet("Press Right Shift to open the main menu.");
        bullet("Hover a nick in chat and click it to open actions.");

        h2("Main menu");
        p("Opened with Right Shift. On the left is the section rail: "
                + "macros, actions, history, profiles, audit, settings. "
                + "At the bottom of the rail is this guidebook.");
        bullet("Right Shift — open/close the menu.");
        bullet("ESC — close a screen or popup.");
        bullet("Settings, themes and profiles apply instantly.");

        h2("Nicks in chat");
        p("The mod finds player nicks in messages and makes them "
                + "clickable. Click a nick to open the actions window. "
                + "Shift+click copies the nick, Alt+click copies the "
                + "whole chat line.");
        p("Hovering a nick shows a card: status, ping, game mode, "
                + "punishment count and server.");
        bullet("Detection methods are set in \"Nick detection\".");
        bullet("Highlighting can be off; clicking still works.");
        bullet("Works even with changed EssentialsX nicks.");

        h2("Player actions");
        p("The actions window opens on a nick click. It lists your "
                + "actions (mute, ban, warn, kick and custom). "
                + "Dangerous actions require confirmation.");
        bullet("Actions are configured in the \"Actions\" section.");
        bullet("Each action has an icon, command and danger flag.");
        bullet("A dangerous action asks \"Click again\" before running.");

        h2("Macros");
        p("A macro is a command with placeholders. Create a macro, "
                + "type the command and use placeholders:");
        code("/tempmute <player> 10m <reason>");
        bullet("<player> — target nick.");
        bullet("<staff> — your nick.");
        bullet("<x> <y> <z> — your coordinates.");
        bullet("<ping> — target latency.");
        bullet("<server> — server address.");
        bullet("<alias> — pick a variant from a list.");
        bullet("<reason> — a reason typed in manually.");

        h2("The <reason> placeholder");
        p("If a command contains <reason>, a window with an input field "
                + "appears before sending. Type the reason and press "
                + "\"Confirm\" (or Enter). A stray Enter right after "
                + "opening does nothing — that is the safety guard.");
        code("/tempmute <player> 10m <reason>");
        p("Result: /tempmute Notch 10m Flood");

        h2("Punishment history");
        p("Every issued punishment and the server reply are stored. An "
                + "entry can be pending, confirmed, rejected or without "
                + "a reply. Click an entry for details.");
        bullet("Filters by type, status and server.");
        bullet("Keeps up to 300 records.");
        bullet("Status can be fixed manually in the details.");

        h2("Profiles");
        p("A profile is a set of settings, macros and actions. Create "
                + "several and switch between them, and export or "
                + "import them.");
        bullet("Great for different servers or roles.");
        bullet("Export to a file, import from the profiles folder.");

        h2("Audit and profiler");
        p("Audit is a journal of everything the mod does. The profiler "
                + "shows timings of hot paths: chat, detection, macros.");
        bullet("Filter the journal by category.");
        bullet("Timings help find slow spots.");

        h2("Database (/stdb)");
        p("Nicks, punishments and audit are stored in SQLite "
                + "(config/stafftools.db).");
        bullet("/stdb list [page] — nick list (clickable).");
        bullet("/stdb search <text> — substring nick search.");
        bullet("/stdb hardsearch <text> — same, explicit.");
        bullet("/stdb status — file size and row counts.");
        bullet("/stdb reload — reload nick cache from DB.");
        bullet("/stdb delete <nick> — remove a nick.");
        bullet("/stdb export — export full DB to CSV (config/).");

        h2("Themes and interface");
        p("12 gradient themes, animations, glow, animation speed and "
                + "the chat frame are set in \"Settings\".");
        bullet("Accent color and nick highlight are selectable.");
        bullet("Animations toggle off with one switch.");
    }

    private void h1(String text) {
        blocks.add(new Block(KIND_H1, text, H1_H + PARA_GAP));
    }

    private void h2(String text) {
        blocks.add(new Block(KIND_H2, text, H2_H + SECTION_GAP));
    }

    private void p(String text) {
        blocks.add(new Block(KIND_P, text, 0));
    }

    private void bullet(String text) {
        blocks.add(new Block(KIND_BULLET, text, LINE_H + BULLET_GAP));
    }

    private void code(String text) {
        blocks.add(new Block(KIND_CODE, text, LINE_H + CODE_PAD * 2 + 4));
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        float step = 18f;
        scroll = Math.clamp(
                scroll + (verticalAmount > 0 ? -step : step),
                0,
                maxScroll()
        );

        return true;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (contentBottom() - contentTop()));
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        TextRenderer tr =
                net.minecraft.client.MinecraftClient.getInstance()
                        .textRenderer;

        int x = contentLeft();
        int w = contentWidth();
        int top = contentTop();
        int bottom = contentBottom();

        scroll = Math.clamp(scroll, 0, maxScroll());

        context.enableScissor(x, top, x + w, bottom);

        int y = top - (int) scroll;

        for (Block block : blocks) {

            int blockH = block.height() > 0
                    ? block.height()
                    : wrappedHeight(tr, block.text(), w - PAD * 2);

            if (y + blockH >= top && y <= bottom) {
                drawBlock(context, tr, block, x, y, w, alpha);
            }

            y += blockH;
        }

        context.disableScissor();

        contentHeight = y + (int) scroll - top;

        if (maxScroll() > 0) {
            Ui.drawScrollbar(
                    context,
                    x + w - 4,
                    top,
                    bottom - top,
                    scroll / (float) maxScroll(),
                    (bottom - top) / (float) (bottom - top + maxScroll()),
                    alpha
            );
        }
    }

    private int wrappedHeight(TextRenderer tr, String text, int maxW) {
        return wrap(tr, text, maxW).size() * LINE_H + PARA_GAP;
    }

    private void drawBlock(
            DrawContext context,
            TextRenderer tr,
            Block block,
            int x,
            int y,
            int w,
            float alpha
    ) {

        switch (block.kind()) {

            case KIND_H1 -> {
                // hero title with a soft accent underline
                context.drawTextWithShadow(
                        tr, Text.literal(block.text()),
                        x + PAD, y + 4,
                        Ui.argb(Ui.TEXT, alpha));

                int underlineY = y + H1_H - 4;
                int lineW = Math.min(
                        w - PAD * 2,
                        tr.getWidth(block.text()) + 20
                );

                Ui.drawRoundRect(
                        context,
                        x + PAD, underlineY, lineW, 2,
                        1, Ui.argb(Ui.ACCENT, alpha));

                Ui.drawRoundRect(
                        context,
                        x + PAD + lineW - 30, underlineY, 30, 2,
                        1, Ui.argb(Ui.ACCENT_2, alpha));
            }

            case KIND_H2 -> {
                // section header: accent bar + tinted background band
                Ui.drawRoundRect(
                        context,
                        x + PAD - 4, y - 2, w - PAD * 2 + 8, H2_H,
                        4, Ui.argb(Ui.mix(Ui.SURFACE, Ui.ACCENT, 0.12f), alpha));

                Ui.drawRoundRect(
                        context,
                        x + PAD - 4, y - 2, 3, H2_H,
                        2, Ui.argb(Ui.ACCENT, alpha));

                context.drawTextWithShadow(
                        tr, Text.literal(block.text()),
                        x + PAD + 6, y + 3,
                        Ui.argb(Ui.ACCENT_SOFT, alpha));
            }

            case KIND_P -> {
                int line = y;
                for (String ln : wrap(tr, block.text(), w - PAD * 2)) {
                    context.drawTextWithShadow(
                            tr, Text.literal(ln),
                            x + PAD, line, Ui.argb(Ui.TEXT, alpha));
                    line += LINE_H;
                }
            }

            case KIND_BULLET -> {
                // colored dot marker + indented wrapped text
                Ui.drawRoundRect(
                        context,
                        x + PAD + 1, y + 4, 4, 4,
                        2, Ui.argb(Ui.ACCENT, alpha));

                int line = y;
                for (String ln : wrap(tr, block.text(), w - PAD * 2 - 14)) {
                    context.drawTextWithShadow(
                            tr, Text.literal(ln),
                            x + PAD + 12, line, Ui.argb(Ui.TEXT_DIM, alpha));
                    line += LINE_H;
                }
            }

            case KIND_CODE -> {
                int boxH = LINE_H + CODE_PAD * 2;

                Ui.drawShadow(
                        context,
                        x + PAD, y, w - PAD * 2, boxH,
                        4, 2, 0.12f * alpha, alpha);

                Ui.drawRoundRect(
                        context,
                        x + PAD, y, w - PAD * 2, boxH,
                        4, Ui.argb(Ui.SURFACE_2, alpha));

                Ui.drawRoundBorder(
                        context,
                        x + PAD, y, w - PAD * 2, boxH,
                        4, Ui.argb(Ui.mix(Ui.BORDER, Ui.SUCCESS, 0.4f), alpha));

                // left accent stripe
                Ui.drawRoundRect(
                        context,
                        x + PAD, y, 3, boxH,
                        2, Ui.argb(Ui.SUCCESS, alpha));

                context.drawTextWithShadow(
                        tr, Text.literal(block.text()),
                        x + PAD + CODE_PAD + 4, y + CODE_PAD,
                        Ui.argb(Ui.SUCCESS, alpha));
            }

            default -> {
            }
        }
    }

    /** Greedy word wrap; long words are hard-split to fit. */
    private List<String> wrap(TextRenderer tr, String text, int maxW) {

        List<String> out = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }

        for (String paragraph : text.split("\n")) {

            if (paragraph.isEmpty()) {
                out.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();

            for (String word : paragraph.split(" ")) {

                String candidate = line.length() == 0
                        ? word
                        : line + " " + word;

                if (tr.getWidth(candidate) <= maxW) {
                    line = new StringBuilder(candidate);
                    continue;
                }

                if (line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder();
                }

                // hard-split a single word that is too wide
                StringBuilder chunk = new StringBuilder();

                for (char c : word.toCharArray()) {

                    if (tr.getWidth(chunk.toString() + c) > maxW
                            && chunk.length() > 0) {

                        out.add(chunk.toString());
                        chunk = new StringBuilder();
                    }

                    chunk.append(c);
                }

                line = chunk;
            }

            if (line.length() > 0) {
                out.add(line.toString());
            }
        }

        return out;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}