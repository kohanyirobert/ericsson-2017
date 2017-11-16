package com.codecool.ccsima2.semifinal.field;

import com.googlecode.lanterna.TextColor;

public final class TrailField extends OwnedField {

    public TrailField(TextColor.ANSI fg, int x, int y, int owner) {
        super('*', fg, x, y, owner);
    }
}
