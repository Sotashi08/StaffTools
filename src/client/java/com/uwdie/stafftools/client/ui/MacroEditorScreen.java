package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.macro.Placeholder;
import com.uwdie.stafftools.client.macro.PlaceholderRegistry;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class MacroEditorScreen extends StScreen {

    private final Screen parent;
    private final String titleKey;

    private Macro editing;

    private StTextField nameField;
    private StTextField descriptionField;
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

    private int nameY;
    private int descY;
    private int cmdY;
    private int toggleY;
    private int chipsY;

    public MacroEditorScreen(
            Screen parent,
            Macro editing
    ) {

        super(Lang.text(
                editing == null
                        ? Key.BTN_CREATE_MACRO
                        : Key.BTN_EDIT_MACRO
        ));

        this.titleKey =
                editing == null
                        ? Key.BTN_CREATE_MACRO
                        : Key.BTN_EDIT_MACRO;

        this.parent = parent;
        this.editing = editing;
    }

    @Override
    protected void init() {

        panelW = Math.min(formWidth(), 330);
        panelX = (width - panelW) / 2;

        left = panelX + 14;
        inner = panelW - 28;

        boolean wasEnabled =
                editing == null || editing.isEnabled();

        // "dangerous" and "confirmationRequired" are now a single
        // confirmation flag; keep both model fields in sync
        boolean wasConfirmation =
                editing != null &&
                        (editing.isDangerous() ||
                                editing.isConfirmationRequired());

        nameY = 50;
        descY = nameY + 38;
        cmdY = descY + 38;

        nameField = field(left, nameY, inner, Key.LABEL_NAME);
        nameField.setMaxLength(64);

        descriptionField = field(left, descY, inner, Key.LABEL_DESCRIPTION);
        descriptionField.setMaxLength(256);

        commandField = field(left, cmdY, inner - 28, Key.LABEL_COMMAND);
        commandField.setMaxLength(256);

        lastFocused = commandField;

        if (editing != null) {

            nameField.setText(editing.getName());
            descriptionField.setText(editing.getDescription());

            if (!editing.getCommands().isEmpty()) {
                commandField.setText(
                        editing.getCommands().get(0)
                );
            }
        }

        addDrawableChild(
                new StButton(
                        left + inner - 24,
                        cmdY,
                        24,
                        20,
                        Text.literal("😀"),
                        button -> toggleEmoji(),
                        nextSlot(),
                        Ui.ACCENT
                )
        );

        toggleY = cmdY + 34;
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

        chipsY = toggleY + 32;

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

        int buttonsY =
                chipsY + (row + 1) * 24 + 6;

        panelBottom = buttonsY + 34;

        button(
                left,
                buttonsY,
                inner / 2 - 2,
                20,
                Key.BTN_SAVE,
                b -> save()
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
                left + inner - 24,
                cmdY,
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

        String name =
                nameField.getText().trim();

        String description =
                descriptionField.getText().trim();

        String command =
                commandField.getText().trim();

        if (name.isEmpty()) {
            name = Lang.t(Key.MSG_UNNAMED);
        }

        if (editing == null) {

            editing = new Macro(
                    UUID.randomUUID(),
                    name,
                    description,
                    List.of(command)
            );

        } else {

            editing.setName(name);
            editing.setDescription(description);
        }

        editing.setCommands(List.of(command));
        editing.setEnabled(enabledToggle.isOn());

        boolean confirm = confirmationToggle.isOn();

        editing.setDangerous(confirm);
        editing.setConfirmationRequired(confirm);

        StafftoolsClient.getMacroManager()
                .register(editing);

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
                Lang.text(Key.LABEL_NAME),
                left,
                nameY - 11,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.LABEL_DESCRIPTION),
                left,
                descY - 11,
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
                left + inner - hintWidth,
                toggleY - 12,
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
