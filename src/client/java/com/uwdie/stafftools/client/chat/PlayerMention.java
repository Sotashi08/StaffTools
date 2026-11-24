package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;

/**
 * A range of a chat message that belongs to a player.
 *
 * @param highlight whether the range may be recoloured. A nick found
 *                  through the real account name is repainted in the
 *                  mod's colour so it stands out in a wall of text,
 *                  while the global nick-highlighting setting can
 *                  remove that repaint. Either kind of mention remains clickable.
 */
public record PlayerMention(
        PlayerContext player,
        int start,
        int end,
        boolean highlight
) {

    public PlayerMention(
            PlayerContext player,
            int start,
            int end
    ) {

        this(player, start, end, true);
    }
}