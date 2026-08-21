package com.uwdie.stafftools.client.macro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderEngine {

    private static final Pattern PATTERN =
            Pattern.compile("<([a-zA-Z0-9_]+)>");

    private PlaceholderEngine() {
    }

    public static String resolve(
            String input,
            MacroContext context
    ) {

        if (input == null) {
            return "";
        }

        Matcher matcher =
                PATTERN.matcher(input);

        StringBuffer result =
                new StringBuffer();

        while (matcher.find()) {

            String name =
                    matcher.group(1);

            Placeholder placeholder =
                    PlaceholderRegistry.get(name);

            String replacement;

            if (placeholder != null) {

                replacement =
                        placeholder
                                .resolver()
                                .apply(context);

            } else {

                replacement =
                        matcher.group(0);
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            replacement
                    )
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }
}
