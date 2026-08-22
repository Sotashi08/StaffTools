package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MacroListScreen extends Screen {

    private static final int ROW_HEIGHT = 38;
    private static final int ROW_GAP = 4;
    private static final int LIST_TOP = 42;
    private static final int LIST_BOTTOM_PAD = 42;

    private final Screen parent;

    private int scroll;
    private boolean needsRebuild;

    private int rowLeft;
    private int rowWidth;
    private int listBottom;

    private final List<Row> rows =
            new ArrayList<>();

    public MacroListScreen(Screen parent) {
        super(Text.literal("Macros"));

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

        List<Macro> macros =
                StafftoolsClient.getMacroManager().getMacros();

        rowWidth = 340;
        rowLeft = (width - rowWidth) / 2;

        int top = LIST_TOP;
        listBottom = height - LIST_BOTTOM_PAD;

        int contentHeight =
                macros.size() * (ROW_HEIGHT + ROW_GAP);

        int maxScroll = Math.max(
                0,
                contentHeight - (listBottom - top)
        );

        if (scroll > maxScroll) {
            scroll = maxScroll;
        }

        int y = top - scroll;

        for (Macro macro : macros) {

            if (y + ROW_HEIGHT < top) {
                y += ROW_HEIGHT + ROW_GAP;
                continue;
            }

            if (y > listBottom) {
                break;
            }

            rows.add(new Row(macro, y));
            addRowButtons(macro, y);

            y += ROW_HEIGHT + ROW_GAP;
        }

        int centerX = width / 2;

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_CREATE_MACRO),
                        button ->
                                client.setScreen(
                                        new MacroEditorScreen(
                                                this,
                                                null
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
                        button ->
                                client.setScreen(
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
            Macro macro,
            int y
    ) {

        int buttonY = y + 8;

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(
                                macro.isEnabled()
                                        ? Key.BTN_ON
                                        : Key.BTN_OFF
                        ),
                        button -> {
                            macro.setEnabled(
                                    !macro.isEnabled()
                            );
                            StafftoolsClient.saveConfig();
                            needsRebuild = true;
                        }
                ).dimensions(
                        rowLeft + rowWidth - 165,
                        buttonY,
                        50,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_EDIT),
                        button ->
                                client.setScreen(
                                        new MacroEditorScreen(
                                                this,
                                                macro
                                        )
                                )
                ).dimensions(
                        rowLeft + rowWidth - 110,
                        buttonY,
                        55,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Lang.text(Key.BTN_DELETE),
                        button -> {
                            StafftoolsClient.getMacroManager()
                                    .remove(macro.getId());
                            needsRebuild = true;
                        }
                ).dimensions(
                        rowLeft + rowWidth - 50,
                        buttonY,
                        45,
                        20
                ).build()
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        List<Macro> macros =
                StafftoolsClient.getMacroManager().getMacros();

        int contentHeight =
                macros.size() * (ROW_HEIGHT + ROW_GAP);

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

        renderBackground(
                context,
                mouseX,
                mouseY,
                delta
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.BTN_MACROS),
                width / 2,
                20,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.MSG_SELECT_MACRO),
                width / 2,
                31,
                0xAAAAAA
        );

        if (rows.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Lang.text(Key.MSG_NO_MACROS),
                    width / 2,
                    LIST_TOP + 20,
                    0x888888
            );
        }

        for (Row row : rows) {

            drawRow(
                    context,
                    row
            );
        }

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );
    }

    private void drawRow(
            DrawContext context,
            Row row
    ) {

        Macro macro = row.macro;

        boolean hovered =
                row.y >= LIST_TOP &&
                        row.y + ROW_HEIGHT <= listBottom;

        context.fill(
                rowLeft,
                row.y,
                rowLeft + rowWidth,
                row.y + ROW_HEIGHT,
                hovered ? 0x22000000 : 0x18000000
        );

        int nameColor =
                macro.isEnabled()
                        ? 0xFFFFFF
                        : 0x777777;

        String name = macro.getName();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(name),
                rowLeft + 8,
                row.y + 4,
                nameColor
        );

        int badgeX =
                rowLeft + 8 +
                        textRenderer.getWidth(name) +
                        6;

        if (macro.isDangerous() ||
                macro.isConfirmationRequired()) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("⚠ confirm"),
                    badgeX,
                    row.y + 4,
                    0xFF5555
            );
        }

        String description =
                macro.getDescription();

        if (description == null ||
                description.isEmpty()) {

            description = Lang.t(Key.MSG_NO_DESCRIPTION);
        }

        int descWidth = rowWidth - 210;

        List<String> descLines = wrapText(
                textRenderer,
                description,
                descWidth,
                2
        );

        for (int i = 0; i < descLines.size(); i++) {

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(descLines.get(i)),
                    rowLeft + 8,
                    row.y + 16 + i * 10,
                    0x888888
            );
        }
    }

    /** Word-aware wrap; hard-trims overlong words, caps line count. */
    private static List<String> wrapText(
            net.minecraft.client.font.TextRenderer tr,
            String text,
            int maxWidth,
            int maxLines
    ) {

        List<String> lines = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {

            if (lines.size() >= maxLines) {
                break;
            }

            String candidate =
                    current.length() == 0
                            ? word
                            : current + " " + word;

            if (tr.getWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }

            if (lines.size() >= maxLines) {
                break;
            }

            while (tr.getWidth(word) > maxWidth
                    && word.length() > 1) {

                word = word.substring(
                        0,
                        word.length() - 1
                );
            }

            current = new StringBuilder(word);
        }

        if (current.length() > 0
                && lines.size() < maxLines) {

            lines.add(current.toString());
        }

        // ellipsis when content was cut off
        int totalWords = text.split(" ").length;

        boolean truncated =
                lines.size() == maxLines &&
                        totalWords > String.join(
                                " ", lines
                        ).split(" ").length;

        if (truncated && !lines.isEmpty()) {

            int lastIdx = lines.size() - 1;

            String last = lines.get(lastIdx);

            last = tr.trimToWidth(
                    last + "...",
                    maxWidth
            );

            lines.set(lastIdx, last);
        }

        return lines;
    }

    private record Row(
            Macro macro,
            int y
    ) {
    }
}
