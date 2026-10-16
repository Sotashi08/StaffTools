package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Live resize of the vanilla chat.
 *
 * While the chat screen is open a grip sits in the top-right corner of
 * the chat area (the bottom edge is anchored above the input line, so
 * that is the only corner that can be moved). Dragging it writes the
 * vanilla {@code chatWidth} / {@code chatHeightFocused} options and, once
 * the vanilla maximum of 320×180 is passed, a boost factor that
 * {@link ChatHudResizeMixin} multiplies the vanilla value with — so the
 * chat can be made as large as the window. Scrolling over the grip
 * changes {@code chatScale}; double clicking it resets everything.
 *
 * The geometry below mirrors {@code ChatHud#render} step by step, so the
 * drawn frame lands exactly on the message background: the boost math
 * is a pure function of the cursor position, which keeps the chat from
 * fighting the drag.
 */
public final class ChatResizer {

    public static final ChatResizer INSTANCE =
            new ChatResizer();

    /** Size of the corner grip, in GUI pixels. */
    private static final int GRIP_SIZE = 14;

    /** Left inset of the chat area ({@code ChatScreen} renders it at 4). */
    private static final int CHAT_INSET = 4;

    /** Distance between the chat bottom and the window bottom. */
    private static final int BOTTOM_OFFSET = 40;

    /** Constants of the vanilla {@code ChatHud#getWidth/#getHeight}. */
    private static final int WIDTH_BASE = 40;
    private static final int WIDTH_SPAN = 280;
    private static final int HEIGHT_BASE = 20;
    private static final int HEIGHT_SPAN = 160;

    /**
     * Padding of the message background around the wrapped text: 4 on
     * the left, 4 + room for the indicator icon on the right.
     */
    private static final int BACKGROUND_PAD_LEFT = 4;
    private static final int BACKGROUND_PAD_RIGHT = 8;

    /**
     * How far the drawn frame is pushed away from the chat area on the
     * top and the bottom, so it never sits on a message itself. Left and
     * right are the chat's own edges: no text runs there.
     */
    private static final int FRAME_INSET = 2;

    /** Vanilla maxima: 1.0 on the sliders. */
    private static final int MAX_WIDTH_PX =
            MathHelper.floor(WIDTH_BASE + WIDTH_SPAN);

    private static final int MAX_HEIGHT_PX =
            MathHelper.floor(HEIGHT_BASE + HEIGHT_SPAN);

    /** Never smaller than this: the grip has to stay reachable. */
    private static final int MIN_WIDTH_PX = 80;
    private static final int MIN_HEIGHT_PX = 24;

    /** Keep the whole chat on screen. */
    private static final int SCREEN_MARGIN = 8;

    /** Vanilla "auto" (0) collapses the hud, so it is never stepped to. */
    private static final double MIN_SCALE = 0.1;
    private static final double SCALE_STEP = 0.1;

    /** Upper bounds and granularity of the boost factors. */
    private static final double MAX_BOOST = 6.0;
    private static final double BOOST_STEP = 0.02;
    /** Option granularity: one percent is the vanilla slider step. */
    private static final int OPTION_STEPS = 100;

    private boolean resizing;

    private ChatResizer() {
    }

    // ------------------------------------------------------------------
    // state
    // ------------------------------------------------------------------

    public boolean isResizing() {
        return resizing;
    }

    /**
     * The mod config, or null while the client is still starting up:
     * {@code ChatHud#getWidth(D)} runs before the entrypoints, and the
     * vanilla layout must never depend on the mod being ready.
     */
    private static StaffToolsConfig config() {

        try {
            return StafftoolsClient.getConfig();
        } catch (RuntimeException notReadyYet) {
            return null;
        }
    }

    private boolean enabled() {

        StaffToolsConfig config = config();

        return config != null && config.isChatResizeEnabled();
    }

    /**
     * Everything the grip needs: the feature must be on, the hud must
     * be there and the chat must actually be drawn.
     */
    private static boolean usable() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options == null || client.inGameHud == null) {
            return false;
        }

        if (client.options.getChatVisibility().getValue()
                == ChatVisibility.HIDDEN) {

            return false;
        }

        return true;
    }

    // ------------------------------------------------------------------
    // the boost applied on top of the vanilla options
    // ------------------------------------------------------------------

    /**
     * Multiplies a vanilla pixel value. Only used by the mixin; a boost
     * of 1 (the default) leaves vanilla completely untouched.
     */
    public int boostWidth(int vanillaWidth) {

        StaffToolsConfig config = config();

        return boost(
                vanillaWidth,
                config == null ? 1.0 : config.getChatWidthBoost()
        );
    }

    public int boostHeight(int vanillaHeight) {

        StaffToolsConfig config = config();

        return boost(
                vanillaHeight,
                config == null ? 1.0 : config.getChatHeightBoost()
        );
    }

    private static int boost(int base, double boost) {

        if (boost <= 1.0 + 1e-9) {
            return base;
        }

        return Math.max(1, Math.round(base * (float) boost));
    }

    private static double configWidthBoost() {

        StaffToolsConfig config = config();

        return config == null ? 1.0 : config.getChatWidthBoost();
    }

    private static double configHeightBoost() {

        StaffToolsConfig config = config();

        return config == null ? 1.0 : config.getChatHeightBoost();
    }

    // ------------------------------------------------------------------
    // vanilla pixel math (mirrors ChatHud#getWidth / #getHeight)
    // ------------------------------------------------------------------

    /** Vanilla width in pixels, WITHOUT the boost. */
    private static int vanillaWidth(double option) {
        return MathHelper.floor(option * WIDTH_SPAN + WIDTH_BASE);
    }

    /** Vanilla height in pixels, WITHOUT the boost. */
    private static int vanillaHeight(double option) {
        return MathHelper.floor(option * HEIGHT_SPAN + HEIGHT_BASE);
    }

    /** Live width in pixels (what the renderer actually draws). */
    private static int liveWidth() {
        return Math.round(
                vanillaWidth(
                        optionValue(options().getChatWidth())
                ) * (float) configWidthBoost()
        );
    }

    /** Live height in pixels (what the renderer actually draws). */
    private static int liveHeight() {
        return Math.round(
                vanillaHeight(
                        optionValue(heightOption())
                ) * (float) configHeightBoost()
        );
    }

    // ------------------------------------------------------------------
    // geometry
    // ------------------------------------------------------------------

    /** Screen space area of the chat as it is rendered right now. */
    private record Area(int left, int top, int right, int bottom) {

        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }
    }

    /**
     * The chat scale that is actually rendered. Vanilla treats 0 as
     * "auto", which would collapse the hud, so it is mapped to 1.
     */
    private static double chatScale() {

        double scale = optionValue(options().getChatScale());

        if (Double.isNaN(scale) || scale < MIN_SCALE) {
            return 1.0;
        }

        return scale;
    }

    private static double optionValue(SimpleOption<Double> option) {

        if (option == null) {
            return Double.NaN;
        }

        Double value = option.getValue();

        return value == null ? Double.NaN : value;
    }

    /** Which height option is live: the chat screen focuses the hud. */
    private static SimpleOption<Double> heightOption() {

        GameOptions options = options();

        if (options == null) {
            return null;
        }

        boolean focused =
                MinecraftClient.getInstance().currentScreen
                        instanceof ChatScreen;

        return focused
                ? options.getChatHeightFocused()
                : options.getChatHeightUnfocused();
    }

    private static GameOptions options() {
        return MinecraftClient.getInstance().options;
    }

    /** Line height in unscaled pixels, exactly like the renderer. */
    private static double lineHeight() {

        double spacing = optionValue(
                options().getChatLineSpacing()
        );

        if (Double.isNaN(spacing)) {
            spacing = 0.0;
        }

        return 9.0 * (spacing + 1.0);
    }

    /** How many lines fit into the live height. */
    private static int visibleLines() {

        double line = lineHeight();

        if (line <= 0.0) {
            return 1;
        }

        return Math.max(1, (int) (liveHeight() / line));
    }

    /**
     * The chat area in screen coordinates, built from the very same
     * numbers the renderer uses:
     *
     * <ul>
     *   <li>bottom: {@code floor((windowHeight - 40) / scale) * scale}</li>
     *   <li>top: that minus every visible line height</li>
     *   <li>left / right: the message background padding, scaled</li>
     * </ul>
     */
    private static Area chatArea() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null || options() == null) {
            return null;
        }

        float scale = (float) chatScale();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // windowHeight - 40, floored exactly like ChatHud#render
        int baseline = MathHelper.floor(
                (screenH - BOTTOM_OFFSET) / scale
        );

        int bottom = Math.round(baseline * scale);

        int top = Math.max(
                0,
                bottom - Math.round(
                        (float) visibleLines() * (float) lineHeight() * scale
                )
        );

        // the background is 4px wider than the wrapped text on the left
        // and 4px (plus room for the indicator icon) on the right
        int localWidth = MathHelper.ceil(liveWidth() / scale);

        int left = Math.max(
                0,
                Math.round(CHAT_INSET - BACKGROUND_PAD_LEFT * scale)
        );

        int right = Math.min(
                screenW,
                Math.round(
                        CHAT_INSET
                                + (localWidth + BACKGROUND_PAD_RIGHT) * scale
                )
        );

        return new Area(left, top, right, bottom);
    }

    private static boolean overGrip(Area area, double mouseX, double mouseY) {

        return mouseX >= area.right() - GRIP_SIZE &&
                mouseX <= area.right() + 2 &&
                mouseY >= area.top() - 2 &&
                mouseY <= area.top() + GRIP_SIZE + 2;
    }

    /**
     * The rectangle the frame and the grip live in: the chat area pushed
     * up and down by {@link #FRAME_INSET}, so the frame lines never land
     * on a message. Never leaves the screen.
     */
    private static Area frameOf(Area chat) {

        return new Area(
                chat.left(),
                Math.max(0, chat.top() - FRAME_INSET),
                chat.right(),
                Math.min(
                        MinecraftClient.getInstance()
                                .getWindow()
                                .getScaledHeight(),
                        chat.bottom() + FRAME_INSET
                )
        );
    }

    /** Area the grip is hit-tested and drawn on. */
    private static Area gripArea() {

        if (!usable()) {
            return null;
        }

        Area chat = chatArea();

        return chat == null ? null : frameOf(chat);
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    /** @return true when the grip was grabbed and the click consumed. */
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            boolean doubled
    ) {

        if (!enabled() || resizing) {
            return false;
        }

        Area area = gripArea();

        if (area == null) {
            return false;
        }

        if (!overGrip(area, mouseX, mouseY)) {
            return false;
        }

        // double click: back to the vanilla defaults
        if (doubled && button == 0) {
            reset();
            return true;
        }

        if (button != 0) {
            return false;
        }

        resizing = true;

        apply(mouseX, mouseY);

        return true;
    }

    /** Grip drag: the right and the top edge follow the cursor. */
    public boolean mouseDragged(double mouseX, double mouseY, int button) {

        if (!resizing) {
            return false;
        }

        // the chat screen left: stop resizing, but keep what was applied
        if (!(MinecraftClient.getInstance().currentScreen
                instanceof ChatScreen) || !usable()) {

            finish();
            return false;
        }

        apply(mouseX, mouseY);

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        if (!resizing) {
            return false;
        }

        finish();

        return true;
    }

    /** Wheel over the grip: chat scale. */
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double verticalAmount
    ) {

        if (!enabled() || resizing || !usable()) {
            return false;
        }

        Area area = gripArea();

        if (area == null || !overGrip(area, mouseX, mouseY)) {
            return false;
        }

        GameOptions options = options();

        if (options == null) {
            return false;
        }

        double current = optionValue(options.getChatScale());

        if (Double.isNaN(current) || current < MIN_SCALE) {
            current = 1.0;
        }

        // wheel up = bigger, like every zoom in the game
        double next = current + (
                verticalAmount > 0
                        ? SCALE_STEP
                        : -SCALE_STEP
        );

        setOption(
                options.getChatScale(),
                quantize(clamp(next, MIN_SCALE, 1.0))
        );

        return true;
    }

    /** Ends a drag that was cut short (the chat screen closed). */
    public void cancel() {

        if (!resizing) {
            return;
        }

        finish();
    }

    private void finish() {

        boolean wasResizing = resizing;
        resizing = false;

        if (wasResizing) {
            persist();
        }
    }

    /** Back to vanilla defaults on every axis. */
    private void reset() {

        GameOptions options = options();

        if (options == null) {
            return;
        }

        setOption(options.getChatWidth(), quantize(1.0));
        setOption(heightOption(), quantize(1.0));
        setOption(options.getChatScale(), quantize(1.0));

        StaffToolsConfig config = config();

        if (config != null) {
            config.setChatWidthBoost(1.0);
            config.setChatHeightBoost(1.0);
        }

        persist();
    }

    /**
     * Stores the size on disk, but only while the "save chat size"
     * toggle is on. Off means: the chat still resizes live, it just
     * falls back to the vanilla values on the next start.
     */
    private void persist() {

        StaffToolsConfig config = config();

        if (config == null || !config.isSaveChatSize()) {
            return;
        }

        try {
            StafftoolsClient.saveConfig();
        } catch (RuntimeException ignored) {
            // never break the drag because the config file is not writable
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (options() == null || !client.isRunning()) {
            return;
        }

        try {
            client.options.write();
        } catch (RuntimeException ignored) {
            // a full disk or a read-only game dir must never break the
            // drag: the values stay live, they are just not persisted
        }
    }

    // ------------------------------------------------------------------
    // applying
    // ------------------------------------------------------------------

    private void apply(double mouseX, double mouseY) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (options() == null || client.inGameHud == null) {
            return;
        }

        float scale = (float) chatScale();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // same baseline the renderer uses
        int baseline = MathHelper.floor(
                (screenH - BOTTOM_OFFSET) / scale
        );

        int bottom = Math.round(baseline * scale);

        // --- width: invert the right edge of the background -----------
        // right = inset + (localWidth + 8) * scale, so
        // localWidth = (wantedRight - inset) / scale - 8
        int wantedRight = clampPixel(
                (int) Math.round(mouseX),
                CHAT_INSET + MIN_WIDTH_PX,
                Math.max(CHAT_INSET + MIN_WIDTH_PX, screenW - SCREEN_MARGIN)
        );

        int localWidth = Math.max(
                1,
                Math.round((wantedRight - CHAT_INSET) / scale)
                        - BACKGROUND_PAD_RIGHT
        );

        // Undo the chat scale: everything below the line is tracked in
        // unscaled pixels, so the grip stops drifting the moment the
        // scale changes.
        applyWidth(localWidth * scale);

        // --- height: invert the top edge, one chat line at a time -----
        // the grip sits on the frame, which is FRAME_INSET above the
        // chat area itself
        int wantedTop = clampPixel(
                (int) Math.round(mouseY) + FRAME_INSET,
                SCREEN_MARGIN,
                Math.max(SCREEN_MARGIN, bottom - MIN_HEIGHT_PX)
        );

        double line = lineHeight();

        int lines = Math.max(
                1,
                Math.round(
                        (bottom - wantedTop) / (float) (line * scale)
                )
        );

        // the renderer counts whole lines, so the wanted size is a
        // whole number of lines
        applyHeight(lines * line);
    }

    /**
     * Splits a wanted unscaled width into the vanilla option and the
     * boost. Purely a function of the target: the current boost never
     * feeds back into it, which is what keeps the chat from twitching
     * while it is dragged past the vanilla maximum.
     *
     * The vanilla slider is only used while it can still deliver the
     * wanted width — otherwise it is pinned and the boost that
     * {@link ChatHudResizeMixin} applies takes over.
     */
    private static void applyWidth(double pixels) {

        double option = optionForWidth(pixels);

        if (vanillaWidth(option) >= pixels) {

            setOption(options().getChatWidth(), option);
            setBoost(true, 1.0);
            return;
        }

        setOption(options().getChatWidth(), quantize(1.0));
        setBoost(true, quantizeBoostUp(pixels / MAX_WIDTH_PX));
    }

    /** Same split for the height, line count driven. */
    private static void applyHeight(double pixels) {

        double option = optionForHeight(pixels);

        if (vanillaHeight(option) >= pixels) {

            setOption(heightOption(), option);
            setBoost(false, 1.0);
            return;
        }

        setOption(heightOption(), quantize(1.0));
        setBoost(false, quantizeBoostUp(pixels / MAX_HEIGHT_PX));
    }

    /**
     * Smallest slider step that still delivers the wanted number of
     * pixels, or 1.0 when it does not fit into the vanilla maximum
     * any more. The slider steps are coarse (2.8px / 1.6px) and the
     * vanilla formulas floor their result, so stepping up one notch is
     * needed to not fall a hair short.
     */
    private static double optionForWidth(double pixels) {

        double option = quantize(inverseWidth(pixels));

        while (option < 1.0 && vanillaWidth(option) < pixels) {
            option = Math.min(1.0, option + 1.0 / OPTION_STEPS);
        }

        return option;
    }

    /** Same for the height. */
    private static double optionForHeight(double pixels) {

        double option = quantize(inverseHeight(pixels));

        while (option < 1.0 && vanillaHeight(option) < pixels) {
            option = Math.min(1.0, option + 1.0 / OPTION_STEPS);
        }

        return option;
    }

    /** Inverse of {@link #vanillaWidth(double)}. */
    private static double inverseWidth(double pixels) {
        return clamp((pixels - WIDTH_BASE) / WIDTH_SPAN, 0.0, 1.0);
    }

    /** Inverse of {@link #vanillaHeight(double)}. */
    private static double inverseHeight(double pixels) {
        return clamp((pixels - HEIGHT_BASE) / HEIGHT_SPAN, 0.0, 1.0);
    }

    /**
     * Only writes a real change: every write re-wraps the visible
     * messages, and an unchanged value would do that for nothing.
     */
    private static void setOption(
            SimpleOption<Double> option,
            double value
    ) {

        if (option == null) {
            return;
        }

        double current = optionValue(option);

        if (Double.isNaN(current) ||
                Math.abs(current - value) < 1.0 / OPTION_STEPS / 2.0) {

            return;
        }

        option.setValue(value);
    }

    /**
     * Writes a boost only when it really moved. Nothing is saved here:
     * the mixin reads the live value, persistence happens once when the
     * drag ends.
     */
    private static void setBoost(boolean width, double value) {

        StaffToolsConfig config = config();

        if (config == null) {
            return;
        }

        double current = width
                ? config.getChatWidthBoost()
                : config.getChatHeightBoost();

        if (Math.abs(current - value) < 1e-4) {
            return;
        }

        if (width) {
            config.setChatWidthBoost(value);
        } else {
            config.setChatHeightBoost(value);
        }
    }

    private static double quantizeBoost(double value) {

        double clamped = clamp(value, 1.0, MAX_BOOST);

        return Math.round(clamped / BOOST_STEP) * BOOST_STEP;
    }

    /**
     * Same quantization, but always up. Reaching for a size may overshoot
     * by one step: rounding down would drop a whole rendered line
     * (height) or a wrapped character column (width) and the grip would
     * start lagging behind the cursor.
     */
    private static double quantizeBoostUp(double value) {

        double clamped = clamp(value, 1.0, MAX_BOOST);

        return Math.ceil(clamped / BOOST_STEP) * BOOST_STEP;
    }

    private static double quantize(double value) {
        return Math.round(value * OPTION_STEPS) / (double) OPTION_STEPS;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    private static int clampPixel(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    /**
     * Draws the animated chat frame: a resting hairline, the orbit bars
     * that the menus use (in their light preset — the chat perimeter is
     * huge, so this one is deliberately the cheapest) and thin accent
     * ribbons top and bottom. No glass body or shadow: the chat has to
     * stay readable.
     *
     * The whole frame sits {@link #FRAME_INSET} pixels away from the chat
     * area on the top and the bottom, so it never covers a message. It
     * can be switched off in the main settings.
     */
    public void renderFrame(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {

        StaffToolsConfig config = config();

        if (!enabled() || !usable() ||
                config == null || !config.isChatFrameEnabled()) {

            return;
        }

        Area chat = chatArea();

        if (chat == null || chat.width() < GRIP_SIZE ||
                chat.height() < GRIP_SIZE) {

            return;
        }

        Area area = frameOf(chat);

        boolean hovered = resizing || overGrip(area, mouseX, mouseY);

        float alpha = resizing
                ? 1f
                : hovered
                        ? 0.9f
                        : 0.55f;

        int frame = resizing || hovered
                ? Ui.mix(Ui.PANEL_BORDER, Ui.ACCENT, 0.6f)
                : Ui.PANEL_BORDER;

        Ui.drawRoundBorder(
                context,
                area.left(),
                area.top(),
                area.width(),
                area.height(),
                3,
                Ui.argb(frame, alpha)
        );

        // the orbit animation of the menu / popup frames, trimmed down
        // for a chat-sized perimeter
        Ui.drawOrbitFrame(
                context,
                area.left(),
                area.top(),
                area.width(),
                area.height(),
                alpha,
                Ui.OrbitStyle.CHAT
        );

        // accent ribbons, one pixel at the top and at the bottom of the
        // frame, echoing the popup frame
        if (area.width() > 12) {

            Ui.drawGradientH(
                    context,
                    area.left() + 3,
                    area.top() + 2,
                    area.width() - 6,
                    1,
                    Ui.ACCENT,
                    Ui.ACCENT_2,
                    alpha * 0.7f
            );

            Ui.drawGradientH(
                    context,
                    area.left() + 3,
                    area.bottom() - 3,
                    area.width() - 6,
                    1,
                    Ui.ACCENT_2,
                    Ui.ACCENT,
                    alpha * 0.35f
            );
        }
    }

    /**
     * Draws the corner grip and the size readout. Drawn after everything
     * else (the actions popup included), so the grip stays reachable no
     * matter what is on top of the frame.
     *
     * With the frame switched off the grip only shows while it is
     * actually used: nothing of the mod stays on screen, but resizing
     * keeps working.
     */
    public void renderGrip(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {

        if (!enabled() || !usable()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        Area chat = chatArea();

        if (chat == null || chat.width() < GRIP_SIZE ||
                chat.height() < GRIP_SIZE) {

            return;
        }

        Area area = frameOf(chat);

        boolean hovered = resizing || overGrip(area, mouseX, mouseY);

        StaffToolsConfig config = config();

        boolean framed = config != null && config.isChatFrameEnabled();

        // frame off: the grip fades in only while something happens
        if (!framed && !hovered) {
            return;
        }

        float alpha = resizing
                ? 1f
                : hovered
                        ? 0.9f
                        : 0.55f;

        // grip in the top-right corner (the chat grows to the left/up)
        int gx = area.right() - GRIP_SIZE;
        int gy = area.top();

        if (hovered) {

            context.setCursor(StandardCursors.RESIZE_ALL);

            context.fill(
                    gx,
                    gy,
                    area.right(),
                    gy + GRIP_SIZE,
                    Ui.argb(Ui.mix(Ui.PANEL_BG, Ui.ACCENT, 0.35f), alpha)
            );
        }

        Ui.drawRoundBorder(
                context,
                gx,
                gy,
                GRIP_SIZE,
                GRIP_SIZE,
                3,
                Ui.argb(
                        resizing || hovered
                                ? Ui.ACCENT
                                : Ui.mix(Ui.ACCENT, Ui.TEXT_MUTED, 0.45f),
                        alpha
                )
        );

        // three diagonal ticks pointing at the corner
        int tick = resizing || hovered
                ? Ui.ACCENT_SOFT
                : Ui.mix(Ui.TEXT_MUTED, Ui.ACCENT_SOFT, 0.35f);

        for (int i = 0; i < 3; i++) {

            int len = 3 + i * 3;

            int ox = area.right() - 3 - len;
            int oy = gy + 2 + i * 3;

            context.fill(
                    ox,
                    oy,
                    area.right() - 3 - i * 3,
                    oy + 1,
                    Ui.argb(tick, alpha)
            );
        }

        // size readout: pixels as drawn, chat scale and the boost that
        // pushes the chat past the vanilla maximum
        if (hovered) {

            int percent = (int) Math.round(chatScale() * 100.0);

            StringBuilder label = new StringBuilder()
                    .append(liveWidth())
                    .append('\u00d7')
                    .append(liveHeight())
                    .append(" \u00b7 ")
                    .append(percent)
                    .append('%');

            double biggest = Math.max(
                    configWidthBoost(),
                    configHeightBoost()
            );

            if (biggest > 1.0) {

                // past the vanilla maximum, double click resets it
                label.append(" \u00b7 \u00d7")
                        .append(trimBoost(biggest))
                        .append("  \u21bb");
            }

            TextRenderer tr = client.textRenderer;

            int labelWidth = tr.getWidth(label.toString());

            int lx = Math.max(
                    2,
                    Math.min(
                            area.right() - labelWidth - 2,
                            area.left() + 2
                    )
            );

            context.drawTextWithShadow(
                    tr,
                    Text.literal(label.toString()),
                    lx,
                    Math.min(
                            area.bottom() - 10,
                            gy + GRIP_SIZE + 3
                    ),
                    Ui.argb(Ui.TEXT_DIM, alpha)
            );
        }
    }

    /** Formats a boost factor without trailing zeroes. */
    private static String trimBoost(double boost) {

        String text = Double.toString(
                Math.round(boost * 100.0) / 100.0
        );

        return text.endsWith(".0")
                ? text.substring(0, text.length() - 2)
                : text;
    }
}
