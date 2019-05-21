package ru.ifmo.rain.shelepov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress serverAddress;

        try {
            serverAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Couldn't find host: " + host);
            return;
        }

        try {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, port);
            for (int i = 0; i < threads; i++) {
                final int id = i;
                pool.submit(() -> handleNRequests(socketAddress, id, requests, prefix));
            }

            pool.shutdown();

            try {
                pool.awaitTermination(threads * requests, TimeUnit.MINUTES);
            } catch (InterruptedException ignored) {

            }
        } catch (IllegalArgumentException e) {
            System.out.println("Illegal argument: " + e.getMessage());
        }

    }

    private String buildMessage(String prefix, int threadId, int requestId) {
        return prefix + threadId + "_" + requestId;
    }


    private void handleNRequests(SocketAddress socketAddress, int threadId, int n, String prefix) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);
            for (int requestId = 0; requestId < n; requestId++) {
                String request = buildMessage(prefix, threadId, requestId);
                DatagramPacket requestPacket = PacketUtils.buildPacketWithMessage(request, socketAddress);
                DatagramPacket respondPacket = PacketUtils.buildEmptyPacket(socket);

                while (true) {
                    try {
                        socket.send(requestPacket);
                        System.out.println("Sending request: " + request);
                        socket.receive(respondPacket);
                        String response = PacketUtils.getPacketMessage(respondPacket);

                        if (isCorrectResponse(response, request)) {
                            System.out.println("Received response: " + response);
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println("Couldn't handle request: " + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Couldn't create socket to host " + ((InetSocketAddress)socketAddress).getHostName() + ": " + e.getMessage());
        }
    }

    private boolean isCorrectResponse(String response, String request) {
        return response.contains(request);
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Usage: HelloUDPClient host port prefix threads requests");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Argument can't be null");
            return;
        }
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(host, port, prefix, threads, requests);
        } catch (NumberFormatException e) {
            System.err.println("Incorrect argument: " + e.getMessage());
        }
    }
}
