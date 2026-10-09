package com.uwdie.stafftools.client.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the chat input field so the actions popup can insert a
 * player's nick straight into the message being typed.
 */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {

    @Accessor("chatField")
    TextFieldWidget stafftools$getChatField();
}
