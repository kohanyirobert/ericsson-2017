package com.codecool.ccsima2.semifinal.field;

import com.googlecode.lanterna.TextColor;

public final class LandField extends OwnedField {

    public LandField(TextColor.ANSI fg, int x, int y, int owner) {
        super('~', fg, x, y, owner);
    }
}
