package com.codecool.ccsima2.semifinal;

import com.codecool.ccsima2.AbstractMain;
import org.capnproto.ListList;
import org.capnproto.MessageBuilder;
import org.capnproto.StructList;

import java.io.IOException;
import java.util.Scanner;

public class Main extends AbstractMain {

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

    private final Field[][] board = new Field[80][100];

    private Main(String team, String hash, String host, int port) {
        super(team, hash, host, port);
    }

    protected void solve() throws IOException {
        System.out.println("Connecting");
        connectToClient();
        System.out.println("Logging in");
        writeLoginCommand();

        ResponseClass.Response.Reader rr;
        while (true) {
            System.out.println("----------------");
            System.out.println("Reading response");
            rr = readResponse(ResponseClass.Response.factory);

            ResponseClass.Response.Info.Reader ir = rr.getInfo();
            System.out.printf("Level: %s%n", ir.getLevel());
            System.out.printf("Owns: %s%n", ir.getOwns());
            System.out.printf("Tick: %s%n", ir.getTick());

            if (rr.hasUnits()) {
                StructList.Reader<ResponseClass.Unit.Reader> units = rr.getUnits();
                for (int i = 0; i < units.size(); i++) {
                    ResponseClass.Unit.Reader ur = units.get(i);
                    System.out.print("Unit: ");
                    System.out.printf("o=%s, ", ur.getOwner());
                    System.out.printf("h=%s, ", ur.getHealth());
                    System.out.printf("k=%s, ", ur.getKiller());
                    CommonClass.Direction dr = ur.getDirection();
                    System.out.printf("%s ,", dr);
                    if (ur.hasPosition()) {
                        CommonClass.Position.Reader pr = ur.getPosition();
                        int x = pr.getX();
                        int y = pr.getY();
                        System.out.format("(%s,%s)", x, y);

                        board[x][y] = new UnitField(i, pr.getX(), pr.getY(), ur.getDirection());
                    } else {
                        System.out.print("no position");
                    }
                    System.out.println();
                }
            } else {
                System.out.println("No units");
            }

            if (rr.hasEnemies()) {
                StructList.Reader<ResponseClass.Enemy.Reader> enemies = rr.getEnemies();
                for (int i = 0; i < enemies.size(); i++) {
                    ResponseClass.Enemy.Reader er = enemies.get(i);
                    System.out.print("Enemy: ");
                    ResponseClass.Enemy.Direction.Reader dr = er.getDirection();
                    CommonClass.Direction horizontal = dr.getHorizontal();
                    CommonClass.Direction vertical = dr.getVertical();
                    System.out.printf("v=%s,h=%s, ", horizontal, vertical);
                    if (er.hasPosition()) {
                        CommonClass.Position.Reader pr = er.getPosition();
                        int x = pr.getX();
                        int y = pr.getY();
                        System.out.format("(%s,%s)", x, y);

                        board[x][y] = new EnemyField(x, y, horizontal, vertical);
                    } else {
                        System.out.print("no position");
                    }
                    System.out.println();
                }
            } else {
                System.out.println("No enemies");
            }

            if (rr.hasStatus()) {
                System.out.println(rr.getStatus());
            } else {
                System.out.println("No status");
            }

            if (rr.hasCells()) {
                ListList.Reader<StructList.Reader<ResponseClass.Cell.Reader>> cells = rr.getCells();
                for (int i = 0; i < cells.size(); i++) {
                    StructList.Reader<ResponseClass.Cell.Reader> row = cells.get(i);
                    for (int j = 0; j < row.size(); j++) {
                        ResponseClass.Cell.Reader cr = row.get(j);
                        ResponseClass.Cell.Attack.Reader ca = cr.getAttack();
                        if (ca.isUnit()) {
                            System.out.print(ca.getUnit());
                        } else if (ca.isCan()) {
                            String c;
                            if (board[i][j] instanceof UnitField) {
                                c = "@";
                            } else if (board[i][j] instanceof EnemyField) {
                                c = "!";
                            } else if (cr.getOwner() == 0) {
                                c = ca.getCan() ? "+" : "-";
                            } else {
                                c = ca.getCan() ? "_" : "#";
                            }
                            System.out.print(c);
                        }
                    }
                    System.out.println();
                }

                Scanner scanner = new Scanner(System.in);
                String line = scanner.nextLine();

                CommonClass.Direction direction = null;
                switch (line) {
                    case "w":
                        direction = CommonClass.Direction.UP;
                        break;
                    case "s":
                        direction = CommonClass.Direction.DOWN;
                        break;
                    case "a":
                        direction = CommonClass.Direction.LEFT;
                        break;
                    case "d":
                        direction = CommonClass.Direction.RIGHT;
                        break;
                    default:
                        System.out.println("No direction change");
                }

                if (direction != null) {
                    writeMoveCommand(0, direction);
                }

                /*
                for (int i = 0; i < board.length; i++) {
                    if (board[i] == null) {
                        continue;
                    }
                    for (int j = 0; j < board[i].length; j++) {
                        Field f = board[i][j];
                        if (f instanceof UnitField) {
                            UnitField uf = (UnitField) f;
                            if (uf.getDirection() != CommonClass.Direction.RIGHT) {
                                writeMoveCommand(uf.getId(), CommonClass.Direction.RIGHT);
                            }
                        }
                    }
                }
                */
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
