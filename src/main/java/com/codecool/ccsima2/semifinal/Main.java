package com.codecool.ccsima2.semifinal;

import com.codecool.ccsima2.AbstractMain;
import com.codecool.ccsima2.semifinal.field.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import org.capnproto.ListList;
import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main extends AbstractMain {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int UNIT_ID = 0;
    private static final int TEAM_ID = 1;
    private static final int ROWS = 80;
    private static final int COLUMNS = 100;

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

    protected void solve() throws IOException {
        LOG.info("Connecting");
        connectToClient();
        LOG.info("Logging in");
        writeLoginCommand();

        TerminalScreen screen = createDisplay();
        Field[][] board = new Field[ROWS][COLUMNS];
        List<UnitField> units = new ArrayList<>();
        List<EnemyField> enemies = new ArrayList<>();
        do {
            clearFields(board);
            units.clear();
            enemies.clear();
            LOG.info("Reading response");
            ResponseClass.Response.Reader responseReader = readResponse(ResponseClass.Response.factory);
            updateInfoAndShowStatus(responseReader);
            fillTerrainFields(board, responseReader);
            fillEnemyFields(board, enemies, responseReader);
            fillUnitFields(board, units, responseReader);
            checkNullOrUnknownFields(board);
            calcTerritory(board);
            sortUnitsById(units);
            List<CommonClass.Direction> directions = calcUnitDirections(board, units, enemies, screen);
            displayFields(board, screen);
            sendUnitMoveReply(directions);
        } while (true);
    }

    private TerminalScreen createDisplay() throws IOException {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(COLUMNS, ROWS));
        final Terminal terminal = factory.createTerminal();
        if (terminal instanceof SwingTerminalFrame) {
            SwingTerminalFrame frame = (SwingTerminalFrame) terminal;
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.setCursorPosition(null);
        screen.startScreen();
        return screen;
    }

    private void clearFields(Field[][] board) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                board[i][j] = null;
            }
        }
    }

    private void updateInfoAndShowStatus(ResponseClass.Response.Reader reader) throws IOException {
        ResponseClass.Response.Info.Reader infoReader = reader.getInfo();

        LOG.info("Level: {}", infoReader.getLevel());
        LOG.info("Owns: {}", infoReader.getOwns());
        LOG.info("Tick: {}", infoReader.getTick());

        if (reader.hasStatus()) {
            String s = reader.getStatus().toString();
            LOG.info("Status: {}", s);
            if (s.contains("You are already logged in")) {
                LOG.warn(s);
                System.exit(0);
            }
        } else {
            LOG.info("Status: none");
        }
    }

    private void fillTerrainFields(Field[][] board, ResponseClass.Response.Reader responseReader) {
        if (responseReader.hasCells()) {
            ListList.Reader<StructList.Reader<ResponseClass.Cell.Reader>> cellsReader = responseReader.getCells();
            for (int i = 0; i < cellsReader.size(); i++) {
                StructList.Reader<ResponseClass.Cell.Reader> rowReader = cellsReader.get(i);
                for (int j = 0; j < rowReader.size(); j++) {
                    ResponseClass.Cell.Reader cellReader = rowReader.get(j);
                    ResponseClass.Cell.Attack.Reader attackReader = cellReader.getAttack();

                    int owner = cellReader.getOwner();
                    Field field = new UnknownField(i, j, owner);

                    if (attackReader.isUnit()) {
                        int unit = attackReader.getUnit();
                        if (owner != TEAM_ID && unit == UNIT_ID) {
                            field = new TrailField(TextColor.ANSI.GREEN, i, j, owner);
                        }
                    } else if (attackReader.isCan()) {
                        boolean can = attackReader.getCan();
                        if (can) {
                            if (owner == 0) {
                                field = new SeaField(i, j, owner);
                            } else {
                                field = new LandField(TextColor.ANSI.GREEN, i, j, owner);
                            }
                        } else {
                            if (owner == TEAM_ID) {
                                field = new SafeField(i, j, owner);
                            }
                        }
                    } else {
                        throw new IllegalStateException();
                    }

                    board[i][j] = field;
                }
            }
        }
    }

    private void fillUnitFields(Field[][] board, List<UnitField> units, ResponseClass.Response.Reader responseReader) {
        if (responseReader.hasUnits()) {
            StructList.Reader<ResponseClass.Unit.Reader> unitsReader = responseReader.getUnits();
            for (int i = 0; i < unitsReader.size(); i++) {
                ResponseClass.Unit.Reader unitReader = unitsReader.get(i);
                int owner = unitReader.getOwner();
                int health = unitReader.getHealth();
                int killer = unitReader.getKiller();
                CommonClass.Direction direction = unitReader.getDirection();
                if (unitReader.hasPosition()) {
                    CommonClass.Position.Reader positionReader = unitReader.getPosition();
                    int row = positionReader.getX();
                    int col = positionReader.getY();
                    LOG.info("Unit: owner={},health={}, killer={}, direction={}, row,col=({},{})",
                            owner,
                            health,
                            killer,
                            direction,
                            row,
                            col);

                    UnitField field = new UnitField(TextColor.ANSI.GREEN, row, col, UNIT_ID, direction);
                    board[row][col] = field;
                    units.add(field);
                } else {
                    LOG.info("Unit: owner={},health={}, killer={}, direction={}, row,col=no position",
                            owner,
                            health,
                            killer,
                            direction);
                }
            }
        } else {
            LOG.info("No units");
        }
    }

    private void fillEnemyFields(Field[][] board, List<EnemyField> enemies, ResponseClass.Response.Reader responseReader) {
        if (responseReader.hasEnemies()) {
            StructList.Reader<ResponseClass.Enemy.Reader> enemiesReader = responseReader.getEnemies();
            for (int i = 0; i < enemiesReader.size(); i++) {
                ResponseClass.Enemy.Reader enemyReader = enemiesReader.get(i);
                ResponseClass.Enemy.Direction.Reader directionReader = enemyReader.getDirection();
                CommonClass.Direction horizontal = directionReader.getHorizontal();
                CommonClass.Direction vertical = directionReader.getVertical();
                if (enemyReader.hasPosition()) {
                    CommonClass.Position.Reader positionReader = enemyReader.getPosition();
                    int x = positionReader.getX();
                    int y = positionReader.getY();
                    LOG.info("Enemy: vertical={},horizontal={}, row,col=({},{})", horizontal, vertical, x, y);
                    EnemyField field = new EnemyField(TextColor.ANSI.RED, x, y, horizontal, vertical);
                    board[x][y] = field;
                    enemies.add(field);
                } else {
                    LOG.info("Enemy: vertical={},horizontal={}, row,col=no position", horizontal, vertical);
                }
            }
        } else {
            LOG.info("No enemies");
        }
    }

    private void checkNullOrUnknownFields(Field[][] board) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                Field field = board[i][j];
                if (field == null) {
                    LOG.error("null field at row,col=({},{})", i, j);
                } else if (field instanceof UnknownField) {
                    LOG.error("Unknown field at row,col=({},{})", i, j);
                }
            }
        }
    }

    private void calcTerritory(Field[][] board) {
        int total = ROWS * COLUMNS;
        int current = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                Field field = board[i][j];
                if (field instanceof OwnedField) {
                    OwnedField ownedField = (OwnedField) field;
                    if (TEAM_ID == ownedField.getOwner()) {
                        current++;
                    }
                }
            }
        }
        LOG.info("Captured territory: {}%", ((double) current / (double) total) * 100.0d);
    }

    private void sortUnitsById(List<UnitField> units) {
        units.sort(Comparator.comparingInt(UnitField::getId));
    }

    private List<CommonClass.Direction> calcUnitDirections(Field[][] board, List<UnitField> units, List<EnemyField> enemies, TerminalScreen screen) throws IOException {
        List<CommonClass.Direction> directions = new ArrayList<>();

        for (UnitField unit : units) {
            CommonClass.Direction direction = unit.getDirection();

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
                        LOG.info("Unknown keystroke: {}", keyStroke.getCharacter());
                        break;
                }
            }

            directions.add(makeSureMoveIsSafe(unit, direction));
        }

        return directions;
    }

    private CommonClass.Direction makeSureMoveIsSafe(UnitField unit, CommonClass.Direction direction) {
        int row = unit.getRow();
        int column = unit.getColumn();
        CommonClass.Direction oppositeDirection;
        switch (direction) {
            case UP:
                oppositeDirection = CommonClass.Direction.DOWN;
                row--;
                break;
            case DOWN:
                oppositeDirection = CommonClass.Direction.UP;
                row++;
                break;
            case LEFT:
                oppositeDirection = CommonClass.Direction.RIGHT;
                column--;
                break;
            case RIGHT:
                oppositeDirection = CommonClass.Direction.LEFT;
                column++;
                break;
            default:
                throw new IllegalStateException();
        }
        boolean safe = row >= 0 && column >= 0 && row < ROWS && column < COLUMNS;
        if (safe) {
            return direction;
        }
        LOG.warn("Unit: id={} would fall off the board, move ({}) not sent, sending opposite direction ({})", unit.getId(), direction, oppositeDirection);
        return oppositeDirection;
    }

    private void sendUnitMoveReply(List<CommonClass.Direction> directions) throws IOException {
        for (int i = 0; i < directions.size(); i++) {
            CommonClass.Direction direction = directions.get(i);
            writeMoveCommand(i, direction);
        }
    }

    private void displayFields(Field[][] board, TerminalScreen screen) throws IOException {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                TextColor fg = TextColor.ANSI.DEFAULT;
                Field field = board[i][j];
                screen.setCharacter(j, i, new TextCharacter(field.getSymbol(), field.getFg(), field.getBg()));
            }
        }

        screen.doResizeIfNecessary();
        screen.refresh();
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
