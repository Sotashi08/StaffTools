package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.ActionEntry;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;
import com.uwdie.stafftools.client.macro.Macro;
import com.uwdie.stafftools.client.macro.MacroExecutor;
import com.uwdie.stafftools.client.mixin.ChatScreenAccessor;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * In-chat popup with player actions. Drawn on top of the current screen
 * (after the chat text), near the cursor, without closing the chat.
 * Has two quick buttons ("Copy name" / "Duplicate to chat"), can be
 * dragged by the top-right grip and resized by the bottom-right corner.
 */
public final class PlayerActionOverlay {

    public static final PlayerActionOverlay INSTANCE =
            new PlayerActionOverlay();

    private static final int PAD = 8;
    private static final int HEADER_H = 34;
    private static final int FIXED_H = 22;
    private static final int FIXED_GAP = 4;
    private static final int FOOTER_H = 14;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;
    private static final int SEP_H = 18;
    private static final int GRIP_SIZE = 16;
    private static final int RESIZE_SIZE = 14;
    private static final int MIN_W = StaffToolsConfig.MIN_POPUP_WIDTH;
    private static final int MIN_H = StaffToolsConfig.MIN_POPUP_HEIGHT;
    private static final long CONFIRM_MS = 3000L;

    // fixed quick buttons
    private static final int BTN_COPY = 1;
    private static final int BTN_DUP = 2;

    // smooth scroll tuning: spring + rubber band at the ends
    private static final float SCROLL_STIFF = 240f;
    private static final float SCROLL_DAMP = 19f;
    private static final float SCROLL_RUBBER = 14f;
    private static final float EDGE_STIFF = 420f;
    private static final float EDGE_DAMP = 30f;
    private static final int FADE_H = 14;
    private static final int BAR_W = 3;
    private static final long BAR_IDLE_MS = 900L;

    // alias modal geometry
    private static final int ALIAS_HEADER_H = 28;
    private static final int ALIAS_ROW_H = 20;
    private static final int ALIAS_OPT_H = 15;

    private boolean open;
    private PlayerContext player;
    private Screen ownerScreen;
    private double anchorX;
    private double anchorY;
    private float scroll;
    private float scrollTarget;
    private float scrollVel;
    private int maxScroll;
    private long lastScrollAt;
    private float barAlpha;
    private float barPos;
    private float barLen;
    private long openTime;
    private long confirmAt;
    private RowItem confirmItem;

    // closing animation
    private boolean closing;
    private long closeAt;

    // "just executed" flash on a row
    private RowItem flashItem;
    private long flashTime;

    private int lastHoverRow = -1;

    // alias selection modal state
    private boolean aliasOpen;
    private Macro aliasMacro;
    private ActionEntry aliasEntry;
    private PlayerContext aliasTarget;
    private List<String> aliasList = new ArrayList<>();
    private int aliasScroll;
    private long aliasOpenTime;
    private int aliasGX;
    private int aliasGY;
    private int aliasGW;
    private int aliasGH;

    // reason input modal state
    private boolean reasonOpen;
    private Macro reasonMacro;
    private ActionEntry reasonEntry;
    private PlayerContext reasonTarget;
    private TextFieldWidget reasonField;
    private long reasonOpenTime;
    private int reasonGX;
    private int reasonGY;
    private int reasonGW;
    private int reasonGH;
    /** Guards against an accidental Enter right after the modal opens. */
    private long reasonArmedAt;

    private boolean manuallyPlaced;
    private boolean dragging;
    private boolean resizing;
    private double grabDX;
    private double grabDY;

    private final List<RowItem> items = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int contentTop;
    private int contentBottom;

    private PlayerActionOverlay() {
    }

    public boolean isOpen() {
        return open;
    }

    public void open(PlayerContext target) {

        if (target == null) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        this.player = target;
        this.open = true;
        this.closing = false;
        this.ownerScreen = client.currentScreen;
        this.scroll = 0;
        this.scrollTarget = 0;
        this.scrollVel = 0;
        this.lastScrollAt = System.currentTimeMillis();
        this.barAlpha = 0;
        this.barPos = 0;
        this.barLen = 0;
        this.confirmItem = null;
        this.confirmAt = 0L;
        this.manuallyPlaced = false;
        this.dragging = false;
        this.resizing = false;
        this.openTime = System.currentTimeMillis();
        this.flashItem = null;

        UiSound.open();

        // user size last (default when the "save size" toggle is off)
        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        this.panelW = config.isSavePopupSize()
                ? config.getPopupWidth()
                : StaffToolsConfig.DEFAULT_POPUP_WIDTH;

        this.panelH = config.isSavePopupSize()
                ? config.getPopupHeight()
                : StaffToolsConfig.DEFAULT_POPUP_HEIGHT;

        this.anchorX = client.mouse.getX();
        this.anchorY = client.mouse.getY();

        buildItems();
        layout();
    }

    /**
     * User-initiated close: plays the fade-out animation first.
     * delayMs allows showing the "just executed" flash before closing.
     */
    public void requestClose(long delayMs) {

        if (!open || closing) {
            return;
        }

        closing = true;
        closeAt = System.currentTimeMillis() + delayMs;

        confirmItem = null;
        confirmAt = 0L;

        if (delayMs <= 0) {
            clearAliasState();
            UiSound.close();
        }
    }

    private void finishClose() {

        this.open = false;
        this.closing = false;
        this.player = null;
        this.ownerScreen = null;
        this.confirmItem = null;
        this.confirmAt = 0L;
        this.dragging = false;
        this.resizing = false;
        this.flashItem = null;

        clearAliasState();
        clearReasonState();

        this.items.clear();
        this.rows.clear();
    }

    private void clearAliasState() {

        aliasOpen = false;
        aliasMacro = null;
        aliasEntry = null;
        aliasTarget = null;
        aliasScroll = 0;

        if (aliasList != null) {
            aliasList.clear();
        }
    }

    private void clearReasonState() {

        reasonOpen = false;
        reasonMacro = null;
        reasonEntry = null;
        reasonTarget = null;
        reasonField = null;
        reasonArmedAt = 0L;
    }

    /** Opens the centered reason-input modal for a macro/action row. */
    private void openReasonModal(RowItem item) {

        MinecraftClient client = MinecraftClient.getInstance();

        reasonMacro = item.macro;
        reasonEntry = item.entry;
        reasonTarget = player;

        int fieldW = 220;
        int fieldH = 20;

        reasonField = new TextFieldWidget(
                client.textRenderer,
                0, 0, fieldW, fieldH,
                Lang.text(Key.REASON_PLACEHOLDER)
        );

        reasonField.setMaxLength(Integer.MAX_VALUE);
        reasonField.setDrawsBackground(false);
        reasonField.setEditableColor(0xFFFFFFFF);
        reasonField.setUneditableColor(0xFFAAAAAA);

        reasonOpenTime = System.currentTimeMillis();
        // Enter only arms after a short delay, so a stray keypress that
        // opened the modal cannot instantly confirm an empty reason.
        reasonArmedAt = reasonOpenTime + 350L;
        reasonOpen = true;

        reasonField.setFocused(true);

        UiSound.open();
    }

    /** Validates and runs the command with the typed reason substituted. */
    private void confirmReason() {

        String reason = reasonField == null
                ? ""
                : reasonField.getText();

        Macro macro = reasonMacro;
        ActionEntry entry = reasonEntry;
        PlayerContext target = reasonTarget;

        clearReasonState();

        if (macro != null) {

            MacroExecutor.executeWithReason(macro, target, reason);

            Toasts.INSTANCE.success(
                    Lang.t(Key.TOAST_EXECUTED, macro.getName())
            );

        } else if (entry != null) {

            MacroExecutor.executeActionWithReason(
                    entry.getCommand(),
                    target,
                    reason,
                    entry.getLabel()
            );

            Toasts.INSTANCE.success(
                    Lang.t(Key.TOAST_EXECUTED, entry.getLabel())
            );
        }

        UiSound.confirm();

        requestClose(0);
    }

    /**
     * ESC handling: closes the alias modal first, then the popup.
     * Returns true if something was closed.
     */
    public boolean handleEscape() {

        if (reasonOpen) {
            clearReasonState();
            return true;
        }

        if (aliasOpen) {
            clearAliasState();
            return true;
        }

        if (open) {
            requestClose(0);
            return true;
        }

        return false;
    }

    /**
     * Keyboard input for the reason modal. Returns true when the key was
     * consumed. Enter confirms only after the arming delay, so a stray
     * keypress that opened the modal cannot submit an empty reason.
     *
     * <p>Since 1.21.11 there is no {@code charTyped}: printable input
     * arrives here as a key event with keycode 0, so it is forwarded to
     * the field as a character.
     */
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {

        if (!reasonOpen || reasonField == null) {
            return false;
        }

        int keyCode = input.key();

        if (keyCode == GLFW.GLFW_KEY_ENTER ||
                keyCode == GLFW.GLFW_KEY_KP_ENTER) {

            if (System.currentTimeMillis() >= reasonArmedAt) {
                confirmReason();
            }

            return true;
        }

        boolean handled = reasonField.keyPressed(input);

        // printable character typed (keycode 0 carries the char)
        if (keyCode == 0) {
            handled |= reasonField.charTyped(
                    new net.minecraft.client.input.CharInput(
                            input.scancode(), input.modifiers()));
        }

        return handled;
    }

    /** Character input routed from the screen while the modal is open. */
    public boolean charTyped(net.minecraft.client.input.CharInput input) {

        if (!reasonOpen || reasonField == null) {
            return false;
        }

        return reasonField.charTyped(input);
    }

    private void buildItems() {

        items.clear();

        for (ActionEntry entry :
                StafftoolsClient.getConfig()
                        .getActionEntries()) {

            if (!entry.isEnabled() || entry.isCopyName()) {
                continue;
            }

            items.add(new RowItem(entry));
        }

        boolean macroHeaderAdded = false;

        for (Macro macro :
                StafftoolsClient.getMacroManager()
                        .getMacros()) {

            if (!macro.isEnabled()) {
                continue;
            }

            if (!macroHeaderAdded) {

                items.add(
                        RowItem.separator(
                                Lang.t(Key.OVERLAY_CUSTOM_MACROS)
                        )
                );

                macroHeaderAdded = true;
            }

            items.add(new RowItem(macro));
        }
    }

    private void layout() {

        rows.clear();

        int cursorY = PAD + HEADER_H + FIXED_H;

        for (RowItem item : items) {

            int h = item.separator ? SEP_H : ROW_H;

            rows.add(new Row(cursorY, h, item));

            cursorY += h + ROW_GAP;
        }

        cursorY -= ROW_GAP;

        contentTop = PAD + HEADER_H + FIXED_H;

        MinecraftClient client =
                MinecraftClient.getInstance();

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        // user-set size, clamped so the frame never leaves the screen
        // and never becomes smaller than its own header + one row
        panelW = Math.clamp(
                panelW,
                MIN_W,
                Math.max(MIN_W, screenWidth - 8)
        );

        panelH = Math.clamp(
                panelH,
                MIN_H,
                Math.max(MIN_H, screenHeight - 8)
        );

        contentBottom = Math.max(
                contentTop,
                panelH - PAD - FOOTER_H
        );

        int contentH = cursorY + PAD + FOOTER_H;

        maxScroll = Math.max(
                0,
                contentH - panelH
        );

        // target may sit a bit past the edge: the spring plus the
        // rubber-band force settles it back onto the bound
        scrollTarget = Math.clamp(
                scrollTarget,
                -SCROLL_RUBBER,
                maxScroll + SCROLL_RUBBER
        );

        if (!manuallyPlaced) {

            panelX = (int) Math.clamp(anchorX,
                    15, screenWidth - panelW - 4);

            panelY = (int) Math.clamp(anchorY,
                    4, screenHeight - panelH - 4);

        } else {

            panelX = Math.clamp(panelX,
                    4, screenWidth - panelW - 4);

            panelY = Math.clamp(panelY,
                    4, screenHeight - panelH - 4);
        }
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        return mouseX >= panelX &&
                mouseX <= panelX + panelW &&
                mouseY >= panelY &&
                mouseY <= panelY + panelH;
    }

    /**
     * The whole header strip is the drag handle: grabbing the panel by
     * its title bar is what every desktop window does, and it removes
     * the need for a separate grip button in the corner.
     */
    private boolean inGrip(double mouseX, double mouseY) {
        return mouseX >= panelX &&
                mouseX <= panelX + panelW &&
                mouseY >= panelY &&
                mouseY <= panelY + PAD + HEADER_H;
    }

    private boolean inResize(double mouseX, double mouseY) {
        return mouseX >= panelX + panelW - RESIZE_SIZE &&
                mouseX <= panelX + panelW &&
                mouseY >= panelY + panelH - RESIZE_SIZE &&
                mouseY <= panelY + panelH;
    }

    /**
     * Quick-button strip under the header: two equal halves.
     * Returns {@link #BTN_COPY}, {@link #BTN_DUP} or 0.
     */
    private int inFixedRow(double mouseX, double mouseY) {

        if (!insidePanel(mouseX, mouseY)) {
            return 0;
        }

        int top = PAD + HEADER_H;

        if (mouseY < panelY + top ||
                mouseY >= panelY + top + FIXED_H) {

            return 0;
        }

        int half = (panelW - PAD * 2 - FIXED_GAP) / 2;

        return mouseX < panelX + PAD + half
                ? BTN_COPY
                : BTN_DUP;
    }

    private int hitRow(double mouseY) {

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            if (row.item.separator) {
                continue;
            }

            int absY = Math.round(row.offset - scroll);

            if (absY < contentTop - ROW_H ||
                    absY > contentBottom) {

                continue;
            }

            if (mouseY >= panelY + absY &&
                    mouseY < panelY + absY + ROW_H) {

                return i;
            }
        }

        return -1;
    }

    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (!open || player == null) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.currentScreen == null ||
                client.currentScreen != ownerScreen) {

            finishClose();
            return;
        }

        long now = System.currentTimeMillis();

        float t = Ui.entranceProgress(openTime, 0L, 180L);

        float scale = Ui.animationsEnabled()
                ? 0.92f + 0.08f * t
                : 1f;

        float alpha = Ui.entranceProgress(openTime, 0L, 160L);

        // closing animation: fade out and slide up
        int riseY = 0;

        if (closing) {

            long passed = now - closeAt;

            if (passed < 0) {
                // delayed close (flash window) — draw normally
            } else {

                if (!Ui.animationsEnabled()) {
                    finishClose();
                    return;
                }

                float rawClose = Ui.animationProgress(closeAt, 160L);

                if (rawClose >= 1f) {
                    finishClose();
                    return;
                }

                float close = Ui.easeInCubic(rawClose);
                alpha *= 1f - close;
                scale *= 1f - 0.03f * close;
                riseY = -(int) (8f * Ui.easeOutCubic(rawClose));
            }
        }

        if (confirmItem != null &&
                now - confirmAt >= CONFIRM_MS) {

            confirmItem = null;
            confirmAt = 0L;
        }

        if (aliasOpen) {

            drawAliasModal(
                    context,
                    mouseX,
                    mouseY
            );

            return;
        }

        if (reasonOpen) {

            drawReasonModal(
                    context,
                    mouseX,
                    mouseY,
                    delta
            );

            return;
        }

        layout();

        // smooth "sticky" scroll: springy settle with a slight
        // overshoot and a rubber band at the ends
        updateScroll(delta);

        var matrices =
                context.getMatrices();

        matrices.pushMatrix();

        matrices.translate(
                panelX,
                panelY + riseY
        );

        matrices.scale(
                scale,
                scale
        );

        Ui.drawPopupFrame(
                context,
                0,
                0,
                panelW,
                panelH,
                alpha
        );

        TextRenderer tr =
                client.textRenderer;

        // every label is clamped to the CURRENT panel width, so a long
        // macro name can never spill outside the frame
        int headerAvail = panelW - PAD * 2 - 26;
        int rowAvail = panelW - PAD * 2 - 10;

        // header: avatar square, nick and the punishment counter
        int headSize = 20;

        // the target's own skin head, so the popup shows who it acts on
        drawPlayerHead(context, PAD, PAD + 1, headSize, alpha);

        if (player.name() != null) {

            context.drawTextWithShadow(
                    tr,
                    Text.literal(
                            tr.trimToWidth(
                                    player.name(),
                                    headerAvail
                            )
                    ),
                    PAD + headSize + 6,
                    PAD + 2,
                    Ui.argb(Ui.TEXT, alpha)
            );
        }

        context.drawTextWithShadow(
                tr,
                Text.literal(
                        tr.trimToWidth(
                                Lang.t(Key.OVERLAY_NP,
                                        punishmentSummary(player.name())),
                                headerAvail
                        )
                ),
                PAD + headSize + 6,
                PAD + 13,
                Ui.argb(Ui.TEXT_MUTED, alpha)
        );

        drawFixedRows(
                context,
                tr,
                mouseX,
                mouseY,
                alpha
        );

        // hairline between the quick buttons and the scrolling list
        Ui.drawDivider(
                context,
                PAD,
                PAD + HEADER_H + FIXED_H,
                panelW - PAD * 2,
                alpha
        );

        // clip the scrolling list so rows never overlap
        // the header, copy row or footer.
        // Since 1.21.11 the scissor rect is transformed by the current
        // matrix, so the bounds are given in panel-local coordinates
        // (not screen ones).
        int clipTop = PAD + HEADER_H + FIXED_H;
        int clipBottom = panelH - PAD - FOOTER_H;

        if (clipBottom > clipTop) {

            context.enableScissor(
                    0,
                    clipTop,
                    panelW,
                    clipBottom
            );

            drawBody(
                    context,
                    tr,
                    mouseX,
                    mouseY,
                    alpha
            );

            drawScrollMasks(
                    context,
                    alpha
            );

            context.disableScissor();

        } else {

            drawBody(
                    context,
                    tr,
                    mouseX,
                    mouseY,
                    alpha
            );
        }

        drawScrollbar(
                context,
                alpha
        );

        drawFooter(
                context,
                tr,
                alpha
        );

        drawGrip(
                context,
                tr,
                mouseX,
                mouseY,
                alpha
        );

        drawResizeHandle(
                context,
                tr,
                mouseX,
                mouseY,
                alpha
        );

        matrices.popMatrix();
    }

    /**
     * Draws the target player's head from their tab-list skin, falling
     * back to a neutral accent square when the skin is not available
     * (offline player, or no world loaded).
     */
    private void drawPlayerHead(
            DrawContext context,
            int x,
            int y,
            int size,
            float alpha
    ) {

        var handler = MinecraftClient.getInstance()
                .getNetworkHandler();

        var entry = handler == null || player == null
                ? null
                : handler.getPlayerListEntry(player.uuid());

        if (entry == null) {

            Ui.drawRoundRect(
                    context, x, y, size, size,
                    Design.RADIUS_SM,
                    Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.30f), alpha)
            );

            return;
        }

        var skin = entry.getSkinTextures().body().texturePath();

        int color = Ui.argb(0xFFFFFFFF, alpha);

        // front face of the head, with the hat layer on top
        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin,
                x, y,
                8.0F, 8.0F,
                size, size,
                64, 64,
                color
        );

        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin,
                x, y,
                40.0F, 8.0F,
                size, size,
                64, 64,
                color
        );
    }

    /**
     * Total punishment count for the target, taken from the local history.
     */
    private String punishmentSummary(String name) {

        int total = 0;

        for (PunishmentRecord record :
                PunishmentHistory.get().getRecords()) {

            String recordPlayer = record.getPlayerName();

            if (recordPlayer == null ||
                    !recordPlayer.equalsIgnoreCase(name)) {

                continue;
            }

            total++;
        }

        return Integer.toString(total);
    }

    /**
     * Spring integration for the list scroll. Slightly underdamped, so
     * every notch settles with a small overshoot ("sticky" feel), and a
     * strong restoring force keeps the content from flying past the ends
     * (rubber band).
     */
    private void updateScroll(float delta) {

        if (!Ui.animationsEnabled()) {
            scroll = Math.clamp(scrollTarget, 0f, maxScroll);
            scrollVel = 0f;
            updateScrollbar(delta);
            return;
        }

        float step = Ui.clamp01(delta) * 0.05f
                * Ui.animationSpeed();
        float target = Math.clamp(
                scrollTarget,
                -SCROLL_RUBBER,
                maxScroll + SCROLL_RUBBER
        );

        for (int i = 0; i < 3; i++) {

            float s = step / 3f;

            float accel =
                    (target - scroll) * SCROLL_STIFF
                            - scrollVel * SCROLL_DAMP;

            float over = scroll < 0f
                    ? scroll
                    : scroll > maxScroll
                    ? scroll - maxScroll
                    : 0f;

            if (over != 0f) {
                accel -= over * EDGE_STIFF;
                accel -= scrollVel * EDGE_DAMP;
            }

            scrollVel += accel * s;
            scroll += scrollVel * s;
        }

        if (Math.abs(scroll - target) < 0.05f &&
                Math.abs(scrollVel) < 0.05f) {

            scroll = target;
            scrollVel = 0f;
        }

        updateScrollbar(delta);
    }

    /** Slim scrollbar: thumb follows the list, fades out when idle. */
    private void updateScrollbar(float delta) {

        float trackH = contentBottom - contentTop;

        boolean active =
                maxScroll > 0 &&
                        (Math.abs(scrollVel) > 25f ||
                                System.currentTimeMillis()
                                        - lastScrollAt < BAR_IDLE_MS);

        // fade in fast, drift out slowly
        float rate = active ? 11f : 3.5f;

        barAlpha = Ui.animateTowards(
                barAlpha,
                active ? 1f : 0f,
                delta,
                rate
        );

        float thumbH = trackH <= 0f
                ? 0f
                : Math.max(
                        16f,
                        trackH * trackH / (trackH + maxScroll)
                );

        float travel = Math.max(0f, trackH - thumbH);
        float t = maxScroll <= 0f
                ? 0f
                : Ui.clamp01(scroll / maxScroll);

        barLen = Ui.animateTowards(
                barLen,
                thumbH,
                delta,
                12f
        );

        barPos = Ui.animateTowards(
                barPos,
                t * travel,
                delta,
                9f
        );
    }

    /**
     * Soft fades above/below the list, only while there is content
     * hidden that way — the "sticky" mask of a scrolling region.
     */
    private void drawScrollMasks(
            DrawContext context,
            float alpha
    ) {

        float top = Ui.clamp01(scroll / FADE_H);
        float bottom = Ui.clamp01((maxScroll - scroll) / FADE_H);

        if (top > 0.01f) {

            context.fillGradient(
                    PAD,
                    contentTop,
                    panelW - PAD,
                    contentTop + FADE_H,
                    Ui.argb(Ui.PANEL_BG, alpha * top),
                    Ui.argb(Ui.PANEL_BG, 0f)
            );
        }

        if (bottom > 0.01f) {

            context.fillGradient(
                    PAD,
                    contentBottom - FADE_H,
                    panelW - PAD,
                    contentBottom,
                    Ui.argb(Ui.PANEL_BG, 0f),
                    Ui.argb(Ui.PANEL_BG, alpha * bottom)
            );
        }
    }

    private void drawScrollbar(
            DrawContext context,
            float alpha
    ) {

        if (barAlpha <= 0.01f || barLen < 4f) {
            return;
        }

        int x = panelW - PAD - BAR_W;
        int y = contentTop + 2 + Math.round(barPos);

        Ui.drawRoundRect(
                context,
                x,
                y,
                BAR_W,
                Math.round(barLen),
                2,
                Ui.argb(
                        Ui.mix(Ui.ACCENT_2, Ui.TEXT, 0.12f),
                        alpha * barAlpha * 0.85f
                )
        );
    }

    /**
     * Centered, non-draggable alias picker with accent styling.
     * Rows slide in left-to-right one after another. Click on an
     * option executes the macro; ESC or outside click cancels.
     */
    private void drawAliasModal(
            DrawContext context,
            double mouseX,
            double mouseY
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        TextRenderer tr = client.textRenderer;

        int screenWidth =
                client.getWindow().getScaledWidth();

        int screenHeight =
                client.getWindow().getScaledHeight();

        long now = System.currentTimeMillis();

        float slide = Ui.entranceProgress(aliasOpenTime, 0L, 220L);

        float modalAlpha = Ui.entranceProgress(aliasOpenTime, 0L, 160L);

        if (modalAlpha <= 0.01f) {
            return;
        }

        aliasGW = 180;

        int pad = 6;

        int visible = Math.min(
                aliasList.size(),
                8
        );

        aliasGH = ALIAS_HEADER_H + visible * ALIAS_ROW_H + pad;

        // whole window slides in from the left
        int offsetX =
                (int) ((1f - slide) * -28);

        aliasGX = (screenWidth - aliasGW) / 2 + offsetX;
        aliasGY = (screenHeight - aliasGH) / 2;

        Ui.drawPopupFrame(
                context,
                aliasGX,
                aliasGY,
                aliasGW,
                aliasGH,
                modalAlpha
        );

        context.drawCenteredTextWithShadow(
                tr,
                Lang.text(Key.OVERLAY_ALIAS_TITLE),
                aliasGX + aliasGW / 2,
                aliasGY + pad + 4,
                Ui.argb(Ui.TEXT, modalAlpha)
        );

        if (aliasList.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    tr,
                    Lang.text(Key.MSG_NO_ACTIONS),
                    aliasGX + aliasGW / 2,
                    aliasGY + ALIAS_HEADER_H,
                    Ui.argb(Ui.TEXT_MUTED, modalAlpha)
            );

            return;
        }

        for (int i = 0; i < visible; i++) {

            int index = aliasScroll + i;

            String alias = aliasList.get(index);

            // staggered per-row slide-in
            float ri = Ui.entranceProgress(
                    aliasOpenTime,
                    70L + i * 20L,
                    190L
            );

            if (ri <= 0.02f) {
                continue;
            }

            int rowOffsetX =
                    (int) ((1f - ri) * -34);

            // option "button": inset with a gap below the previous one
            int ry = aliasGY + ALIAS_HEADER_H
                    + i * ALIAS_ROW_H;

            int ox = aliasGX + PAD;
            int oy = ry + 2;
            int ow = aliasGW - PAD * 2;
            int oh = ALIAS_OPT_H;

            boolean hovered =
                    mouseX >= ox &&
                            mouseX <= ox + ow &&
                            mouseY >= oy &&
                            mouseY < oy + oh;

            if (hovered) {

                context.fill(
                        ox + rowOffsetX,
                        oy,
                        ox + ow + rowOffsetX,
                        oy + oh,
                        Ui.argb(0x403F8AE0, modalAlpha)
                );
            }

            Ui.drawRoundBorder(
                    context,
                    ox + rowOffsetX,
                    oy,
                    ow,
                    oh,
                    3,
                    Ui.argb(
                            hovered ? Ui.ACCENT : 0xFF3A4A66,
                            modalAlpha * ri
                    )
            );

            String label = tr.trimToWidth(
                    alias,
                    ow - 8
            );

            context.drawTextWithShadow(
                    tr,
                    Text.literal(label),
                    ox + 4 + rowOffsetX,
                    oy + (oh - 9) / 2,
                    Ui.argb(
                            hovered ? Ui.ACCENT_SOFT : Ui.TEXT,
                            modalAlpha * ri
                    )
            );
        }
    }

    /** Centered modal with a single text field for the typed reason. */
    private void drawReasonModal(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        float slide = Ui.entranceProgress(reasonOpenTime, 0L, 220L);
        float modalAlpha =
                Ui.entranceProgress(reasonOpenTime, 0L, 160L);

        if (modalAlpha <= 0.01f) {
            return;
        }

        reasonGW = 248;
        reasonGH = 108;

        int offsetX = (int) ((1f - slide) * -28);

        reasonGX = (screenWidth - reasonGW) / 2 + offsetX;
        reasonGY = (screenHeight - reasonGH) / 2;

        Ui.drawPopupFrame(
                context,
                reasonGX,
                reasonGY,
                reasonGW,
                reasonGH,
                modalAlpha
        );

        context.drawCenteredTextWithShadow(
                tr,
                Lang.text(Key.REASON_TITLE),
                reasonGX + reasonGW / 2,
                reasonGY + 12,
                Ui.argb(Ui.TEXT, modalAlpha)
        );

        int fieldX = reasonGX + PAD;
        int fieldY = reasonGY + 42;
        int fieldW = reasonGW - PAD * 2;
        int fieldH = 22;

        // visible bordered box so it is obvious where to type
        boolean fieldFocused =
                reasonField != null && reasonField.isFocused();

        Ui.drawRoundRect(
                context,
                fieldX, fieldY, fieldW, fieldH,
                4,
                Ui.argb(Ui.SURFACE_2, modalAlpha));

        Ui.drawRoundBorder(
                context,
                fieldX, fieldY, fieldW, fieldH,
                4,
                Ui.argb(
                        fieldFocused ? Ui.ACCENT : Ui.BORDER,
                        modalAlpha));

        if (fieldFocused) {
            Ui.drawHoverGlow(
                    context,
                    fieldX, fieldY, fieldW, fieldH,
                    4, Ui.ACCENT, 0.6f, modalAlpha * 0.6f);
        }

        if (reasonField != null) {

            reasonField.setX(fieldX);
            reasonField.setY(fieldY + 4);
            reasonField.setWidth(fieldW);

            reasonField.render(context, mouseX, mouseY, delta);
        }

        // confirm button
        int btnW = 96;
        int btnH = 18;
        int btnX = reasonGX + (reasonGW - btnW) / 2;
        int btnY = reasonGY + reasonGH - btnH - 10;

        boolean hovered =
                mouseX >= btnX && mouseX <= btnX + btnW &&
                        mouseY >= btnY && mouseY <= btnY + btnH;

        Ui.drawRoundRect(
                context,
                btnX, btnY, btnW, btnH,
                4,
                Ui.argb(
                        hovered ? Ui.ACCENT : Ui.ACCENT_DARK,
                        modalAlpha
                )
        );

        String label = tr.trimToWidth(
                Lang.t(Key.REASON_CONFIRM),
                btnW - 10
        );

        context.drawCenteredTextWithShadow(
                tr,
                Text.literal(label),
                btnX + btnW / 2,
                btnY + (btnH - 9) / 2,
                Ui.argb(0xFFFFFFFF, modalAlpha)
        );
    }

    /**
     * The two quick buttons under the header: "Copy name" (left) and
     * "Duplicate to chat" (right), split in two equal halves.
     */
    private void drawFixedRows(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        int y = PAD + HEADER_H;
        int h = FIXED_H - 2;
        int half = (panelW - PAD * 2 - FIXED_GAP) / 2;

        int active = inFixedRow(mouseX, mouseY);

        drawFixedButton(
                context,
                tr,
                PAD,
                y,
                half,
                h,
                "\uD83D\uDCCB",
                Lang.t(Key.OVERLAY_COPY_NAME),
                active == BTN_COPY,
                alpha
        );

        drawFixedButton(
                context,
                tr,
                PAD + half + FIXED_GAP,
                y,
                panelW - PAD * 2 - half - FIXED_GAP,
                h,
                "\uD83D\uDCE5",
                Lang.t(Key.OVERLAY_DUP_CHAT),
                active == BTN_DUP,
                alpha
        );
    }

    private void drawFixedButton(
            DrawContext context,
            TextRenderer tr,
            int x,
            int y,
            int w,
            int h,
            String icon,
            String label,
            boolean hovered,
            float alpha
    ) {

        Ui.drawRoundRect(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                Ui.argb(
                        hovered
                                ? Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.22f)
                                : Ui.mix(Ui.PANEL_BG, Ui.TEXT, 0.04f),
                        alpha
                )
        );

        Ui.drawRoundBorder(
                context,
                x, y, w, h,
                Design.RADIUS_MD,
                Ui.argb(hovered ? Ui.ACCENT : Ui.BORDER, alpha)
        );

        String text = icon + " " + label;
        int avail = w - 8;

        // narrow popup: drop the icon before trimming the label
        if (tr.getWidth(text) > avail &&
                tr.getWidth(label) <= avail) {

            text = label;
        }

        text = tr.trimToWidth(text, avail);

        context.drawTextWithShadow(
                tr,
                Text.literal(text),
                x + w / 2 - tr.getWidth(text) / 2,
                y + (h - 9) / 2,
                Ui.argb(
                        hovered ? Ui.ACCENT_SOFT : Ui.TEXT,
                        alpha
                )
        );
    }

    private void drawBody(
            DrawContext context,
            TextRenderer tr,
            int mouseX,
            int mouseY,
            float alpha
    ) {

        if (items.isEmpty()) {

            context.drawCenteredTextWithShadow(
                    tr,
                    Text.literal(
                            tr.trimToWidth(
                                    Lang.t(Key.OVERLAY_NO_ACTIONS),
                                    panelW - PAD * 2
                            )
                    ),
                    panelW / 2,
                    contentTop + 8,
                    Ui.argb(Ui.TEXT_MUTED, alpha)
            );

            return;
        }

        boolean inside =
                insidePanel(mouseX, mouseY);

        long now = System.currentTimeMillis();

        float remain = 1f;

        if (confirmItem != null) {

            remain = Ui.animationsEnabled()
                    ? Ui.clamp01(
                            (CONFIRM_MS - (now - confirmAt))
                                    / (float) CONFIRM_MS
                    )
                    : 1f;
        }

        int hoverNow = -1;

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            Row row = rows.get(rowIndex);

            float absYf = row.offset - scroll;
            int absY = Math.round(absYf);

            if (row.item.separator) {

                if (absY >= contentTop - SEP_H &&
                        absY <= contentBottom) {

                    String caption = tr.trimToWidth(
                            row.item.label,
                            panelW - PAD * 2 - 20
                    );

                    context.drawCenteredTextWithShadow(
                            tr,
                            Text.literal(caption),
                            panelW / 2,
                            absY + (SEP_H - 9) / 2,
                            Ui.argb(Ui.TEXT_MUTED, alpha)
                    );

                    // hairlines on both sides of the caption
                    int capW = tr.getWidth(caption);

                    int capLeft = panelW / 2 - capW / 2 - 6;
                    int capRight = panelW / 2 + capW / 2 + 6;

                    Ui.drawDivider(
                            context,
                            PAD,
                            absY + SEP_H / 2,
                            Math.max(0, capLeft - PAD),
                            alpha * 0.8f
                    );

                    Ui.drawDivider(
                            context,
                            capRight,
                            absY + SEP_H / 2,
                            Math.max(0, panelW - PAD - capRight),
                            alpha * 0.8f
                    );
                }

                continue;
            }

            if (absY < contentTop - ROW_H ||
                    absY > contentBottom) {

                continue;
            }

            boolean hovered =
                    inside &&
                            mouseY >= panelY + absY &&
                            mouseY < panelY + absY + ROW_H;

            boolean confirming =
                    confirmItem == row.item;

            boolean flashing =
                    flashItem == row.item &&
                            now - flashTime < 300L;

            int bg = 0;

            if (confirming) {
                bg = Ui.argb(
                        0xFF8A2B2B,
                        alpha * (0.30f + 0.70f * remain)
                );
            } else if (flashing) {
                bg = Ui.argb(0x4055DD77, alpha);
            } else if (hovered) {
                bg = Ui.argb(
                        Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.20f),
                        alpha
                );
            } else {
                bg = Ui.argb(
                        Ui.mix(Ui.PANEL_BG, Ui.TEXT, 0.035f),
                        alpha * 0.9f
                );
            }

            Ui.drawRoundRect(
                    context,
                    PAD,
                    absY,
                    panelW - PAD * 2,
                    ROW_H - 2,
                    5,
                    bg
            );

            int borderColor =
                    confirming
                            ? Ui.DANGER
                            : flashing
                            ? Ui.SUCCESS
                            : hovered
                            ? Ui.ACCENT
                            : Ui.mix(Ui.PANEL_BORDER, Ui.TEXT, 0.08f);

            Ui.drawRoundBorder(
                    context,
                    PAD,
                    absY,
                    panelW - PAD * 2,
                    ROW_H - 2,
                    5,
                    Ui.argb(
                            borderColor,
                            alpha * (hovered || confirming
                                    ? 1f
                                    : 0.5f)
                    )
            );

            // accent edge on the hovered row, matching the list screens
            if (hovered && !confirming) {

                context.fill(
                        PAD + 1,
                        absY + 3,
                        PAD + 3,
                        absY + ROW_H - 5,
                        Ui.argb(Ui.ACCENT, alpha)
                );
            }

            if (confirming) {

                int pw = (int)
                        ((panelW - PAD * 2) * remain);

                context.fill(
                        PAD + 1,
                        absY + ROW_H - 4,
                        PAD + 1 + pw,
                        absY + ROW_H - 3,
                        Ui.argb(Ui.DANGER, alpha)
                );
            }

            int color =
                    confirming
                            ? Ui.DANGER
                            : row.item.confirmationRequired
                            ? 0xFFE0A0A0
                            : Ui.TEXT;

            context.drawTextWithShadow(
                    tr,
                    Text.literal(
                            tr.trimToWidth(
                                    row.item.label,
                                    panelW - PAD * 2 - 10
                            )
                    ),
                    PAD + 4,
                    absY + (ROW_H - 9) / 2,
                    Ui.argb(color, alpha)
            );

            if (hovered) {
                hoverNow = rowIndex;
            }
        }

        // hover tick sound on row change
        if (hoverNow != lastHoverRow) {
            lastHoverRow = hoverNow;

            if (hoverNow >= 0) {
                Ui.playHover();
            }
        }
    }

    private void drawFooter(
            DrawContext context,
            TextRenderer tr,
            float alpha
    ) {

        String line1 =
                Lang.t(Key.OVERLAY_HINT_CLOSE) +
                        " • " +
                        Lang.t(Key.OVERLAY_HINT_SCROLL);

        context.drawCenteredTextWithShadow(
                tr,
                Text.literal(
                        tr.trimToWidth(line1, panelW - PAD * 2)
                ),
                panelW / 2,
                panelH - PAD - 9,
                Ui.argb(0xFF556070, alpha)
        );
    }

    /**
     * Bottom-right corner: drag to resize the whole popup (every element
     * re-flows through the new width/height).
     */
    /**
     * Resize affordance: three diagonal ticks in the bottom-right corner.
     * The corner itself is the hit area, so there is no separate button
     * frame to misalign with the rounded border.
     */
    private void drawResizeHandle(
            DrawContext context,
            TextRenderer tr,
            double mouseX,
            double mouseY,
            float alpha
    ) {

        boolean hovered =
                resizing || inResize(mouseX, mouseY);

        int dotColor = hovered
                ? Ui.ACCENT_SOFT
                : Ui.mix(Ui.TEXT_MUTED, Ui.ACCENT_SOFT, 0.35f);

        for (int i = 0; i < 3; i++) {

            int len = 3 + i * 3;
            int ox = panelW - 6 - len;
            int oy = panelH - 6 - i * 3;

            context.fill(
                    ox,
                    oy,
                    panelW - 6 - i * 3,
                    oy + 1,
                    Ui.argb(dotColor, alpha)
            );
        }
    }

    /**
     * Header affordance: a subtle grab hint on the right of the title
     * bar. The whole header is draggable, so this is only a visual cue.
     */
    private void drawGrip(
            DrawContext context,
            TextRenderer tr,
            double mouseX,
            double mouseY,
            float alpha
    ) {

        boolean hovered = dragging || inGrip(mouseX, mouseY);

        int dotColor = hovered
                ? Ui.ACCENT_SOFT
                : Ui.mix(Ui.TEXT_MUTED, Ui.ACCENT_SOFT, 0.35f);

        String dots = "\u22EF";

        context.drawTextWithShadow(
                tr,
                Text.literal(dots),
                panelW - PAD - tr.getWidth(dots),
                PAD + 2,
                Ui.argb(dotColor, alpha)
        );
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (!open) {
            return false;
        }

        // swallow clicks while the close animation plays
        if (closing) {
            return true;
        }

        if (button != 0) {
            return true;
        }

        // alias modal swallows all clicks
        if (reasonOpen) {

            // confirm button
            int btnW = 96;
            int btnH = 18;
            int btnX = reasonGX + (reasonGW - btnW) / 2;
            int btnY = reasonGY + reasonGH - btnH - 10;

            boolean onButton =
                    mouseX >= btnX && mouseX <= btnX + btnW &&
                            mouseY >= btnY && mouseY <= btnY + btnH;

            if (onButton) {
                confirmReason();
                return true;
            }

            // clicking the field focuses it; anything else keeps the
            // modal open (no accidental dismissal)
            if (reasonField != null) {
                reasonField.mouseClicked(
                        new net.minecraft.client.gui.Click(
                                mouseX, mouseY,
                                new net.minecraft.client.input.MouseInput(0, 0)
                        ),
                        false
                );
            }

            return true;
        }

        if (aliasOpen) {

            boolean inside =
                    mouseX >= aliasGX &&
                            mouseX <= aliasGX + aliasGW &&
                            mouseY >= aliasGY &&
                            mouseY <= aliasGY + aliasGH;

            if (inside && !aliasList.isEmpty()) {

                int startY =
                        aliasGY + ALIAS_HEADER_H + 2;

                // floor, not truncation: clicks above the first
                // option must give a negative row
                int row = (int) Math.floor(
                        (mouseY - startY) / ALIAS_ROW_H
                );

                boolean withinOptionY =
                        mouseY >= startY + row * ALIAS_ROW_H &&
                                mouseY < startY + row * ALIAS_ROW_H
                                        + ALIAS_OPT_H;

                int index = aliasScroll + row;

                if (row >= 0 &&
                        withinOptionY &&
                        index < aliasList.size()) {

                    String alias = aliasList.get(index);

                    // a macro keeps its name in the toast, an action
                    // shows the action label instead
                    if (aliasMacro != null) {

                        MacroExecutor.executeWithAlias(
                                aliasMacro,
                                aliasTarget,
                                alias
                        );

                        Toasts.INSTANCE.success(
                                Lang.t(Key.TOAST_EXECUTED,
                                        aliasMacro.getName())
                        );

                    } else if (aliasEntry != null) {

                        MacroExecutor.executeActionWithAlias(
                                aliasEntry.getCommand(),
                                aliasTarget,
                                alias,
                                aliasEntry.getLabel()
                        );

                        Toasts.INSTANCE.success(
                                Lang.t(Key.TOAST_EXECUTED,
                                        aliasEntry.getLabel())
                        );
                    }
                }
            }

            // any click (option or outside) finishes the flow
            requestClose(0);

            return true;
        }

        layout();

        if (inGrip(mouseX, mouseY)) {

            dragging = true;
            manuallyPlaced = true;
            grabDX = mouseX - panelX;
            grabDY = mouseY - panelY;

            return true;
        }

        if (inResize(mouseX, mouseY)) {

            resizing = true;
            manuallyPlaced = true;

            // negative offsets from the bottom-right corner
            grabDX = mouseX - (panelX + panelW);
            grabDY = mouseY - (panelY + panelH);

            return true;
        }

        if (!insidePanel(mouseX, mouseY)) {
            requestClose(0);
            return false;
        }

        int fixed = inFixedRow(mouseX, mouseY);

        if (fixed == BTN_COPY) {
            copyName();
            return true;
        }

        if (fixed == BTN_DUP) {
            duplicateToChat();
            return true;
        }

        int index = hitRow(mouseY);

        if (index < 0) {
            return true;
        }

        handleRow(rows.get(index).item);

        return true;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    ) {

        if (!open) {
            return false;
        }

        if (resizing) {

            // top-left stays put: only width/height follow the cursor
            panelW = (int) Math.round(
                    mouseX - grabDX - panelX
            );

            panelH = (int) Math.round(
                    mouseY - grabDY - panelY
            );

            layout();

            return true;
        }

        if (!dragging) {
            return false;
        }

        panelX = (int) Math.round(mouseX - grabDX);
        panelY = (int) Math.round(mouseY - grabDY);

        layout();

        return true;
    }

    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (!open) {
            return false;
        }

        if (resizing) {

            resizing = false;
            savePopupSize();

            return true;
        }

        if (!dragging) {
            return false;
        }

        dragging = false;

        return true;
    }

    /** Persists the popup size when the "save size" toggle is on. */
    private void savePopupSize() {

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        if (!config.isSavePopupSize()) {
            return;
        }

        config.setPopupWidth(panelW);
        config.setPopupHeight(panelH);

        StafftoolsClient.saveConfig();
    }

    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {

        if (!open) {
            return false;
        }

        if (aliasOpen) {

            if (mouseX >= aliasGX &&
                    mouseX <= aliasGX + aliasGW &&
                    mouseY >= aliasGY &&
                    mouseY <= aliasGY + aliasGH) {

                int visible = Math.min(
                        aliasList.size(),
                        8
                );

                int maxScroll = Math.max(
                        0,
                        aliasList.size() - visible
                );

                aliasScroll = Math.clamp(
                        aliasScroll
                                + (verticalAmount > 0 ? -1 : 1),
                        0,
                        maxScroll
                );
            }

            return true;
        }

        layout();

        if (!insidePanel(mouseX, mouseY)) {
            return false;
        }

        lastScrollAt = System.currentTimeMillis();

        scrollTarget += verticalAmount > 0
                ? -ROW_H
                : ROW_H;

        layout();

        return true;
    }

    private void copyName() {

        MinecraftClient.getInstance().keyboard
                .setClipboard(player.name());

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_COPIED, player.name())
        );

        UiSound.copy();

        requestClose(0);
    }

    /**
     * Inserts the nick into the chat input at the caret, so the command
     * can be typed right after it. Falls back to the clipboard when the
     * chat screen is not the one currently open.
     */
    private void duplicateToChat() {

        MinecraftClient client =
                MinecraftClient.getInstance();

        String nick = player.name();

        if (client.currentScreen instanceof ChatScreen chat) {

            TextFieldWidget field =
                    ((ChatScreenAccessor) chat)
                            .stafftools$getChatField();

            if (field != null) {

                String text = field.getText();
                int caret = Math.clamp(
                        field.getCursor(),
                        0,
                        text.length()
                );

                field.setText(
                        text.substring(0, caret) +
                                nick +
                                text.substring(caret)
                );

                field.setCursor(caret + nick.length(), false);

                Toasts.INSTANCE.success(
                        Lang.t(Key.TOAST_DUP_CHAT, nick)
                );

                requestClose(0);
                return;
            }
        }

        client.keyboard.setClipboard(nick);

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_COPIED, nick)
        );

        requestClose(0);
    }

    private void handleRow(RowItem item) {

        // second click on a confirming row — proceed to execution
        if (confirmItem == item) {

            confirmItem = null;
            confirmAt = 0L;

            if (execute(item)) {
                return;
            }

            // short flash on the row, then animated close
            requestClose(300);

            return;
        }

        if (item.confirmationRequired &&
                StafftoolsClient.getConfig()
                        .isDangerousMacroConfirmation()) {

            confirmItem = item;
            confirmAt = System.currentTimeMillis();

            return;
        }

        // deferred: alias modal is open — nothing sent yet,
        // the popup stays alive underneath
        if (execute(item)) {
            return;
        }

        requestClose(300);
    }

    /**
     * Executes the row action. Returns true when execution was DEFERRED
     * (the alias picker modal opened and the flow continues there);
     * false means everything already finished.
     */
    private boolean execute(RowItem item) {

        List<String> commands =
                item.macro != null
                        ? item.macro.getCommands()
                        : item.commands;

        List<String> aliases =
                item.macro != null
                        ? item.macro.getAliases()
                        : item.aliases();

        // <reason> is resolved first: the user types it in a modal,
        // then the flow continues (alias / direct execution) with the
        // reason already substituted.
        if (MacroExecutor.requiresReason(commands)) {

            openReasonModal(item);
            return true;
        }

        boolean needsAlias =
                MacroExecutor.requiresAlias(commands);

        if (needsAlias) {

            // no variants configured: substitute an empty alias
            if (aliases.isEmpty()) {

                if (item.macro != null) {

                    MacroExecutor.executeWithAlias(
                            item.macro,
                            player,
                            ""
                    );

                } else {

                    MacroExecutor.executeActionWithAlias(
                            entryCommand(item),
                            player,
                            "",
                            item.label
                    );
                }

                flashItem = item;
                flashTime = System.currentTimeMillis();

                Toasts.INSTANCE.success(
                        Lang.t(Key.TOAST_EXECUTED,
                                item.label)
                );

                return false;
            }

            // let the user pick a variant first
            aliasMacro = item.macro;
            aliasEntry = item.entry;
            aliasTarget = player;
            aliasList = new ArrayList<>(aliases);
            aliasScroll = 0;
            aliasOpenTime = System.currentTimeMillis();
            aliasOpen = true;

            return true;
        }

        if (item.macro != null) {

            MacroExecutor.execute(
                    item.macro,
                    player
            );

        } else {

            MacroExecutor.executeAction(
                    entryCommand(item),
                    player,
                    item.label
            );
        }

        flashItem = item;
        flashTime = System.currentTimeMillis();

        Toasts.INSTANCE.success(
                Lang.t(Key.TOAST_EXECUTED, item.label)
        );

        return false;
    }

    /** Command of an action row, never null. */
    private static String entryCommand(RowItem item) {
        return item.entry != null
                ? item.entry.getCommand()
                : "";
    }

    private static final class RowItem {

        private String label;
        private final List<String> commands;
        private final boolean dangerous;
        private final boolean confirmationRequired;
        private boolean separator;
        private final Macro macro;
        private final ActionEntry entry;

        private RowItem(ActionEntry entry) {

            this.label =
                    (entry.getIcon() == null
                            ? ""
                            : entry.getIcon()) +
                            " " +
                            entry.getLabel();

            this.commands =
                    List.of(entry.getCommand());

            this.dangerous = entry.isDangerous();
            this.confirmationRequired =
                    entry.isConfirmationRequired();

            this.separator = false;
            this.macro = null;
            this.entry = entry;
        }

        private RowItem(Macro macro) {

            this.label = "\uD83D\uDCCC " + macro.getName();
            this.commands = macro.getCommands();
            this.dangerous = macro.isDangerous();
            this.confirmationRequired =
                    macro.isConfirmationRequired();

            this.separator = false;
            this.macro = macro;
            this.entry = null;
        }

        /** Alias variants to offer for this row. */
        private List<String> aliases() {

            if (macro != null) {
                return macro.getAliases();
            }

            return entry == null
                    ? List.of()
                    : entry.getAliases();
        }

        private static RowItem separator(String text) {

            RowItem item = new RowItem();
            item.separator = true;
            item.label = text;

            return item;
        }

        private RowItem() {

            this.label = "";
            this.commands = List.of();
            this.dangerous = false;
            this.confirmationRequired = false;
            this.separator = false;
            this.macro = null;
            this.entry = null;
        }
    }

    private record Row(
            int offset,
            int height,
            RowItem item
    ) {
    }
}
