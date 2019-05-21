package ru.ifmo.rain.shelepov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService pool;
    private ExecutorService listener;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            pool = Executors.newFixedThreadPool(threads);
            listener = Executors.newSingleThreadExecutor();
            listener.submit(this::listen);
        } catch (SocketException e) {
            System.out.println("Couldn't create socket: " + e.getMessage());
        }
    }

    private void listen() {
        try {
            while (!socket.isClosed() && !Thread.interrupted()) {
                DatagramPacket packet = PacketUtils.buildEmptyPacket(socket);
                socket.receive(packet);
                pool.submit(() -> handleRequest(packet));
            }
        } catch (IOException e) {
            System.out.println("Couldn't handle socket: " + e.getMessage());
        }
    }

    private void handleRequest(DatagramPacket packet) {
        String request = PacketUtils.getPacketMessage(packet);
        String answer = "Hello, " + request;

        try {
            socket.send(PacketUtils.buildPacketWithMessage(answer, packet.getSocketAddress()));
        } catch (IOException e) {
            System.out.println("Couldn't handle request " + request + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
        if (listener != null) {
            listener.shutdownNow();
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: HelloUDPServer port threads");
            return;
        }
        try {
            new HelloUDPServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Incorrect argument: " + e.getMessage());
        }
    }
}
