package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;

public record PlayerMention(
        PlayerContext player,
        int start,
        int end
) {
}
