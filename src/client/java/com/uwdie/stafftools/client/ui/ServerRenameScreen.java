package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.ui.widget.StTextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Renames a server of the punishment history. The address itself is
 * shown as a hint and never changes — only the display name does, so the
 * sidebar stays readable without remembering IPs.
 */
public class ServerRenameScreen extends StScreen {

    private static final int LABEL_Y = 58;
    private static final int FIELD_Y = 70;

    private final Screen parent;
    private final String key;

    private StTextField nameField;

    public ServerRenameScreen(
            Screen parent,
            String key
    ) {

        super(Lang.text(Key.TITLE_SERVER_RENAME));

        this.parent = parent;
        this.key = key;
    }

    @Override
    protected void init() {

        resetSlots();

        String current = PunishmentHistory.get()
                .serverName(key);

        String address = key == null ? "" : key;

        nameField = new StTextField(
                textRenderer,
                Math.max(
                        10,
                        (width - Math.min(220, formWidth())) / 2
                ),
                FIELD_Y,
                Math.min(220, formWidth()),
                20,
                Text.literal(current),
                nextSlot()
        );

        nameField.setMaxLength(32);
        nameField.setText(current);

        addDrawableChild(nameField);
        setInitialFocus(nameField);

        int left =
                (width - Math.min(220, formWidth())) / 2;

        int half =
                (Math.min(220, formWidth()) - 8) / 2;

        button(
                left,
                height - 34,
                half,
                22,
                Key.BTN_CANCEL,
                b -> client.setScreen(parent)
        );

        button(
                left + half + 8,
                height - 34,
                half,
                22,
                Key.BTN_SAVE,
                b -> {
                    String value =
                            nameField.getText().trim();

                    if (!value.isEmpty()) {
                        PunishmentHistory.get()
                                .renameServer(key, value);
                    }

                    client.setScreen(parent);
                }
        );

        // the unchangeable address, below the field
        int fieldLeft =
                Math.max(10, (width - Math.min(220, formWidth())) / 2);

        addressX = fieldLeft;
        addressY = FIELD_Y + 24;
        address = address;
    }

    private int addressX;
    private int addressY;
    private String address;

    @Override
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {

        int panelW = Math.min(240, formWidth() + 20);
        int panelX = (width - panelW) / 2;

        Ui.drawPanel(
                context,
                panelX,
                34,
                panelW,
                height - 44,
                alpha
        );

        Ui.drawOrbitFrame(
                context,
                panelX,
                34,
                panelW,
                height - 44,
                alpha
        );

        drawTitle(context, Key.TITLE_SERVER_RENAME, alpha);

        int left =
                (width - Math.min(220, formWidth())) / 2;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(Lang.t(Key.LABEL_SERVER_NAME)),
                left,
                LABEL_Y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(
                        Lang.t(Key.LABEL_SERVER_KEY) + ": " +
                                (address == null || address.isBlank()
                                        ? "—"
                                        : address)
                ),
                addressX,
                addressY,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );
    }
}
