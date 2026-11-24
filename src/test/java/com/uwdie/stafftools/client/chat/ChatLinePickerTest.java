package com.uwdie.stafftools.client.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatLinePickerTest {

    @Test
    void mapsPixelPositionToTheGlyphUnderTheCursor() {

        String line = "uwdie pun EternalOfTweak";
        int[] glyphWidths = new int[line.length()];
        Arrays.fill(glyphWidths, 6);

        int offset = ChatLinePicker.characterAt(
                line,
                12.5,
                glyphWidths,
                glyphWidths.length
        );

        assertEquals(2, offset);
        assertEquals('d', line.charAt(offset));
    }

    @Test
    void returnsUtf16OffsetForSupplementaryCharacters() {

        String line = "uw\uD83D\uDE00die";
        int[] glyphWidths = {6, 6, 12, 6, 6};

        int offset = ChatLinePicker.characterAt(
                line,
                24.5,
                glyphWidths,
                glyphWidths.length
        );

        assertEquals(4, offset);
        assertEquals('d', line.charAt(offset));
    }

    @Test
    void rejectsClicksOutsideRenderedGlyphs() {

        String line = "uwdie";
        int[] glyphWidths = {6, 6, 6, 6, 6};

        assertEquals(
                -1,
                ChatLinePicker.characterAt(
                        line,
                        -0.5,
                        glyphWidths,
                        glyphWidths.length
                )
        );
        assertEquals(
                -1,
                ChatLinePicker.characterAt(
                        line,
                        30.0,
                        glyphWidths,
                        glyphWidths.length
                )
        );
    }
}
