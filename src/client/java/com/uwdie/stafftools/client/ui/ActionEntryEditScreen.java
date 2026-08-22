package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Placeholder;
import com.uwdie.stafftools.client.macro.PlaceholderRegistry;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ActionEntryEditScreen extends StScreen {

    private final Screen parent;
    private final String titleKey;
    private final ActionEntry editing;
    private final boolean isNew;

    private StTextField iconField;
    private StTextField labelField;
    private StTextField commandField;

    private StTextField lastFocused;

    private StToggle enabledToggle;
    private StToggle confirmationToggle;

    private final EmojiPicker emojiPicker =
            new EmojiPicker();

    // aliases side panel
    private final List<String> workingAliases =
            new ArrayList<>();

    private int aliasScroll;
    private int aliasPanelX;
    private int aliasPanelW = 140;
    private int aliasListTop = 84;
    private int aliasListVisible;
    private StTextField aliasField;

    // placeholder info panel (right side)
    private static final int INFO_W = 160;
    private int infoPanelX;

    private int left;
    private int inner;
    private int panelBottom;

    private int panelX;
    private int panelW;

    private int iconY;
    private int labelY;
    private int cmdY;
    private int chipsY;

    private record Chip(
            int x,
            int y,
            int w,
            String tag,
            String tooltip
    ) {
    }

    private final List<Chip> chips =
            new ArrayList<>();

    public ActionEntryEditScreen(
            Screen parent,
            ActionEntry editing,
            boolean isNew
    ) {

        super(Lang.text(
                editing == null
                        ? Key.BTN_ADD_ACTION
                        : Key.BTN_EDIT_ACTION
        ));

        this.titleKey =
                editing == null
                        ? Key.BTN_ADD_ACTION
                        : Key.BTN_EDIT_ACTION;

        this.parent = parent;
        this.editing = editing;
        this.isNew = isNew;
    }

    @Override
    protected void init() {

        panelW = Math.min(formWidth(), 330);
        panelX = (width - panelW) / 2;

        left = panelX + 14;
        inner = panelW - 28;

        boolean wasEnabled =
                editing == null || editing.isEnabled();

        // single confirmation flag (dangerous + confirmationRequired)
        boolean wasConfirmation =
                editing != null &&
                        (editing.isDangerous() ||
                                editing.isConfirmationRequired());

        iconY = 50;
        labelY = iconY + 38;
        cmdY = labelY + 38;

        iconField = field(left, iconY, 60, Key.LABEL_ICON);
        iconField.setMaxLength(8);

        labelField = field(left, labelY, inner, Key.LABEL_ACTION);
        labelField.setMaxLength(32);

        commandField = field(left, cmdY, inner, Key.LABEL_COMMAND);
        commandField.setMaxLength(256);

        lastFocused = commandField;

        if (editing != null) {

            iconField.setText(editing.getIcon());
            labelField.setText(editing.getLabel());
            commandField.setText(editing.getCommand());
        }

        addDrawableChild(
                new StButton(
                        left + 62,
                        iconY,
                        28,
                        20,
                        Text.literal("😀"),
                        button -> toggleEmoji(),
                        nextSlot(),
                        Ui.ACCENT
                )
        );

        chipsY = cmdY + 34;

        int chipW = 70;

        for (Placeholder placeholder :
                PlaceholderRegistry.getAll()) {

            chipW = Math.max(
                    chipW,
                    textRenderer.getWidth(
                            "<" + placeholder.name() + ">"
                    ) + 10
            );
        }

        int step = chipW + 4;

        int perRow = Math.max(
                1,
                (inner + 4) / step
        );

        int row = 0;
        int col = 0;

        for (Placeholder placeholder :
                PlaceholderRegistry.getAll()) {

            String tag =
                    "<" + placeholder.name() + ">";

            int bx = left + col * step;
            int by = chipsY + row * 24;

            chips.add(
                    new Chip(
                            bx,
                            by,
                            chipW,
                            tag,
                            Lang.t(placeholder.description())
                    )
            );

            addDrawableChild(
                    new StButton(
                            bx,
                            by,
                            chipW,
                            20,
                            Text.literal(tag),
                            button -> insertPlaceholder(tag),
                            nextSlot(),
                            Ui.ACCENT
                    )
            );

            col++;

            if (col >= perRow) {
                col = 0;
                row++;
            }
        }

        int toggleY =
                chipsY + (row + 1) * 24 + 8;

        int toggleW = (inner - 4) / 2;

        enabledToggle = toggle(
                left,
                toggleY,
                toggleW,
                20,
                Key.TOGGLE_ENABLED,
                wasEnabled,
                null
        );

        confirmationToggle = toggle(
                left + toggleW + 4,
                toggleY,
                toggleW,
                20,
                Key.TOGGLE_CONFIRMATION,
                wasConfirmation,
                null
        );

        int buttonsY = toggleY + 34;

        panelBottom = buttonsY + 34;

        // aliases side panel
        workingAliases.addAll(
                editing != null
                        ? editing.getAliases()
                        : List.of()
        );

        aliasPanelX = Math.max(
                4,
                panelX - 10 - aliasPanelW
        );

        infoPanelX = Math.min(
                width - INFO_W - 4,
                panelX + panelW + 10
        );

        aliasField = new StTextField(
                textRenderer,
                aliasPanelX + 6,
                56,
                aliasPanelW - 32,
                20,
                Text.empty(),
                nextSlot()
        );

        aliasField.setOnFocusGain(
                () -> lastFocused = aliasField
        );

        addDrawableChild(aliasField);

        addDrawableChild(
                new StButton(
                        aliasPanelX + aliasPanelW - 24,
                        56,
                        18,
                        20,
                        Text.literal("+"),
                        b -> {
                            String text =
                                    aliasField.getText().trim();

                            if (!text.isEmpty()) {
                                workingAliases.add(text);
                                aliasField.setText("");
                            }
                        },
                        nextSlot(),
                        Ui.ACCENT
                )
        );

        button(
                left,
                buttonsY,
                inner / 2 - 2,
                20,
                Key.BTN_SAVE,
                button -> save()
        );

        button(
                left + inner / 2 + 2,
                buttonsY,
                inner / 2 - 2,
                20,
                Key.BTN_CANCEL,
                b -> client.setScreen(
                        parent instanceof StafftoolsScreen
                                ? new StafftoolsScreen()
                                : parent
                )
        );
    }

    private StTextField field(
            int x,
            int y,
            int width,
            String placeholderKey
    ) {

        StTextField widget =
                new StTextField(
                        textRenderer,
                        x,
                        y,
                        width,
                        20,
                        Lang.text(placeholderKey),
                        nextSlot()
                );

        widget.setOnFocusGain(
                () -> lastFocused = widget
        );

        addDrawableChild(widget);

        return widget;
    }

    private void toggleEmoji() {

        if (emojiPicker.isOpen()) {
            emojiPicker.close();
            return;
        }

        emojiPicker.open(
                left + 62,
                iconY,
                this::insertEmoji
        );
    }

    private TextFieldWidget activeField() {
        return lastFocused != null
                ? lastFocused
                : commandField;
    }

    private void insertEmoji(String emoji) {

        activeField().write(emoji);
        activeField().setCursorToEnd(false);
    }

    private void insertPlaceholder(String placeholder) {

        activeField().write(placeholder);
        activeField().setCursorToEnd(false);
    }

    private void save() {

        ActionEntry target = editing;

        if (target == null) {
            target = new ActionEntry();
        }

        target.setIcon(iconField.getText().trim());
        target.setLabel(labelField.getText().trim());
        target.setCommand(commandField.getText().trim());

        target.setEnabled(enabledToggle.isOn());

        boolean confirm = confirmationToggle.isOn();

        target.setDangerous(confirm);
        target.setConfirmationRequired(confirm);

        target.setAliases(
                new ArrayList<>(workingAliases)
        );

        if (isNew) {

            StafftoolsClient.getConfig()
                    .getActionEntries()
                    .add(target);
        }

        StafftoolsClient.saveConfig();

        client.setScreen(
                parent instanceof StafftoolsScreen
                        ? new StafftoolsScreen()
                        : parent
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (emojiPicker.mouseClicked(mouseX, mouseY)) {
            return true;
        }

        if (button == 0 &&
                isInsideAliasList(mouseX, mouseY)) {

            int index = aliasScroll + (int)
                    ((mouseY - aliasListTop) / 16);

            if (index >= 0 &&
                    index < workingAliases.size()) {

                int rx = (int) mouseX;

                if (rx >= aliasPanelX + aliasPanelW - 22 &&
                        rx <= aliasPanelX + aliasPanelW - 6) {

                    workingAliases.remove(index);
                }
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (isInsideAliasList(mouseX, mouseY)) {

            int maxScroll = Math.max(
                    0,
                    workingAliases.size() - aliasListVisible
            );

            aliasScroll = Math.clamp(
                    aliasScroll
                            + (verticalAmount > 0 ? -1 : 1),
                    0,
                    maxScroll
            );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    private boolean isInsideAliasList(
            double mouseX,
            double mouseY
    ) {

        return mouseX >= aliasPanelX &&
                mouseX <= aliasPanelX + aliasPanelW &&
                mouseY >= aliasListTop &&
                mouseY < aliasListTop
                        + aliasListVisible * 16;
    }

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        Ui.drawPanel(
                context,
                panelX,
                34,
                panelW,
                panelBottom - 34,
                alpha
        );

        drawTitle(context, titleKey, alpha);

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_ICON),
                left,
                iconY - 11,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_ACTION),
                left,
                labelY - 11,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_COMMAND),
                left,
                cmdY - 11,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        int hintWidth =
                textRenderer.getWidth(
                        Lang.text(Key.MSG_EMOJI_HINT)
                );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.MSG_EMOJI_HINT),
                left + 96,
                iconY + 5,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_PLACEHOLDERS),
                left,
                chipsY - 12,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        // aliases side panel (height capped to its content)
        int aliasPanelBottom = Math.min(
                panelBottom - 6,
                aliasListTop + 8 * 16 + 12
        );

        Ui.drawPanel(
                context,
                aliasPanelX,
                34,
                aliasPanelW,
                aliasPanelBottom - 34,
                alpha
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_ALIASES),
                aliasPanelX + aliasPanelW / 2,
                40,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        int maxVisible = Math.max(
                0,
                (aliasPanelBottom - 12 - aliasListTop) / 16
        );

        aliasListVisible = Math.min(
                workingAliases.size(),
                maxVisible
        );

        for (int i = 0; i < aliasListVisible; i++) {

            String alias =
                    workingAliases.get(aliasScroll + i);

            int ry = aliasListTop + i * 16;

            context.fill(
                    aliasPanelX + 4,
                    ry,
                    aliasPanelX + aliasPanelW - 4,
                    ry + 14,
                    Ui.argb(Ui.PANEL_BG_SOFT, alpha)
            );

            String shown = textRenderer.trimToWidth(
                    alias,
                    aliasPanelW - 32
            );

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(shown),
                    aliasPanelX + 7,
                    ry + 3,
                    Ui.argb(Ui.TEXT, alpha)
            );

            boolean xHovered =
                    mouseX >= aliasPanelX + aliasPanelW - 22 &&
                            mouseX <= aliasPanelX + aliasPanelW - 6 &&
                            mouseY >= ry &&
                            mouseY < ry + 14;

            if (xHovered) {

                context.fill(
                        aliasPanelX + aliasPanelW - 22,
                        ry,
                        aliasPanelX + aliasPanelW - 6,
                        ry + 14,
                        Ui.argb(0xFF8A2B2B, alpha)
                );
            }

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("✕"),
                    aliasPanelX + aliasPanelW - 14,
                    ry + 2,
                    Ui.argb(
                            xHovered ? 0xFFFFFFFF : Ui.DANGER_SOFT,
                            alpha
                    )
            );
        }

        // placeholder info panel (right side), shown on chip hover
        Chip hoveredChip = getHoveredChip(mouseX, mouseY);

        if (hoveredChip != null) {
            drawInfoPanel(
                    context,
                    hoveredChip,
                    alpha
            );
        }
    }

    private Chip getHoveredChip(
            double mouseX,
            double mouseY
    ) {

        for (Chip chip : chips) {

            if (mouseX >= chip.x() &&
                    mouseX < chip.x() + chip.w() &&
                    mouseY >= chip.y() &&
                    mouseY < chip.y() + 20) {

                return chip;
            }
        }

        return null;
    }

    private void drawInfoPanel(
            DrawContext context,
            Chip chip,
            float alpha
    ) {

        int top = 34;
        int bottom = Math.max(
                top + 60,
                panelBottom
        );

        Ui.drawPanel(
                context,
                infoPanelX,
                top,
                INFO_W,
                bottom - top,
                alpha
        );

        // accent header strip
        Ui.drawGradientH(
                context,
                infoPanelX + 3,
                top + 1,
                INFO_W - 6,
                2,
                Ui.ACCENT,
                Ui.ACCENT_2,
                alpha
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(chip.tag()),
                infoPanelX + 8,
                top + 12,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        int maxLines =
                (bottom - top - 34) / 11;

        List<String> lines = wrapAll(
                textRenderer,
                chip.tooltip(),
                INFO_W - 16,
                maxLines
        );

        int y = top + 30;

        for (String line : lines) {

            if (line.isEmpty()) {

                y += 6;
                continue;
            }

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(line),
                    infoPanelX + 8,
                    y,
                    Ui.argb(Ui.TEXT_DIM, alpha)
            );

            y += 11;
        }
    }

    /** Word wrap without a line cap (panel handles overflow). */
    private static List<String> wrapAll(
            net.minecraft.client.font.TextRenderer tr,
            String text,
            int maxWidth,
            int maxLines
    ) {

        List<String> out = new ArrayList<>();

        for (String para : text.split("\n")) {

            if (para.isEmpty()) {

                out.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();

            for (String word : para.split(" ")) {

                String candidate =
                        current.length() == 0
                                ? word
                                : current + " " + word;

                if (tr.getWidth(candidate) <= maxWidth) {
                    current = new StringBuilder(candidate);
                    continue;
                }

                if (current.length() > 0) {
                    out.add(current.toString());
                    current = new StringBuilder();
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

            if (current.length() > 0) {
                out.add(current.toString());
            }
        }

        if (out.size() > maxLines) {

            out = new ArrayList<>(
                    out.subList(0, maxLines)
            );

            out.set(
                    out.size() - 1,
                    out.get(out.size() - 1) + "..."
            );
        }

        return out;
    }

    @Override
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        emojiPicker.render(context, mouseX, mouseY);
    }
}
