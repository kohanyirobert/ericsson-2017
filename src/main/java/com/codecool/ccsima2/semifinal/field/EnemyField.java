package com.codecool.ccsima2.semifinal.field;

import com.codecool.ccsima2.semifinal.CommonClass;
import com.googlecode.lanterna.TextColor;

public final class EnemyField extends Field {

    private final CommonClass.Direction horizontal;
    private final CommonClass.Direction vertical;

    public EnemyField(TextColor.ANSI fg, int x, int y, CommonClass.Direction horizontal, CommonClass.Direction vertical) {
        super('@', fg, x, y);
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public CommonClass.Direction getHorizontal() {
        return horizontal;
    }

    public CommonClass.Direction getVertical() {
        return vertical;
    }
}
