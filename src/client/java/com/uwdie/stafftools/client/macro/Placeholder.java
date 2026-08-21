package com.uwdie.stafftools.client.macro;

import java.util.function.Function;

public record Placeholder(
        String name,
        String description,
        Function<MacroContext, String> resolver
) {
}