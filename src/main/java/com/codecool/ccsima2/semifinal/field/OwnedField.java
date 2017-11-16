package com.codecool.ccsima2.semifinal.field;

import com.googlecode.lanterna.TextColor;

public abstract class OwnedField extends Field {

    private final int owner;

    OwnedField(char symbol, int x, int y, int owner) {
        super(symbol, x, y);
        this.owner = owner;
    }

    OwnedField(char symbol, TextColor.ANSI fg, int x, int y, int owner) {
        super(symbol, fg, x, y);
        this.owner = owner;
    }

    OwnedField(char symbol, TextColor.ANSI fg, TextColor.ANSI bg, int x, int y, int owner) {
        super(symbol, fg, bg, x, y);
        this.owner = owner;
    }

    public int getOwner() {
        return owner;
    }
}
