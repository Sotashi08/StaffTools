package com.uwdie.stafftools.client.macro;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlaceholderRegistry {

    private static final Map<String, Placeholder>
            PLACEHOLDERS =
            new LinkedHashMap<>();

    static {

        register(
                "player",
                "Target player name",
                context ->
                        context.get("player")
        );

        register(
                "staff",
                "Your player name",
                context ->
                        context.get("staff")
        );

        register(
                "x",
                "Your X coordinate",
                context ->
                        context.get("x")
        );

        register(
                "y",
                "Your Y coordinate",
                context ->
                        context.get("y")
        );

        register(
                "z",
                "Your Z coordinate",
                context ->
                        context.get("z")
        );

        register(
                "ping",
                "Target player latency (ms)",
                context ->
                        context.get("ping")
        );

        register(
                "health",
                "Your health (hearts x2)",
                context ->
                        context.get("health")
        );

        register(
                "server",
                "Current server address",
                context ->
                        context.get("server")
        );
    }

    private PlaceholderRegistry() {
    }

    public static void register(
            String name,
            String description,
            java.util.function.Function<
                    MacroContext,
                    String
                    > resolver
    ) {

        PLACEHOLDERS.put(
                name,
                new Placeholder(
                        name,
                        description,
                        resolver
                )
        );
    }

    public static Placeholder get(
            String name
    ) {

        return PLACEHOLDERS.get(name);
    }

    public static Collection<Placeholder>
    getAll() {

        return PLACEHOLDERS.values();
    }
}
