package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;

/**
 * A range of a chat message that belongs to a player.
 *
 * @param highlight whether the range may be recoloured. A nick found
 *                  through the real account name is repainted in the
 *                  mod's colour so it stands out in a wall of text,
 *                  while a nick that is only known through a changed
 *                  /nick is a normal word of somebody's message:
 *                  repainting it would turn arbitrary words of the
 *                  chat yellow, so those keep the server's colour and
 *                  only become clickable.
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