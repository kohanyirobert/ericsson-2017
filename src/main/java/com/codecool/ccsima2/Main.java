package com.codecool.ccsima2;

import com.codecool.ccsima2.preliminary.BugfixClass;
import com.codecool.ccsima2.preliminary.RequestClass;
import com.codecool.ccsima2.preliminary.ResponseClass;
import org.capnproto.MessageBuilder;
import org.capnproto.Serialize;
import org.capnproto.Text;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class Main {

    public static void main(String[] args) throws IOException {
        String team = args[0];
        String hash = args[1];
        String host = args[2];
        int port = Integer.valueOf(args[3]);
        new Main(team, hash, host, port).solveBugs();
    }

    private final String team;
    private final String hash;
    private final String host;
    private final int port;
    private final SocketAddress addr;

    private SocketChannel sc;

    private Main(String team, String hash, String host, int port) {
        this.team = team;
        this.hash = hash;
        this.host = host;
        this.port = port;

        addr = new InetSocketAddress(host, port);
    }

    private void solveBugs() throws IOException {
        System.out.println("Connecting");
        connectToClient();
        System.out.println("Logging in");
        writeLoginRequest();

        int bugsToFix = 0;
        int bugsFixed = 0;
        boolean noMoreBugs = false;

        ResponseClass.Response.Reader rr;
        do {
            System.out.println("----------------");
            System.out.println("Reading response");
            rr = readResponse();

            if (rr.getEnd()) {
                System.out.println("We're done");
            } else {
                System.out.println("Not done yet");
            }

            if (rr.hasStatus()) {
                Text.Reader sr = rr.getStatus();
                String status = sr.toString();
                if (status.contains("You fixed all the bugs!")) {
                    noMoreBugs = true;
                }
                System.out.printf("Got status: %s%n", status);
            } else {
                System.out.println("No status");
            }

            if (rr.hasBugfix()) {
                BugfixClass.Bugfix.Reader br = rr.getBugfix();
                if (br.hasMessage()) {
                    Text.Reader mr = br.getMessage();
                    System.out.printf("Got bugfix message: %s%n", mr.toString());
                } else {
                    System.out.println("No bugfix message");
                }


                bugsToFix = br.getBugs();
                System.out.printf("Bugs to fix: %s%n", bugsToFix);
            } else {
                System.out.println("No bugfix");
            }

            if (noMoreBugs) {
                System.out.printf("Phew, fixed %s%n", bugsFixed);

                writeBugfixRequest("I solved a huge amount of bug. I am proud of myself.", bugsFixed);
            } else if (bugsToFix >= 0) {
                System.out.printf("Still fixing bugsToFix (%s remaining)%n", bugsToFix);

                writeBugfixRequest("Fixed", --bugsToFix);
                bugsFixed++;
            } else {
                throw new IllegalStateException();
            }
        } while (!rr.getEnd());
    }

    private void connectToClient() throws IOException {
        sc = SocketChannel.open(addr);
    }

    private void writeBugfixRequest(String message, int bugs) throws IOException {
        MessageBuilder mb = new MessageBuilder();
        RequestClass.Request.Builder rb = mb.initRoot(RequestClass.Request.factory);

        BugfixClass.Bugfix.Builder bb = rb.initBugfix();
        bb.setMessage(message);
        bb.setBugs((byte) bugs);

        writeRequest(mb);
    }

    private void writeLoginRequest() throws IOException {
        MessageBuilder mb = new MessageBuilder();
        RequestClass.Request.Builder rb = mb.initRoot(RequestClass.Request.factory);
        RequestClass.Request.Login.Builder lb = rb.initLogin();
        lb.setTeam(team);
        lb.setHash(hash);

        writeRequest(mb);
    }

    private void writeRequest(MessageBuilder mb) throws IOException {
        Serialize.write(sc, mb);
    }

    private ResponseClass.Response.Reader readResponse() throws IOException {
        return Serialize.read(sc).getRoot(ResponseClass.Response.factory);
    }
}
