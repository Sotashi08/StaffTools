package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.CommandPreview;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.macro.Placeholder;
import com.uwdie.stafftools.client.macro.PlaceholderRegistry;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
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
    private int bodyTop;
    private int panelBottom;

    private int panelX;
    private int panelW;

    private int nameY;
    private int descY;
    private int cmdY;
    private int toggleY;
    private int chipsY;

    // aliases side panel
    private final List<String> workingAliases =
            new java.util.ArrayList<>();

    /** Row height of one alias card. */
    private static final int ALIAS_ROW_H = 16;
    private static final int CHIP_ROW_STEP = 22;

    /** Drag-to-reorder state of the alias list. */
    private final DragReorder aliasDrag = new DragReorder();

    private int aliasScroll;
    private int aliasPanelX;
    private int aliasPanelW = 124;
    private int aliasListTop = 0;
    private int aliasListCapacity;
    private int aliasListVisible;
    private StTextField aliasField;

    // placeholder info panel (right side)
    private static final int INFO_W = 160;
    private int infoPanelW = INFO_W;
    private int infoPanelX;

    private record Chip(
            int x,
            int y,
            int w,
            String tag,
            String tooltip
    ) {
    }

    private final List<Chip> chips =
            new java.util.ArrayList<>();

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

        resetSlots();

        configureShell();
        chips.clear();
        bodyTop = contentTop() + 28;

        int totalWidth = contentWidth();
        int gap = 8;

        if (totalWidth >= 500) {
            aliasPanelW = 124;
            infoPanelW = INFO_W;
        } else if (totalWidth >= 320) {
            aliasPanelW = Math.min(104, totalWidth / 4);
            infoPanelW = 0;
        } else {
            aliasPanelW = Math.min(72, totalWidth / 4);
            infoPanelW = 0;
        }

        panelW = Math.max(
                1,
                totalWidth - aliasPanelW - infoPanelW - gap * 2
        );
        panelX = contentLeft() + aliasPanelW + gap;

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

        nameY = bodyTop + 16;
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
                        Text.literal("\uD83D\uDE00"),
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

        List<Object[]> defs = new java.util.ArrayList<>();

        for (Placeholder placeholder :
                PlaceholderRegistry.getAll()) {

            String tag = "<" + placeholder.name() + ">";

            defs.add(new Object[]{tag, Lang.t(placeholder.description())});

            // <alias> goes right after <player>
            if (placeholder.name().equals("player")) {

                defs.add(new Object[]{
                        "<alias>",
                        Lang.t(Key.MSG_ALIAS_HELP)
                });
            }

            chipW = Math.max(
                    chipW,
                    textRenderer.getWidth(tag) + 10
            );
        }

        chipW = Math.max(
                chipW,
                textRenderer.getWidth("<alias>") + 10
        );
        chipW = Math.min(
                chipW,
                Math.max(1, (inner - 4) / 2)
        );

        int step = chipW + 4;

        int perRow = Math.max(
                1,
                (inner + 4) / step
        );

        int row = 0;
        int col = 0;

        for (Object[] def : defs) {

            String tag = (String) def[0];
            String tooltip = (String) def[1];

            int bx = left + col * step;
            int by = chipsY + row * CHIP_ROW_STEP;

            chips.add(
                    new Chip(
                            bx,
                            by,
                            chipW,
                            tag,
                            tooltip
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

        // aliases side panel
        workingAliases.clear();
        if (editing != null) {
            workingAliases.addAll(editing.getAliases());
        }

        aliasPanelX = contentLeft();

        aliasListTop = bodyTop + 46;
        infoPanelX = panelX + panelW + gap;

        aliasField = new StTextField(
                textRenderer,
                aliasPanelX + 6,
                bodyTop + 22,
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
                        bodyTop + 22,
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

        int chipRows = row + (col > 0 ? 1 : 0);
        int buttonsY = Math.min(
                chipsY + chipRows * CHIP_ROW_STEP + 4,
                contentBottom() - 28
        );

        panelBottom = buttonsY + 26;

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

        editing.setAliases(
                new ArrayList<>(workingAliases)
        );

        boolean confirm = confirmationToggle.isOn();

        editing.setDangerous(confirm);
        editing.setConfirmationRequired(confirm);

        StafftoolsClient.getMacroManager()
                .register(editing);

        com.uwdie.stafftools.client.audit.AuditLog.get()
                .info(
                        com.uwdie.stafftools.client.audit
                                .AuditCategory.CONFIG,
                        "Macro saved: " + editing.getName()
                );

        client.setScreen(
                parent instanceof StafftoolsScreen
                        ? new StafftoolsScreen()
                        : parent
        );
    }

    @Override
    public boolean mouseClicked(
            Click click,
            boolean doubled
    ) {

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (emojiPicker.mouseClicked(mouseX, mouseY)) {
            return true;
        }

        // alias list rows: the cross deletes, the rest of the row
        // starts a drag-to-reorder
        if (button == 0 &&
                isInsideAliasList(mouseX, mouseY)) {

            int index = aliasScroll + (int)
                    ((mouseY - aliasListTop) / ALIAS_ROW_H);

            if (index >= 0 &&
                    index < workingAliases.size()) {

                int rx = (int) mouseX;

                if (rx >= aliasPanelX + aliasPanelW - 22 &&
                        rx <= aliasPanelX + aliasPanelW - 6) {

                    workingAliases.remove(index);
                    aliasScroll = Math.min(
                            aliasScroll,
                            Math.max(
                                    0,
                                    workingAliases.size()
                                            - aliasListCapacity
                            )
                    );

                } else {

                    // anywhere else on the row arms a reorder drag
                    aliasDrag.press(index, mouseY);
                }
            }

            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(
            Click click,
            double offsetX,
            double offsetY
    ) {

        if (aliasDrag.drag(
                click.y(),
                aliasListTop,
                ALIAS_ROW_H,
                workingAliases.size(),
                aliasScroll * ALIAS_ROW_H
        )) {

            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {

        int to = aliasDrag.targetIndex();

        int from = aliasDrag.release();

        if (from >= 0 && to >= 0 &&
                from != to &&
                from < workingAliases.size() &&
                to < workingAliases.size()) {

            String moved = workingAliases.remove(from);

            workingAliases.add(to, moved);

            Ui.playHover();
        }

        return super.mouseReleased(click);
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
                    workingAliases.size() - aliasListCapacity
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
                        + aliasListVisible * ALIAS_ROW_H;
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
                bodyTop - 4,
                panelW,
                panelBottom - bodyTop + 4,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                panelX,
                bodyTop - 4,
                panelW,
                panelBottom - bodyTop + 4,
                alpha
        );

        Ui.drawSectionTitle(
                context,
                textRenderer,
                contentLeft(),
                contentTop(),
                "\u270E",
                Lang.t(titleKey),
                Lang.t(Key.MSG_EDITOR_HINT),
                alpha
        );

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

        String hint = textRenderer.trimToWidth(
                Lang.t(Key.MSG_EMOJI_HINT),
                Math.max(1, inner - 4)
        );
        int hintWidth = textRenderer.getWidth(hint);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(hint),
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

        // aliases side panel (height capped to its content)
        int aliasPanelBottom = Math.min(
                Math.min(panelBottom - 6, contentBottom() - 4),
                aliasListTop + 8 * ALIAS_ROW_H + 12
        );

        Ui.drawPanel(
                context,
                aliasPanelX,
                bodyTop,
                aliasPanelW,
                aliasPanelBottom - bodyTop,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                aliasPanelX,
                bodyTop,
                aliasPanelW,
                aliasPanelBottom - bodyTop,
                alpha
        );

        Ui.drawSectionHeader(
                context,
                textRenderer,
                aliasPanelX + 6,
                bodyTop + 5,
                aliasPanelW - 12,
                Lang.t(Key.LABEL_ALIASES),
                alpha
        );

        int maxVisible = Math.max(
                0,
                (aliasPanelBottom - 12 - aliasListTop)
                        / ALIAS_ROW_H
        );

        aliasListCapacity = maxVisible;
        aliasScroll = Math.clamp(
                aliasScroll,
                0,
                Math.max(0, workingAliases.size() - aliasListCapacity)
        );
        aliasListVisible = Math.min(
                aliasListCapacity,
                workingAliases.size() - aliasScroll
        );

        for (int i = 0; i < aliasListVisible; i++) {

            String alias =
                    workingAliases.get(aliasScroll + i);

            int ry = aliasListTop + i * ALIAS_ROW_H;

            context.fill(
                    aliasPanelX + 4,
                    ry,
                    aliasPanelX + aliasPanelW - 4,
                    ry + ALIAS_ROW_H - 2,
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
                            mouseY < ry + ALIAS_ROW_H - 2;

            if (xHovered) {

                context.fill(
                        aliasPanelX + aliasPanelW - 22,
                        ry,
                        aliasPanelX + aliasPanelW - 6,
                        ry + ALIAS_ROW_H - 2,
                        Ui.argb(0xFF8A2B2B, alpha)
                );
            }

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("\u2715"),
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

        drawSidePanel(
                context,
                hoveredChip,
                alpha
        );
    }

    /**
     * Right-hand panel: the live command preview on top, and the
     * placeholder description underneath while a chip is hovered.
     */
    private void drawSidePanel(
            DrawContext context,
            Chip hoveredChip,
            float alpha
    ) {

        int top = bodyTop - 4;
        if (infoPanelW <= 0) {
            return;
        }

        int bottom = Math.max(top + 60, panelBottom);

        Ui.drawPanel(
                context,
                infoPanelX,
                top,
                infoPanelW,
                bottom - top,
                alpha
        );
        Ui.drawOrbitFrame(
                context,
                infoPanelX,
                top,
                infoPanelW,
                bottom - top,
                alpha
        );

        // accent header strip
        Ui.drawGradientH(
                context,
                infoPanelX + 3,
                top + 1,
                infoPanelW - 6,
                2,
                Ui.ACCENT,
                Ui.ACCENT_2,
                alpha
        );

        int y = top + 10;

        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.PREVIEW_TITLE),
                infoPanelX + 8,
                y,
                Ui.argb(Ui.ACCENT_SOFT, alpha)
        );

        y += 14;

        String command = commandField == null
                ? ""
                : commandField.getText();

        String preview = CommandPreview.resolve(
                command,
                workingAliases.isEmpty()
                        ? null
                        : workingAliases.get(0)
        );

        if (preview.isEmpty()) {

            context.drawTextWithShadow(
                    textRenderer,
                    Lang.text(Key.PREVIEW_EMPTY),
                    infoPanelX + 8,
                    y,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            y += 12;

        } else {

            List<String> lines = wrapAll(
                    textRenderer,
                    preview,
                    infoPanelW - 16,
                    6
            );

            for (String line : lines) {

                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal(line),
                        infoPanelX + 8,
                        y,
                        Ui.argb(Ui.TEXT, alpha)
                );

                y += 10;
            }

            y += 2;
        }

        // example caption
        context.drawTextWithShadow(
                textRenderer,
                Lang.text(Key.PREVIEW_EXAMPLE),
                infoPanelX + 8,
                y,
                Ui.argb(Ui.TEXT_MUTED, alpha * 0.9f)
        );

        y += 12;

        if (hoveredChip != null) {

            Ui.drawHeader(
                    context,
                    infoPanelX + 8,
                    y,
                    infoPanelW - 16,
                    alpha * 0.6f
            );

            y += 6;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(hoveredChip.tag()),
                    infoPanelX + 8,
                    y,
                    Ui.argb(Ui.ACCENT_SOFT, alpha)
            );

            y += 12;

            int maxLines = Math.max(
                    1,
                    (bottom - y - 6) / 11
            );

            for (String line : wrapAll(
                    textRenderer,
                    hoveredChip.tooltip(),
                    infoPanelW - 16,
                    maxLines
            )) {

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
