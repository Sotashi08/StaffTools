package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.ui.widget.StButton;
import com.uwdie.stafftools.client.ui.widget.StToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Base screen for the mod UI: the launcher-style shell (icon rail, top
 * bar, tab row), entrance animation and helpers to create themed
 * widgets.
 *
 * <p>Subclasses draw their content inside the area reported by
 * {@link #contentLeft()} / {@link #contentTop()} / {@link #contentWidth()},
 * so every screen shares the same chrome and the same layout grid.
 */
public abstract class StScreen extends Screen {

    private static final int MAX_FORM_WIDTH = 380;

    // not final: a screen may be revisited through a parent link
    protected long openTime =
            System.currentTimeMillis();

    private boolean entranceNeedsStart = true;

    private int slotCounter;

    /** Application shell shared by every screen. */
    protected final StShell shell = new StShell();

    /**
     * Stagger counter for the widget entrance. It MUST be cleared on
     * every rebuild: screens are re-entered through parent links and
     * {@code setScreen} re-runs init() on the same instance, so without
     * a reset every visit would push the buttons further away in time.
     */
    protected void resetSlots() {

        slotCounter = 0;
    }

    protected StScreen(Text title) {
        super(title);
    }

    protected int nextSlot() {
        return slotCounter++;
    }

    protected float entrance() {

        if (!Ui.animationsEnabled()) {
            return 1f;
        }

        return Ui.easeOutCubic(
                Ui.animationProgress(openTime, Design.DUR_ENTRANCE)
        );
    }

    // ---------------------------------------------------------------
    // shell geometry
    // ---------------------------------------------------------------

    /** Left edge of the content area. */
    protected int contentLeft() {
        return shell.contentLeft();
    }

    /** Top edge of the content area. */
    protected int contentTop() {
        return shell.contentTop();
    }

    /** Width available to the content. */
    protected int contentWidth() {
        return shell.contentWidth();
    }

    /** Bottom edge of the content area. */
    protected int contentBottom() {
        return shell.contentBottom();
    }

    /**
     * Configures the shell for this screen. Called from {@code init()}
     * by every subclass, so the rail and tabs always reflect the current
     * screen.
     */
    protected void configureShell() {

        // the window rectangle must exist before any widget is placed
        shell.layout();

        shell.rail(
                new StShell.RailItem(
                        "\u26A1", "btn.macros", "hint.rail.macros",
                        () -> client.setScreen(new MacroListScreen(this))),
                new StShell.RailItem(
                        "\uD83D\uDCCB", "btn.playerActions", "hint.rail.actions",
                        () -> client.setScreen(new ActionsConfigScreen(this))),
                new StShell.RailItem(
                        "\uD83D\uDD52", "btn.history", "hint.rail.history",
                        () -> client.setScreen(new ActionHistoryScreen(this))),
                new StShell.RailItem(
                        "\uD83D\uDC64", "btn.profiles", "hint.rail.profiles",
                        () -> client.setScreen(new ProfilesScreen(this))),
                new StShell.RailItem(
                        "\uD83D\uDD0E", "btn.audit", "hint.rail.audit",
                        () -> client.setScreen(new AuditScreen(this))),
                new StShell.RailItem(
                        "\u2699", "btn.settings", "hint.rail.settings",
                        () -> client.setScreen(new SettingsScreen(this)))
        );
    }

    /** Title shown in the top bar. */
    protected String shellTitle() {
        return "StaffTools";
    }

    /** Brand animation preset; subclasses can opt into a non-static style. */
    protected int shellTitleAnimation() {
        return 0;
    }

    /** Accent subtitle under the title (version, server, ...). */
    protected String shellSubtitle() {
        return "v" + net.fabricmc.loader.api.FabricLoader
                .getInstance()
                .getModContainer("stafftools")
                .map(m -> m.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }

    /** Right-hand status pill text, or null to hide it. */
    protected String shellStatus() {
        return null;
    }

    protected int formWidth() {
        return Math.min(MAX_FORM_WIDTH, width - 32);
    }

    protected int formLeft() {
        return (width - formWidth()) / 2;
    }

    protected StButton button(
            int x,
            int y,
            int width,
            int height,
            String key,
            ButtonWidget.PressAction onPress
    ) {

        return button(x, y, width, height, key, onPress, Ui.ACCENT);
    }

    protected StButton button(
            int x,
            int y,
            int width,
            int height,
            String key,
            ButtonWidget.PressAction onPress,
            int accent
    ) {

        StButton widget =
                new StButton(
                        x,
                        y,
                        width,
                        height,
                        Lang.text(key),
                        onPress,
                        // screen open-time: a rebuild of the widget list
                        // must not replay the entrance animation
                        openTime,
                        nextSlot(),
                        accent
                );

        addDrawableChild(widget);

        return widget;
    }

    protected StToggle toggle(
            int x,
            int y,
            int width,
            int height,
            String labelKey,
            boolean value,
            Consumer<Boolean> onChange
    ) {

        StToggle widget =
                new StToggle(
                        x,
                        y,
                        width,
                        height,
                        labelKey,
                        value,
                        onChange,
                        nextSlot()
                );

        addDrawableChild(widget);

        return widget;
    }

    /**
     * Static darkened backdrop. Overrides the vanilla one because it
     * applies its own animated blur on screen transitions — that was
     * the "jumping background" bug.
     */
    /**
     * When true, renderBackground does nothing. super.render() draws the
     * vanilla background itself; we suppress it there and keep only our
     * own static backdrop drawn outside the animated matrix.
     */
    private boolean suppressBackground;

    @Override
    public void renderBackground(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (suppressBackground) {
            return;
        }

        // the shell paints the whole window background itself
        context.fill(
                0, 0, width, height,
                Ui.argb(Ui.BG, 1f)
        );
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (entranceNeedsStart) {
            openTime = System.currentTimeMillis();
            entranceNeedsStart = false;
        }

        float alpha = entrance();

        // static: the backdrop must NOT animate, otherwise it slides
        // and leaves a gap at the top
        renderBackground(
                context,
                mouseX,
                mouseY,
                delta
        );

        // chrome is drawn outside the entrance matrix so the rail and
        // top bar never slide
        shell.render(
                context,
                mouseX,
                mouseY,
                alpha,
                shellTitle(),
                shellSubtitle(),
                shellStatus(),
                shellTitleAnimation(),
                delta
        );

        context.getMatrices().pushMatrix();

        context.getMatrices().translate(
                0,
                (1 - alpha) * 8
        );

        renderTheme(
                context,
                mouseX,
                mouseY,
                delta,
                alpha
        );

        // super.render() draws widgets AND its own background; the flag
        // suppresses that background so only our static one (drawn
        // above, outside the matrix) remains visible.
        suppressBackground = true;

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        suppressBackground = false;

        renderOverlay(
                context,
                mouseX,
                mouseY,
                delta,
                alpha
        );

        context.getMatrices().popMatrix();

        // the shell tooltip floats above everything, including widgets
        shell.renderTooltip(context, alpha);
    }

    @Override
    public void removed() {
        entranceNeedsStart = true;
        super.removed();
    }

    @Override
    public boolean mouseClicked(
            net.minecraft.client.gui.Click click,
            boolean doubled
    ) {

        // the shell chrome sits above the content and gets the click first
        if (shell.mouseClicked(
                click.x(),
                click.y(),
                click.button()
        )) {

            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    /** Draws titles, labels, rows and other custom content. */
    protected void renderTheme(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {
    }

    /** Draws content on top of widgets (e.g. popups). */
    protected void renderOverlay(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            float alpha
    ) {
    }

    protected void drawTitle(
            DrawContext context,
            String key,
            float alpha
    ) {

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(key),
                width / 2,
                16,
                Ui.argb(Ui.TEXT, alpha)
        );

        // accent underline: a short gradient bar instead of a flat line,
        // so every screen shares the same header language
        Ui.drawGradientH(
                context,
                width / 2 - 55,
                27,
                110,
                2,
                Ui.ACCENT,
                Ui.ACCENT_2,
                alpha
        );
    }

    protected void drawHint(
            DrawContext context,
            String key,
            int y,
            float alpha
    ) {

        context.drawCenteredTextWithShadow(
                textRenderer,
                Lang.text(key),
                width / 2,
                y,
                Ui.argb(Ui.TEXT_DIM, alpha)
        );
    }
}
