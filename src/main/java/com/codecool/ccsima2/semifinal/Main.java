package com.codecool.ccsima2.semifinal;

import com.codecool.ccsima2.AbstractMain;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.capnproto.ListList;
import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main extends AbstractMain {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int WIDTH = 80;
    private static final int HEIGHT = 100;

    private static abstract class Field {

        private final int x;
        private final int y;

        Field(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private static final class UnitField extends Field {

        private final int id;
        private final CommonClass.Direction direction;

        UnitField(int id, int x, int y, CommonClass.Direction direction) {
            super(x, y);
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

    private static final class EnemyField extends Field {

        private final CommonClass.Direction horizontal;
        private final CommonClass.Direction vertical;

        EnemyField(int x, int y, CommonClass.Direction horizontal, CommonClass.Direction vertical) {
            super(x, y);
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

    public static void main(String[] args) throws IOException {
        String team = args[0];
        String hash = args[1];
        String host = args[2];
        int port = Integer.valueOf(args[3]);
        new Main(team, hash, host, port).solve();
    }

    private Main(String team, String hash, String host, int port) {
        super(team, hash, host, port);
    }

    private <T extends Field> boolean has(List<T> fields, int x, int y) {
        for (T field : fields) {
            // need to switch x and y for some reason
            if (y == field.getX() && x == field.getY()) {
                return true;
            }
        }
        return false;
    }

    protected void solve() throws IOException {
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Terminal terminal = defaultTerminalFactory.createTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.setCursorPosition(null);
        screen.startScreen();

        LOG.info("Connecting");
        connectToClient();
        LOG.info("Logging in");
        writeLoginCommand();

        List<UnitField> currentUnits = new ArrayList<>();
        List<EnemyField> currentEnemies = new ArrayList<>();

        ResponseClass.Response.Reader rr;
        while (true) {
            currentUnits.clear();
            currentEnemies.clear();

            LOG.info("----------------");
            LOG.info("Reading response");
            rr = readResponse(ResponseClass.Response.factory);

            ResponseClass.Response.Info.Reader ir = rr.getInfo();
            LOG.info("Level: {}", ir.getLevel());
            LOG.info("Owns: {}", ir.getOwns());
            LOG.info("Tick: {}", ir.getTick());

            if (rr.hasUnits()) {
                StructList.Reader<ResponseClass.Unit.Reader> units = rr.getUnits();
                for (int i = 0; i < units.size(); i++) {
                    ResponseClass.Unit.Reader ur = units.get(i);
                    CommonClass.Direction dr = ur.getDirection();
                    if (ur.hasPosition()) {
                        CommonClass.Position.Reader pr = ur.getPosition();
                        int x = pr.getX();
                        int y = pr.getY();
                        LOG.info("Unit o={},h={}, k={}, {}, x,y=({},{})",
                                ur.getOwner(),
                                ur.getHealth(),
                                ur.getKiller(),
                                ur.getDirection(),
                                x,
                                y);
                        currentUnits.add(new UnitField(i, pr.getX(), pr.getY(), ur.getDirection()));
                    } else {
                        System.out.print("no position");
                    }
                    LOG.info("");
                }
            } else {
                LOG.info("No units");
            }

            if (rr.hasEnemies()) {
                StructList.Reader<ResponseClass.Enemy.Reader> enemies = rr.getEnemies();
                for (int i = 0; i < enemies.size(); i++) {
                    ResponseClass.Enemy.Reader er = enemies.get(i);
                    ResponseClass.Enemy.Direction.Reader dr = er.getDirection();
                    CommonClass.Direction horizontal = dr.getHorizontal();
                    CommonClass.Direction vertical = dr.getVertical();
                    if (er.hasPosition()) {
                        CommonClass.Position.Reader pr = er.getPosition();
                        int x = pr.getX();
                        int y = pr.getY();
                        LOG.info("Enemy: v={},h={}, x,y=({},{})", horizontal, vertical, x, y);

                        currentEnemies.add(new EnemyField(x, y, horizontal, vertical));
                    } else {
                        LOG.info("Enemy: no position");
                    }
                    LOG.info("");
                }
            } else {
                LOG.info("No enemies");
            }

            if (rr.hasStatus()) {
                LOG.info("Status: " + rr.getStatus());
            } else {
                LOG.info("No status");
            }

            if (rr.hasCells()) {
                ListList.Reader<StructList.Reader<ResponseClass.Cell.Reader>> cells = rr.getCells();
                OUTER:
                for (int i = 0; i < cells.size(); i++) {
                    StructList.Reader<ResponseClass.Cell.Reader> row = cells.get(i);
                    INNER:
                    for (int j = 0; j < row.size(); j++) {
                        ResponseClass.Cell.Reader cr = row.get(j);
                        ResponseClass.Cell.Attack.Reader ca = cr.getAttack();

                        int x = j;
                        int y = i;

                        char c = '?';
                        TextColor fg = TextColor.ANSI.DEFAULT;
                        TextColor bg = TextColor.ANSI.DEFAULT;

                        if (has(currentUnits, x, y)) {
                            c = '@';
                            fg = TextColor.ANSI.GREEN;
                        } else if (has(currentEnemies, x, y)) {
                            c = '@';
                            fg = TextColor.ANSI.RED;
                        } else {
                            if (ca.isUnit()) {
                                int owner = cr.getOwner();
                                if (owner == 0) {
                                    c = '@';
                                    fg = TextColor.ANSI.GREEN;
                                } else {
                                    c = 'Y';
                                }
                            } else if (ca.isCan()) {
                                int owner = cr.getOwner();
                                boolean can = ca.getCan();
                                if (owner == 0) {
                                    if (can) {
                                        c = ' ';
                                    } else {
                                        c = '?';
                                    }
                                } else {
                                    if (can) {
                                        c = '~';
                                    } else {
                                        c = '+';
                                    }
                                }
                            } else {
                                throw new IllegalStateException();
                            }
                        }

                        screen.setCharacter(x, y, new TextCharacter(c, fg, bg));
                    }
                    screen.refresh();
                    screen.doResizeIfNecessary();
                }

                CommonClass.Direction direction = null;
                KeyStroke keyStroke = screen.pollInput();
                if (keyStroke != null && keyStroke.getKeyType() == KeyType.Character) {
                    switch (keyStroke.getCharacter()) {
                        case 'w':
                            direction = CommonClass.Direction.UP;
                            break;
                        case 's':
                            direction = CommonClass.Direction.DOWN;
                            break;
                        case 'a':
                            direction = CommonClass.Direction.LEFT;
                            break;
                        case 'd':
                            direction = CommonClass.Direction.RIGHT;
                            break;
                        default:
                            LOG.info("No direction change");
                    }
                }

                if (direction != null) {
                    if (currentUnits.size() > 1) {
                        throw new IllegalStateException();
                    }
                    writeMoveCommand(0, direction);
                }
            }
        }
    }

    private void writeMoveCommand(int unit, CommonClass.Direction direction) throws IOException {
        MessageBuilder mb = new MessageBuilder();
        CommandClass.Command.Builder rb = mb.initRoot(CommandClass.Command.factory);
        CommandClass.Command.Commands.Builder cb = rb.initCommands();

        StructList.Builder<CommandClass.Move.Builder> mlb = cb.initMoves(1);
        CommandClass.Move.Builder m = mlb.get(0);
        m.setUnit(unit);
        m.setDirection(direction);

        writeRequest(mb);
    }

    private void writeLoginCommand() throws IOException {
        MessageBuilder mb = new MessageBuilder();
        CommandClass.Command.Builder rb = mb.initRoot(CommandClass.Command.factory);
        CommandClass.Command.Commands.Builder cb = rb.initCommands();
        CommandClass.Command.Commands.Login.Builder lb = cb.initLogin();
        lb.setTeam(team);
        lb.setHash(hash);

        writeRequest(mb);
    }
}
