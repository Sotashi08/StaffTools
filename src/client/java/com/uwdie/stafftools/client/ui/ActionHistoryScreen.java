package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import com.uwdie.stafftools.client.punishment.PunishmentStatus;
import com.uwdie.stafftools.client.punishment.PunishmentType;
import com.uwdie.stafftools.client.punishment.ServerProfile;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Punishment log.
 *
 * The main area (summary, filters, table) is centered on the screen and
 * the narrow server list stays pinned to the left edge. The list scrolls
 * on its own, type and status are real dropdown menus instead of cycling
 * chips, and clicking a row opens its details.
 */
public class ActionHistoryScreen extends StScreen {

    /** Rows of the log table. */
    private static final int ROW_H = 16;
    private static final int ROW_GAP = 3;

    /** Narrow server list, pinned to the left edge. */
    private static final int SIDE_X = 8;
    private static final int SIDE_W = 104;
    private static final int SIDE_TOP = 34;
    private static final int SIDE_ROW_H = 15;
    private static final int SIDE_GAP = 2;
    private static final int SIDE_BOTTOM_PAD = 36;

    /** Summary block. */
    private static final int PANEL_Y = 34;
    private static final int STATS_H = 76;
    private static final int STATS_PAD = 10;

    /** Filter bar. */
    private static final int FILTER_Y = 116;
    private static final int FILTER_H = 20;

    /** Table. */
    private static final int TABLE_TOP = 152;
    private static final int HEADER_H = 11;
    private static final int BOTTOM_PAD = 40;

    /** Dropdown menus. */
    private static final int MENU_NONE = 0;
    private static final int MENU_TYPE = 1;
    private static final int MENU_STATUS = 2;

    private static final int ITEM_H = 13;
    private static final int MENU_W = 108;

    /** Sentinel key of the shared journal entry. */
    private static final String ALL_SERVERS = "\0all";

    private final Screen parent;

    private final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm");

    private int scroll;
    private int sideScroll;
    private int tableBottom;
    private int resetX;

    /** Centered main area. */
    private int contentLeft;
    private int contentWidth;

    /** Relative column offsets, resolved against contentWidth. */
    private int colType;
    private int colPlayer;
    private int colStatus;
    private int colServer;

    private String serverKey;

    private PunishmentType typeFilter;
    private PunishmentStatus statusFilter;
    private String nickFilter = "";

    private StTextField nickField;

    private int openMenu = MENU_NONE;
    private int hoverMenuItem = -1;

    private HistoryStats stats;

    public ActionHistoryScreen(Screen parent) {

        super(Lang.text(Key.HISTORY_TITLE));

        this.parent = parent;
    }

    public ActionHistoryScreen(Screen parent, String serverKey) {

        super(Lang.text(Key.HISTORY_TITLE));

        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {

        resetSlots();

        // the sidebar stays on the left, everything else is centered in
        // the space that is left
        int free = width - (SIDE_X + SIDE_W) - 24;

        contentWidth = Math.max(180, Math.min(free, 430));
        contentLeft = (width - contentWidth) / 2;

        colType = 40;
        colPlayer = contentWidth * 38 / 100;
        colStatus = contentWidth * 66 / 100;
        colServer = contentWidth * 84 / 100;

        tableBottom = height - BOTTOM_PAD;

        resetX = contentLeft + contentWidth - 52;

        int fieldX = contentLeft + 204;
        int fieldW = Math.max(60, resetX - fieldX - 6);

        StTextField field = new StTextField(
                textRenderer,
                fieldX,
                FILTER_Y,
                fieldW,
                FILTER_H,
                Text.literal(Lang.t(Key.HIST_FILTER_NICK_HINT)),
                nextSlot()
        );

        field.setMaxLength(32);

        field.setChangedListener(value -> {

            nickFilter = value == null ? "" : value.trim();

            scroll = 0;
        });

        if (!nickFilter.isEmpty()) {
            field.setText(nickFilter);
        }

        nickField = field;

        addDrawableChild(field);

        int half = (contentWidth - 8) / 2;

        button(
                contentLeft,
                height - 32,
                half,
                22,
                Key.BTN_BACK,
                b -> client.setScreen(
                        serverKey == null
                                ? new StafftoolsScreen()
                                : new ActionHistoryScreen(parent)
                )
        );

        button(
                contentLeft + half + 8,
                height - 32,
                contentWidth - half - 8,
                22,
                serverKey == null
                        ? Key.BTN_CLEAR
                        : Key.BTN_HIST_CLEAR_ONE,
                b -> {

                    if (serverKey == null) {
                        PunishmentHistory.get().clear();
                    } else {
                        PunishmentHistory.get()
                                .clearServer(serverKey);
                    }

                    scroll = 0;
                },
                Ui.DANGER
        );
    }

    // ------------------------------------------------------------------
    // scrolling
    // ------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        // the server list scrolls independently of the table
        if (inSidebar(mouseX, mouseY)) {

            sideScroll = clampScroll(
                    sideScroll,
                    sideScrollAmount(),
                    sideMaxScroll()
            );

            return true;
        }

        int step = ROW_H + ROW_GAP;

        scroll = clampScroll(
                scroll,
                verticalAmount > 0 ? -step : step,
                tableMaxScroll()
        );

        return true;
    }

    private static int clampScroll(
            int current,
            int delta,
            int max
    ) {

        return Math.max(0, Math.min(max, current + delta));
    }

    private int sideTop() {

        return SIDE_TOP + 18;
    }

    private int sideBottom() {

        return height - SIDE_BOTTOM_PAD;
    }

    private int sideViewport() {

        return sideBottom() - sideTop();
    }

    private int sideScrollAmount() {

        return SIDE_ROW_H + SIDE_GAP;
    }

    private int sideMaxScroll() {

        int rows = sidebarRows().size();

        return Math.max(
                0,
                rows * sideScrollAmount() - sideViewport()
        );
    }

    private int tableMaxScroll() {

        int contentHeight = visible().size() * (ROW_H + ROW_GAP);

        return Math.max(
                0,
                contentHeight - (tableBottom - TABLE_TOP)
        );
    }

    private boolean inSidebar(
            double mouseX,
            double mouseY
    ) {

        return mouseX >= SIDE_X &&
                mouseX < SIDE_X + SIDE_W &&
                mouseY >= SIDE_TOP &&
                mouseY <= height - SIDE_BOTTOM_PAD;
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        double mouseX = click.x();
        double mouseY = click.y();

        if (click.button() == 0) {

            // an open menu eats the click, no matter where it is
            if (openMenu != MENU_NONE) {

                if (menuClick(mouseX, mouseY)) {
                    return true;
                }

                openMenu = MENU_NONE;
                hoverMenuItem = -1;

                // fall through: the click that closes the menu may also
                // be a click on the chip itself
            }

            if (clickChip(mouseX, mouseY)) {
                return true;
            }

            if (clickReset(mouseX, mouseY)) {
                return true;
            }

            if (inSidebar(mouseX, mouseY)) {

                if (clickSidebar(mouseX, mouseY)) {
                    return true;
                }
            }

            PunishmentRecord record =
                    recordAt(mouseX, mouseY);

            if (record != null) {

                client.setScreen(
                        new PunishmentDetailScreen(
                                this,
                                record,
                                serverKey
                        )
                );

                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    // ------------------------------------------------------------------
    // sidebar
    // ------------------------------------------------------------------

    /** One selectable sidebar row. */
    private record ServerPick(String key, int button) {
    }

    private record ServerRow(
            String key,
            String label,
            int count,
            boolean all,
            boolean selected
    ) {
    }

    private List<ServerRow> sidebarRows() {

        PunishmentHistory history = PunishmentHistory.get();

        List<ServerRow> rows = new ArrayList<>();

        rows.add(new ServerRow(
                ALL_SERVERS,
                Lang.t(Key.HIST_ALL_SERVERS),
                history.getRecords().size(),
                true,
                serverKey == null
        ));

        for (ServerProfile profile : history.getServers()) {

            rows.add(new ServerRow(
                    profile.getKey(),
                    profile.label(),
                    history.getRecordsOf(
                            profile.getKey()
                    ).size(),
                    false,
                    profile.getKey().equals(serverKey)
            ));
        }

        return rows;
    }

    /** Row under the cursor plus which part of it was hit. */
    private ServerPick sidebarRowAt(
            double mouseX,
            double mouseY
    ) {

        if (!inSidebar(mouseX, mouseY) || mouseY < sideTop()) {
            return null;
        }

        List<ServerRow> rows = sidebarRows();

        int y = sideTop() - sideScroll;

        for (ServerRow row : rows) {

            if (mouseY >= y && mouseY < y + SIDE_ROW_H) {

                if (row.selected() && !row.all()) {

                    int actionsX = SIDE_X + SIDE_W - 26;

                    if (mouseX >= actionsX + 13) {
                        return new ServerPick(row.key(), 2);
                    }

                    if (mouseX >= actionsX) {
                        return new ServerPick(row.key(), 1);
                    }
                }

                return new ServerPick(row.key(), 0);
            }

            y += sideScrollAmount();
        }

        return null;
    }

    private boolean clickSidebar(
            double mouseX,
            double mouseY
    ) {

        ServerPick pick = sidebarRowAt(mouseX, mouseY);

        if (pick == null) {
            return false;
        }

        if (pick.button() == 1) {

            client.setScreen(
                    new ServerRenameScreen(this, pick.key())
            );

            return true;
        }

        if (pick.button() == 2) {

            PunishmentHistory.get().clearServer(pick.key());

            return true;
        }

        if (pick.key().equals(ALL_SERVERS)) {

            if (serverKey != null) {
                serverKey = null;
                scroll = 0;
            }

            return true;
        }

        serverKey = pick.key();
        scroll = 0;

        // the footer names the active view, so the widgets are rebuilt
        init();

        return true;
    }

    // ------------------------------------------------------------------
    // dropdown menus
    // ------------------------------------------------------------------

    private int typeMenuX() {
        return contentLeft;
    }

    private int statusMenuX() {
        return contentLeft + 104;
    }

    private boolean chipHover(
            double mouseX,
            double mouseY,
            int x
    ) {

        return mouseX >= x &&
                mouseX < x + 96 &&
                mouseY >= FILTER_Y &&
                mouseY <= FILTER_Y + FILTER_H;
    }

    private boolean clickChip(
            double mouseX,
            double mouseY
    ) {

        if (openMenu != MENU_NONE) {
            return false;
        }

        if (!inFilterBar(mouseX, mouseY)) {
            return false;
        }

        if (chipHover(mouseX, mouseY, typeMenuX())) {

            openMenu = MENU_TYPE;

            return true;
        }

        if (chipHover(mouseX, mouseY, statusMenuX())) {

            openMenu = MENU_STATUS;

            return true;
        }

        return false;
    }

    private boolean inFilterBar(
            double mouseX,
            double mouseY
    ) {

        return mouseY >= FILTER_Y &&
                mouseY <= FILTER_Y + FILTER_H &&
                mouseX >= contentLeft &&
                mouseX <= contentLeft + contentWidth;
    }

    /** The reset button drops all three filters at once. */
    private boolean clickReset(
            double mouseX,
            double mouseY
    ) {

        if (!inFilterBar(mouseX, mouseY)) {
            return false;
        }

        if (mouseX < resetX || mouseX > resetX + 52) {
            return false;
        }

        typeFilter = null;
        statusFilter = null;
        nickFilter = "";

        if (nickField != null) {
            nickField.setText("");
        }

        scroll = 0;

        return true;
    }

    /** Geometry of the open menu; {@code null} when closed. */
    private int[] menuBox() {

        int x = openMenu == MENU_TYPE
                ? typeMenuX()
                : statusMenuX();

        int items = openMenu == MENU_TYPE
                ? PunishmentType.values().length + 1
                : PunishmentStatus.values().length + 1;

        int y = FILTER_Y + FILTER_H + 1;
        int h = items * ITEM_H + 4;

        return new int[]{x, y, MENU_W, h};
    }

    private int menuItemAt(
            double mouseX,
            double mouseY
    ) {

        if (openMenu == MENU_NONE) {
            return -1;
        }

        int[] box = menuBox();

        if (mouseX < box[0] ||
                mouseX > box[0] + box[2] ||
                mouseY < box[1] + 2 ||
                mouseY > box[1] + box[3]) {

            return -1;
        }

        int index = (int) ((mouseY - box[1] - 2) / ITEM_H);

        int items = openMenu == MENU_TYPE
                ? PunishmentType.values().length + 1
                : PunishmentStatus.values().length + 1;

        return index >= 0 && index < items ? index : -1;
    }

    private boolean menuClick(
            double mouseX,
            double mouseY
    ) {

        int index = menuItemAt(mouseX, mouseY);

        if (index < 0) {
            return false;
        }

        if (openMenu == MENU_TYPE) {

            typeFilter = index == 0
                    ? null
                    : PunishmentType.values()[index - 1];

        } else {

            statusFilter = index == 0
                    ? null
                    : PunishmentStatus.values()[index - 1];
        }

        openMenu = MENU_NONE;
        hoverMenuItem = -1;

        scroll = 0;

        return true;
    }

    /** Menu item captions, index 0 is "all". */
    private String[] menuLabels() {

        if (openMenu == MENU_TYPE) {

            PunishmentType[] values = PunishmentType.values();

            String[] labels = new String[values.length + 1];

            labels[0] = Lang.t(Key.HIST_FILTER_ALL);

            for (int i = 0; i < values.length; i++) {
                labels[i + 1] = values[i].getIcon() + " " +
                        values[i].getLabel();
            }

            return labels;
        }

        PunishmentStatus[] values = PunishmentStatus.values();

        String[] labels = new String[values.length + 1];

        labels[0] = Lang.t(Key.HIST_FILTER_ALL);

        for (int i = 0; i < values.length; i++) {
            labels[i + 1] = statusText(values[i]);
        }

        return labels;
    }

    /** Index of the selected item of the open menu. */
    private int menuSelectedIndex() {

        if (openMenu == MENU_TYPE) {

            return typeFilter == null
                    ? 0
                    : typeFilter.ordinal() + 1;
        }

        return statusFilter == null
                ? 0
                : statusFilter.ordinal() + 1;
    }

    private int menuItemColor(int index) {

        if (index == 0) {
            return Ui.TEXT_DIM;
        }

        // the filter may be "off" while the menu is open, so the
        // selection has to be compared through menuSelectedIndex()
        boolean selected = index == menuSelectedIndex();

        if (openMenu == MENU_TYPE) {

            return selected
                    ? Ui.ACCENT_SOFT
                    : Ui.TEXT;
        }

        return selected
                ? statusColor(PunishmentStatus.values()[index - 1])
                : Ui.TEXT;
    }

    // ------------------------------------------------------------------
    // data
    // ------------------------------------------------------------------

    private List<PunishmentRecord> visible() {

        List<PunishmentRecord> records =
                serverKey == null
                        ? PunishmentHistory.get().getRecords()
                        : PunishmentHistory.get()
                        .getRecordsOf(serverKey);

        String needle = nickFilter.toLowerCase();

        return records.stream()
                .filter(record -> typeFilter == null ||
                        record.getType() == typeFilter)
                .filter(record -> statusFilter == null ||
                        record.getEffectiveStatus() == statusFilter)
                .filter(record -> needle.isEmpty() ||
                        record.getPlayerName() != null &&
                                record.getPlayerName()
                                        .toLowerCase()
                                        .contains(needle))
                .toList();
    }

    private PunishmentRecord recordAt(
            double mouseX,
            double mouseY
    ) {

        if (mouseX < contentLeft ||
                mouseX > contentLeft + contentWidth ||
                mouseY < TABLE_TOP ||
                mouseY > tableBottom) {

            return null;
        }

        List<PunishmentRecord> records = visible();

        int y = TABLE_TOP - scroll;

        for (int i = 0; i < records.size(); i++) {

            if (mouseY >= y && mouseY < y + ROW_H) {
                return records.get(i);
            }

            y += ROW_H + ROW_GAP;
        }

        return null;
    }

    private static String statusText(PunishmentStatus status) {

        return switch (status) {

            case DONE -> Lang.t(Key.PUN_STATUS_DONE);
            case FAILED -> Lang.t(Key.PUN_STATUS_FAILED);
            case NO_RESPONSE -> Lang.t(Key.PUN_STATUS_NO_RESPONSE);
            case PENDING -> Lang.t(Key.PUN_STATUS_PENDING);
        };
    }

    private static int statusColor(PunishmentStatus status) {

        return switch (status) {

            case DONE -> Ui.SUCCESS;
            case FAILED -> Ui.DANGER;
            case NO_RESPONSE -> Ui.TEXT_DIM;
            case PENDING -> Ui.WARNING;
        };
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        drawTitle(context, Key.HISTORY_TITLE, alpha);

        stats = HistoryStats.of(visible());

        drawSidebar(context, mouseX, mouseY, alpha);

        drawStats(context, alpha);

        drawFilterBar(context, mouseX, mouseY, alpha);

        drawTable(context, mouseX, mouseY, alpha);
    }

    @Override
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        // the open menu floats above the table, so it is drawn last
        if (openMenu == MENU_NONE) {
            return;
        }

        hoverMenuItem = menuItemAt(mouseX, mouseY);

        int[] box = menuBox();

        context.getMatrices().pushMatrix();

        Ui.drawPanel(
                context,
                box[0],
                box[1],
                box[2],
                box[3],
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                box[0],
                box[1],
                box[2],
                box[3],
                alpha
        );

        String[] labels = menuLabels();
        int selected = menuSelectedIndex();

        for (int i = 0; i < labels.length; i++) {

            int y = box[1] + 2 + i * ITEM_H;

            if (i == selected || i == hoverMenuItem) {

                context.fill(
                        box[0] + 2,
                        y,
                        box[0] + box[2] - 2,
                        y + ITEM_H,
                        Ui.argb(
                                i == selected
                                        ? 0x403F8AE0
                                        : 0x22FFFFFF,
                                alpha
                        )
                );
            }

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            textRenderer.trimToWidth(
                                    labels[i],
                                    box[2] - 12
                            )
                    ),
                    box[0] + 6,
                    y + 2,
                    Ui.argb(menuItemColor(i), alpha)
            );
        }

        context.getMatrices().popMatrix();
    }

    private void drawSidebar(
            DrawContext context,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        int top = SIDE_TOP;
        int h = height - SIDE_BOTTOM_PAD - top;

        Ui.drawPanel(context, SIDE_X, top, SIDE_W, h, alpha);

        Ui.drawOrbitFrame(context, SIDE_X, top, SIDE_W, h, alpha);

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.HIST_SERVERS),
                SIDE_X + 6,
                top + 5,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.fill(
                SIDE_X + 5,
                top + 15,
                SIDE_X + SIDE_W - 5,
                top + 16,
                Ui.argb(0x2C3B55, alpha)
        );

        ServerPick hovered = sidebarRowAt(mouseX, mouseY);

        int y = sideTop() - sideScroll;

        for (ServerRow row : sidebarRows()) {

            if (y + SIDE_ROW_H >= sideTop() &&
                    y <= sideBottom()) {

                drawServerRow(
                        context,
                        row,
                        y,
                        hovered,
                        mouseX,
                        alpha
                );
            }

            y += sideScrollAmount();
        }

        drawSideScrollbar(context, alpha);
    }

    private void drawServerRow(
            DrawContext context,
            ServerRow row,
            int y,
            ServerPick hovered,
            int mouseX,
            float alpha
    ) {

        boolean over = hovered != null &&
                hovered.key().equals(row.key());

        int fill = row.selected()
                ? Ui.argb(0x30ACC2F0, alpha)
                : over
                        ? Ui.argb(0x1AFFFFFF, alpha)
                        : 0;

        if (fill != 0) {

            context.fill(
                    SIDE_X + 3,
                    y,
                    SIDE_X + SIDE_W - 3,
                    y + SIDE_ROW_H,
                    fill
            );
        }

        if (row.selected()) {

            context.fill(
                    SIDE_X + 3,
                    y,
                    SIDE_X + 5,
                    y + SIDE_ROW_H,
                    Ui.argb(Ui.ACCENT, alpha)
            );
        }

        String label = row.label();

        // the action buttons need their space
        int maxText = row.selected() && !row.all()
                ? SIDE_W - 46
                : SIDE_W - 34;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(label, maxText)
                ),
                SIDE_X + 8,
                y + 3,
                Ui.argb(
                        row.selected()
                                ? Ui.TEXT
                                : row.all()
                                        ? Ui.ACCENT_SOFT
                                        : Ui.TEXT_DIM,
                        alpha
                )
        );

        if (row.selected() && !row.all()) {

            int actionsX = SIDE_X + SIDE_W - 26;

            boolean editOver = over && hovered.button() == 1;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("✎"),
                    actionsX + 2,
                    y + 3,
                    Ui.argb(editOver ? Ui.ACCENT : Ui.TEXT_MUTED, alpha)
            );

            boolean clearOver = over && hovered.button() == 2;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("✕"),
                    actionsX + 15,
                    y + 3,
                    Ui.argb(
                            clearOver ? Ui.DANGER : Ui.TEXT_MUTED,
                            alpha
                    )
            );

        } else {

            String count = Integer.toString(row.count());

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(count),
                    SIDE_X + SIDE_W - 8 - textRenderer.getWidth(count),
                    y + 3,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    /** Thin scrollbar of the server list. */
    private void drawSideScrollbar(
            DrawContext context,
            float alpha
    ) {

        int max = sideMaxScroll();

        if (max <= 0) {
            return;
        }

        int trackH = sideViewport();
        int thumbH = Math.max(
                16,
                trackH * trackH / (trackH + max)
        );

        int travel = trackH - thumbH;
        int thumbY = sideTop() + travel * sideScroll / max;

        context.fill(
                SIDE_X + SIDE_W - 3,
                sideTop(),
                SIDE_X + SIDE_W - 2,
                sideBottom(),
                Ui.argb(0x18FFFFFF, alpha)
        );

        context.fill(
                SIDE_X + SIDE_W - 3,
                thumbY,
                SIDE_X + SIDE_W - 2,
                thumbY + thumbH,
                Ui.argb(Ui.ACCENT_SOFT, alpha * 0.9f)
        );
    }

    private void drawStats(
            DrawContext context,
            float alpha
    ) {

        Ui.drawPanel(
                context,
                contentLeft,
                PANEL_Y,
                contentWidth,
                STATS_H,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                contentLeft,
                PANEL_Y,
                contentWidth,
                STATS_H,
                alpha
        );

        int cardW = (contentWidth - STATS_PAD * 2 - 3 * 5) / 4;

        drawCard(context, STATS_PAD, Key.STAT_TOTAL,
                stats.getTotal(), Ui.ACCENT_SOFT, alpha);
        drawCard(context, STATS_PAD + (cardW + 5), Key.STAT_CONFIRMED,
                stats.getConfirmed(), Ui.SUCCESS, alpha);
        drawCard(context, STATS_PAD + (cardW + 5) * 2, Key.STAT_FAILED,
                stats.getFailed(), Ui.DANGER, alpha);
        drawCard(context, STATS_PAD + (cardW + 5) * 3, Key.STAT_NO_RESPONSE,
                stats.getUnanswered(), Ui.TEXT_DIM, alpha);

        int x = contentLeft + STATS_PAD;
        int lineY = PANEL_Y + 40;


        drawLine(context, x, lineY, alpha,

                Lang.t(Key.STAT_PLAYERS) + ": "
                        + stats.getUniquePlayers(),

                Lang.t(Key.STAT_TOP_PLAYER) + ": "
                        + (stats.getTopPlayer() == null
                        ? Lang.t(Key.DETAIL_NONE)
                        : stats.topPlayerLabel())
        );

        drawLine(context, x, lineY + 11, alpha,

                Lang.t(Key.STAT_MODERATORS) + ": "
                        + stats.getModerators(),

                Lang.t(Key.STAT_FROM_MACROS) + ": "
                        + stats.getFromMacros() + "  ·  "
                        + Lang.t(Key.STAT_FROM_ACTIONS) + ": "
                        + stats.getFromActions()
        );

        String breakdown = stats.typeBreakdown();

        if (breakdown != null) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(
                            Lang.t(Key.STAT_BY_TYPE) + ": " + breakdown
                    ),
                    x,
                    lineY + 22,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );
        }
    }

    /** Two facts on one line, the second one right-aligned. */
    private void drawLine(
            DrawContext context,
            int x,
            int y,
            float alpha,
            String left,
            String right
    ) {

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(
                                left,
                                contentWidth - STATS_PAD * 2
                        )
                ),
                x,
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        int maxRight = contentWidth / 2;

        String trimmed = textRenderer.trimToWidth(right, maxRight);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(trimmed),
                contentLeft + contentWidth - STATS_PAD
                        - textRenderer.getWidth(trimmed),
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );
    }

    private void drawCard(
            DrawContext context,
            int offset,
            String captionKey,
            int value,
            int color,
            float alpha
    ) {

        int x = contentLeft + offset;
        int y = PANEL_Y + 8;

        int w = (contentWidth - STATS_PAD * 2 - 3 * 5) / 4;

        context.fill(
                x,
                y,
                x + w,
                y + 26,
                Ui.argb(0x16FFFFFF, alpha)
        );

        Ui.drawRoundBorder(
                context,
                x,
                y,
                w,
                26,
                3,
                Ui.argb(color, alpha * 0.5f)
        );

        String caption = Lang.t(captionKey);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(caption),
                x + w / 2 - textRenderer.getWidth(caption) / 2,
                y + 3,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        String text = Integer.toString(value);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(text),
                x + w / 2 - textRenderer.getWidth(text) / 2,
                y + 14,
                Ui.argb(color, alpha)
        );
    }

    private void drawFilterBar(
            DrawContext context,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        drawChip(
                context,
                typeMenuX(),
                Lang.t(Key.HIST_FILTER_TYPE) + ": " + typeLabel(),
                typeFilter == null ? Ui.TEXT_DIM : Ui.ACCENT_SOFT,
                mouseX,
                mouseY,
                alpha
        );

        drawChip(
                context,
                statusMenuX(),
                Lang.t(Key.HIST_FILTER_STATUS) + ": " + statusLabel(),
                statusFilter == null ? Ui.TEXT_DIM : statusColor(statusFilter),
                mouseX,
                mouseY,
                alpha
        );

        // reset button at the right edge of the bar
        int rx = resetX;

        boolean over = mouseX >= rx && mouseX < rx + 52 &&
                mouseY >= FILTER_Y &&
                mouseY <= FILTER_Y + FILTER_H;

        boolean dirty =
                typeFilter != null ||
                        statusFilter != null ||
                        !nickFilter.isEmpty();

        context.fill(
                rx,
                FILTER_Y,
                rx + 52,
                FILTER_Y + FILTER_H,
                Ui.argb(
                        dirty
                                ? over ? 0x50FF5555 : 0x28FF5555
                                : over ? 0x20FFFFFF : 0x00000000,
                        alpha
                )
        );

        Ui.drawRoundBorder(
                context,
                rx,
                FILTER_Y,
                52,
                FILTER_H,
                3,
                Ui.argb(
                        dirty
                                ? over ? Ui.DANGER : 0x66FF5555
                                : over ? Ui.ACCENT : 0x33FFFFFF,
                        alpha * 0.9f
                )
        );

        String reset = Lang.t(Key.HIST_FILTER_RESET);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(reset, 44)
                ),
                rx + (52 - textRenderer.getWidth(
                        textRenderer.trimToWidth(reset, 44))) / 2,
                FILTER_Y + 6,
                Ui.argb(dirty ? Ui.DANGER : Ui.TEXT_MUTED, alpha)
        );
    }

    private void drawChip(
            DrawContext context,
            int x,
            String label,
            int color,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        boolean hovered = chipHover(mouseX, mouseY, x);
        boolean open = openMenu != MENU_NONE &&
                (openMenu == MENU_TYPE && x == typeMenuX() ||
                        openMenu == MENU_STATUS && x == statusMenuX());

        context.fill(
                x,
                FILTER_Y,
                x + 96,
                FILTER_Y + FILTER_H,
                Ui.argb(
                        open ? 0x50ACC2F0 : hovered ? 0x30ACC2F0 : 0x22000000,
                        alpha
                )
        );

        Ui.drawRoundBorder(
                context,
                x,
                FILTER_Y,
                96,
                FILTER_H,
                3,
                Ui.argb(
                        open ? Ui.ACCENT : hovered ? Ui.ACCENT : 0xFF2C3B55,
                        alpha * 0.9f
                )
        );

        String trimmed = textRenderer.trimToWidth(label, 76);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(trimmed),
                x + 6,
                FILTER_Y + 6,
                Ui.argb(color, alpha)
        );

        // little caret that shows the menu opens downwards
        int caretX = x + 96 - 9;

        for (int i = 0; i < 3; i++) {

            context.fill(
                    caretX - i,
                    FILTER_Y + 8 + i,
                    caretX + i + 1,
                    FILTER_Y + 9 + i,
                    Ui.argb(open ? Ui.ACCENT : Ui.TEXT_MUTED, alpha)
            );
        }
    }

    private String typeLabel() {

        return typeFilter == null
                ? Lang.t(Key.HIST_FILTER_ALL)
                : typeFilter.getIcon() + " " +
                        typeFilter.getLabel();
    }

    private String statusLabel() {

        return statusFilter == null
                ? Lang.t(Key.HIST_FILTER_ALL)
                : statusText(statusFilter);
    }

    private void drawTable(
            DrawContext context,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        drawTableHeader(context, alpha);

        List<PunishmentRecord> records = visible();

        if (records.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_HISTORY),
                    contentLeft + contentWidth / 2,
                    TABLE_TOP + 22,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        int y = TABLE_TOP - scroll;

        for (PunishmentRecord record : records) {

            if (y + ROW_H >= TABLE_TOP && y <= tableBottom) {

                drawRecord(
                        context,
                        record,
                        y,
                        mouseX,
                        mouseY,
                        alpha
                );
            }

            y += ROW_H + ROW_GAP;
        }

        drawTableScrollbar(context, alpha);
    }

    private void drawTableHeader(
            DrawContext context,
            float alpha
    ) {

        int y = TABLE_TOP - HEADER_H;

        context.fill(
                contentLeft,
                y + 8,
                contentLeft + contentWidth,
                y + 9,
                Ui.argb(0x2C3B55, alpha)
        );

        header(context, Key.HIST_COL_TIME, 0, y, contentWidth * 30 / 100, alpha);
        header(context, Key.HIST_COL_ACTION, colType, y, colPlayer - colType - 6, alpha);
        header(context, Key.HIST_COL_PLAYER, colPlayer, y, colStatus - colPlayer - 6, alpha);
        header(context, Key.HIST_COL_STATUS, colStatus, y, colServer - colStatus - 6, alpha);
        header(context, Key.HIST_COL_SERVER, colServer, y, contentWidth - colServer, alpha);
    }

    private void header(
            DrawContext context,
            String key,
            int offset,
            int y,
            int maxWidth,
            float alpha
    ) {

        String caption = Lang.t(key);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(caption, maxWidth)
                ),
                contentLeft + offset,
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );
    }

    private void drawRecord(
            DrawContext context,
            PunishmentRecord record,
            int y,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        boolean hovered =
                mouseX >= contentLeft &&
                        mouseX <= contentLeft + contentWidth &&
                        mouseY >= y &&
                        mouseY <= y + ROW_H;

        if (hovered) {

            context.fill(
                    contentLeft,
                    y,
                    contentLeft + contentWidth,
                    y + ROW_H,
                    Ui.argb(0x2A2C3A4A, alpha)
            );

        } else {

            Ui.drawRoundRect(
                    context,
                    contentLeft,
                    y,
                    contentWidth,
                    ROW_H,
                    2,
                    Ui.argb(0x12000000, alpha)
            );
        }

        String time =
                TIME_FORMAT.format(record.getTimestamp());

        String type =
                record.getType().getIcon() + " " +
                        record.getType().getLabel();

        String player =
                record.getPlayerName() == null
                        ? "?"
                        : record.getPlayerName();

        PunishmentStatus status =
                record.getEffectiveStatus();

        String server =
                PunishmentHistory.get()
                        .serverName(record.getServerKey());

        int y2 = y + 4;

        cell(context, time, 0, y2,
                contentWidth * 30 / 100, Ui.TEXT_MUTED, alpha);

        cell(context, type, colType, y2,
                colPlayer - colType - 6, Ui.ACCENT_SOFT, alpha);

        cell(context, player, colPlayer, y2,
                colStatus - colPlayer - 6, Ui.TEXT, alpha);

        cell(context, statusText(status), colStatus, y2,
                colServer - colStatus - 6, statusColor(status), alpha);

        cell(context, server, colServer, y2,
                contentWidth - colServer, Ui.TEXT_MUTED, alpha);
    }

    private void cell(
            DrawContext context,
            String text,
            int offset,
            int y,
            int maxWidth,
            int color,
            float alpha
    ) {

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        textRenderer.trimToWidth(text, maxWidth)
                ),
                contentLeft + offset,
                y,
                Ui.argb(color, alpha)
        );
    }

    private void drawTableScrollbar(
            DrawContext context,
            float alpha
    ) {

        int max = tableMaxScroll();

        if (max <= 0) {
            return;
        }

        int trackH = tableBottom - TABLE_TOP;
        int thumbH = Math.max(16, trackH * trackH / (trackH + max));

        int travel = trackH - thumbH;
        int thumbY = TABLE_TOP + travel * scroll / max;

        context.fill(
                contentLeft + contentWidth + 1,
                TABLE_TOP,
                contentLeft + contentWidth + 2,
                tableBottom,
                Ui.argb(0x18FFFFFF, alpha)
        );

        context.fill(
                contentLeft + contentWidth + 1,
                thumbY,
                contentLeft + contentWidth + 2,
                thumbY + thumbH,
                Ui.argb(Ui.ACCENT_SOFT, alpha * 0.9f)
        );
    }
}
