package com.codecool.ccsima2.semifinal.field;

import com.googlecode.lanterna.TextColor;

public abstract class Field {

    private char symbol;
    private TextColor.ANSI fg;
    private TextColor.ANSI bg;
    private final int row;
    private final int column;

    Field(char symbol, int row, int column) {
        this(symbol, TextColor.ANSI.DEFAULT, row, column);
    }

    Field(char symbol, TextColor.ANSI fg, int row, int column) {
        this(symbol, fg, TextColor.ANSI.DEFAULT, row, column);
    }

    Field(char symbol, TextColor.ANSI fg, TextColor.ANSI bg, int row, int column) {
        this.symbol = symbol;
        this.fg = fg;
        this.bg = bg;
        this.row = row;
        this.column = column;
    }

    public char getSymbol() {
        return symbol;
    }

    public TextColor.ANSI getFg() {
        return fg;
    }

    public TextColor.ANSI getBg() {
        return bg;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
