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

    private int left;
    private int inner;
    private int panelBottom;

    private int panelX;
    private int panelW;

    private int iconY;
    private int labelY;
    private int cmdY;
    private int chipsY;

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

            addDrawableChild(
                    new StButton(
                            left + col * step,
                            chipsY + row * 24,
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

        if (isNew) {

            StafftoolsClient.getConfig()
                    .getActionEntries()
                    .add(target);
        }

        StafftoolsClient.saveConfig();

        client.setScreen(parent);
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

        return super.mouseClicked(mouseX, mouseY, button);
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
