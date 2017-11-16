package com.codecool.ccsima2.semifinal.field;

import com.codecool.ccsima2.semifinal.CommonClass;
import com.googlecode.lanterna.TextColor;

public final class UnitField extends Field {

    private final int id;
    private final CommonClass.Direction direction;

    public UnitField(TextColor.ANSI fg, int x, int y, int id, CommonClass.Direction direction) {
        super('@', fg, x, y);
        this.id = id;
        this.direction = direction;
    }

    public int getId() {
        return id;
    }

    public CommonClass.Direction getDirection() {
        return direction;
    }
}
