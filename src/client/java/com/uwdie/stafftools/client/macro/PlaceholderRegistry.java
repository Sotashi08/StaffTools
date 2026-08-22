package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.i18n.Lang;

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
                Lang.Key.PH_PLAYER,
                context ->
                        context.get("player")
        );

        register(
                "staff",
                Lang.Key.PH_STAFF,
                context ->
                        context.get("staff")
        );

        register(
                "x",
                Lang.Key.PH_X,
                context ->
                        context.get("x")
        );

        register(
                "y",
                Lang.Key.PH_Y,
                context ->
                        context.get("y")
        );

        register(
                "z",
                Lang.Key.PH_Z,
                context ->
                        context.get("z")
        );

        register(
                "ping",
                Lang.Key.PH_PING,
                context ->
                        context.get("ping")
        );

        register(
                "server",
                Lang.Key.PH_SERVER,
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
