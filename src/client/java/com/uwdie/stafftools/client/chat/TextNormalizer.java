package com.uwdie.stafftools.client.chat;

import java.util.HashMap;
import java.util.Map;

/**
 * Lore-friendly nick normalisation.
 *
 * Chat plugins love to spell a player's nick with look-alike characters
 * (fullwidth, small caps, Cyrillic/Greek letters), which breaks naive
 * equality with the real nick. {@link #unicodeFold(String)} builds a
 * cleaned copy of the text with the length preserved, so a match found
 * in the copy still maps to the exact range the player sees.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Groups of look-alike chars and their plain ASCII counterparts. */
    private static final String[][] FOLDS = {

            // Greek uppercase
            {
                    "\u0391\u0392\u0395\u0397\u0399\u039A\u039C\u039D\u039F"
                            + "\u03A1\u03A4\u03A5\u03A7",
                    "ABEHIKMNOPTYX"
            },

            // Cyrillic uppercase / lowercase
            {
                    "\u0410\u0412\u0415\u041A\u041C\u041D\u041E\u0420\u0421"
                            + "\u0422\u0423\u0425",
                    "ABEKMHOPCTYX"
            },

            {
                    "\u0430\u0435\u043E\u0440\u0441\u0443\u0445",
                    "aeopcyx"
            },

            // fullwidth latin
            {
                    "\uFF21\uFF22\uFF23\uFF24\uFF25\uFF26\uFF27\uFF28\uFF29"
                            + "\uFF2A\uFF2B\uFF2C\uFF2D\uFF2E\uFF2F\uFF30\uFF31"
                            + "\uFF32\uFF33\uFF34\uFF35\uFF36\uFF37\uFF38\uFF39"
                            + "\uFF3A",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            },

            {
                    "\uFF41\uFF42\uFF43\uFF44\uFF45\uFF46\uFF47\uFF48\uFF49"
                            + "\uFF4A\uFF4B\uFF4C\uFF4D\uFF4E\uFF4F\uFF50\uFF51"
                            + "\uFF52\uFF53\uFF54\uFF55\uFF56\uFF57\uFF58\uFF59"
                            + "\uFF5A",
                    "abcdefghijklmnopqrstuvwxyz"
            },

            // small caps ("q", "s", "x" are already plain ASCII there)
            {
                    "\u1D00\u0299\u1D04\u1D05\u1D07\uAB30\u0262\u029C\u026A"
                            + "\u1D0A\u1D0B\u029F\u1D0D\u0274\u1D0F\u1D18\u0280"
                            + "\u1D1B\u1D1C\u1D20\u1D21\u028F\u1D22",
                    "abcdefghijklmnoprstuvwyz"
            },

            // superscript / subscript digits
            {
                    "\u2070\u00B9\u00B2\u00B3\u2074\u2075\u2076\u2077\u2078"
                            + "\u2079",
                    "0123456789"
            },

            {
                    "\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088"
                            + "\u2089",
                    "0123456789"
            }
    };

    private static final Map<Character, Character> FOLD_MAP =
            new HashMap<>();

    static {

        for (String[] fold : FOLDS) {

            if (fold[0].length() != fold[1].length()) {

                // a bad row must never take the whole client down:
                // skip it and keep the rest of the table working
                System.err.println(
                        "[StaffTools] Skipping malformed lookalike group."
                );

                continue;
            }

            for (int i = 0; i < fold[0].length(); i++) {
                FOLD_MAP.put(
                        fold[0].charAt(i),
                        fold[1].charAt(i)
                );
            }
        }
    }

    /** Char is one of the letters/digits/underscore we can match on. */
    public static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Maps every look-alike char to its plain ASCII twin and removes
     * zero-width marks. Length is preserved, so indices never shift.
     */
    public static String unicodeFold(String source) {

        char[] out = new char[source.length()];

        for (int i = 0; i < source.length(); i++) {

            char c = source.charAt(i);

            Character folded = FOLD_MAP.get(c);

            out[i] = folded != null ? folded : c;
        }

        return new String(out);
    }

    /**
     * Replaces every char that is not part of a name with a space.
     * Length is preserved, so a match maps straight back to the
     * original string — this is what makes "{@code [VIP]Notch★}"
     * match the plain nick "Notch".
     */
    public static String decorationFold(String source) {

        char[] out = new char[source.length()];

        for (int i = 0; i < source.length(); i++) {

            char c = source.charAt(i);

            out[i] = isNameChar(c) ? c : 32;
        }

        return new String(out);
    }
}
