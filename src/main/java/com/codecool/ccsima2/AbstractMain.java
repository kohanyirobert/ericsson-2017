package com.codecool.ccsima2;

import org.capnproto.FromPointerReader;
import org.capnproto.MessageBuilder;
import org.capnproto.SerializePacked;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public abstract class AbstractMain {

    protected final String team;
    protected final String hash;

    private final String host;
    private final int port;
    private final SocketAddress addr;

    private SocketChannel sc;

    protected AbstractMain(String team, String hash, String host, int port) {
        this.team = team;
        this.hash = hash;
        this.host = host;
        this.port = port;

        addr = new InetSocketAddress(host, port);
    }

    protected abstract void solve() throws IOException, InterruptedException;

    protected void connectToClient() throws IOException {
        sc = SocketChannel.open(addr);
    }

    protected void writeRequest(MessageBuilder mb) throws IOException {
        SerializePacked.writeToUnbuffered(sc, mb);
    }

    protected <T> T readResponse(FromPointerReader<T> factory) throws IOException {
        return SerializePacked.readFromUnbuffered(sc).getRoot(factory);
    }
}
