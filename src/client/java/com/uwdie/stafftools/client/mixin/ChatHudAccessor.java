package com.uwdie.stafftools.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Exposes the layout internals of the chat hud, so the mod can tell
 * which rendered chat line (and which word inside it) the cursor is
 * over — the same math vanilla uses for click events.
 *
 * Since 1.21.11 {@code ChatHud} keeps its scale, width and scroll
 * offset private and no longer offers the line lookup as a method, all
 * three are pulled in here.
 */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> stafftools$getVisibleMessages();

    @Accessor("scrolledLines")
    int stafftools$getScrolledLines();

    @Invoker("getChatScale")
    double stafftools$getChatScale();

    @Invoker("getWidth")
    int stafftools$getWidth();

    @Invoker("getLineHeight")
    int stafftools$getLineHeight();
}
