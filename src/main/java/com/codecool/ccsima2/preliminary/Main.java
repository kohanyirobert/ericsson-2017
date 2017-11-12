package com.codecool.ccsima2.preliminary;

import com.codecool.ccsima2.AbstractMain;
import org.capnproto.MessageBuilder;
import org.capnproto.Text;

import java.io.IOException;

public class Main extends AbstractMain {

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
            rr = readResponse(ResponseClass.Response.factory);

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
}
