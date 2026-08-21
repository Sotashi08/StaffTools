package com.uwdie.stafftools.client.player;

import java.util.UUID;

public record PlayerContext(
        String name,
        UUID uuid
) {
}
