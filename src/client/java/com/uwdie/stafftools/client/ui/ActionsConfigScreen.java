package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ActionsConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 4;
    private static final int LIST_TOP = 44;
    private static final int LIST_BOTTOM_PAD = 42;

    private final Screen parent;

    private int scroll;
    private boolean needsRebuild;

    private int rowLeft;
    private int rowWidth;
    private int listBottom;

    private final List<Row> rows = new ArrayList<>();

    public ActionsConfigScreen(Screen parent) {

        super(Text.literal("Player Actions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            rebuild();
        }
    }

    private void rebuild() {

        clearChildren();
        rows.clear();

        List<ActionEntry> entries =
                StafftoolsClient.getConfig()
                        .getActionEntries();

        rowWidth = 360;
        rowLeft = (width - rowWidth) / 2;

        int top = LIST_TOP;
        listBottom = height - LIST_BOTTOM_PAD;

        int contentHeight =
                entries.size() * (ROW_HEIGHT + ROW_GAP);

        int maxScroll = Math.max(
                0,
                contentHeight - (listBottom - top)
        );

        if (scroll > maxScroll) {
            scroll = maxScroll;
        }

        int y = top - scroll;

        for (int i = 0; i < entries.size(); i++) {

            if (y + ROW_HEIGHT < top) {
                y += ROW_HEIGHT + ROW_GAP;
                continue;
            }

            if (y > listBottom) {
                break;
            }

            rows.add(new Row(entries.get(i), i, y));
            addRowButtons(entries.get(i), i, y);

            y += ROW_HEIGHT + ROW_GAP;
        }

        int centerX = width / 2;

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_ADD_ACTION),
                        button -> client.setScreen(
                                new ActionEntryEditScreen(
                                        this,
                                        null,
                                        true
                                )
                        )
                ).dimensions(
                        centerX - 105,
                        height - 30,
                        100,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_BACK),
                        button -> client.setScreen(
                                new StafftoolsScreen()
                        )
                ).dimensions(
                        centerX + 5,
                        height - 30,
                        100,
                        20
                ).build()
        );
    }

    private void addRowButtons(
            ActionEntry entry,
            int index,
            int y
    ) {

        int buttonY = y + 5;

        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("▲"),
                        button -> {
                            move(index, -1);
                            needsRebuild = true;
                        }
                ).dimensions(
                        rowLeft + rowWidth - 135,
                        buttonY,
                        20,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("▼"),
                        button -> {
                            move(index, 1);
                            needsRebuild = true;
                        }
                ).dimensions(
                        rowLeft + rowWidth - 112,
                        buttonY,
                        20,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_EDIT),
                        button -> client.setScreen(
                                new ActionEntryEditScreen(
                                        this,
                                        entry,
                                        false
                                )
                        )
                ).dimensions(
                        rowLeft + rowWidth - 88,
                        buttonY,
                        45,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_DELETE),
                        button -> {

                            StafftoolsClient.getConfig()
                                    .getActionEntries()
                                    .remove(entry);

                            StafftoolsClient.saveConfig();

                            needsRebuild = true;
                        }
                ).dimensions(
                        rowLeft + rowWidth - 40,
                        buttonY,
                        36,
                        20
                ).build()
        );
    }

    private void move(int index, int delta) {

        List<ActionEntry> entries =
                StafftoolsClient.getConfig()
                        .getActionEntries();

        int target = index + delta;

        if (target < 0 ||
                target >= entries.size()) {

            return;
        }

        ActionEntry entry =
                entries.remove(index);

        entries.add(target, entry);

        StafftoolsClient.saveConfig();
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        List<ActionEntry> entries =
                StafftoolsClient.getConfig()
                        .getActionEntries();

        int contentHeight =
                entries.size() * (ROW_HEIGHT + ROW_GAP);

        int maxScroll = Math.max(
                0,
                contentHeight - (listBottom - LIST_TOP)
        );

        int delta = verticalAmount > 0 ? -1 : 1;

        scroll = Math.max(
                0,
                Math.min(
                        maxScroll,
                        scroll + delta * (ROW_HEIGHT + ROW_GAP)
                )
        );

        needsRebuild = true;

        return true;
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.BTN_PLAYER_ACTIONS),
                width / 2,
                20,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.MSG_ACTIONS_HINT),
                width / 2,
                31,
                0xAAAAAA
        );

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_ACTIONS),
                    width / 2,
                    LIST_TOP + 20,
                    0x888888
            );
        }

        for (Row row : rows) {

            context.fill(
                    rowLeft,
                    row.y,
                    rowLeft + rowWidth,
                    row.y + ROW_HEIGHT,
                    Ui.PANEL_BG_SOFT
            );

            String icon =
                    row.entry.getIcon() == null
                            ? ""
                            : row.entry.getIcon();

            int nameColor =
                    row.entry.isEnabled()
                            ? 0xFFFFFF
                            : 0x777777;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(icon + " " + row.entry.getLabel()),
                    rowLeft + 6,
                    row.y + 2,
                    nameColor
            );

            String command =
                    row.entry.getCommand();

            if (textRenderer.getWidth(command) >
                    rowWidth - 210) {

                command = textRenderer.trimToWidth(
                        command,
                        rowWidth - 210
                ) + "...";
            }

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(command),
                    rowLeft + 6,
                    row.y + 16,
                    0x888888
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private record Row(
            ActionEntry entry,
            int index,
            int y
    ) {
    }
}
