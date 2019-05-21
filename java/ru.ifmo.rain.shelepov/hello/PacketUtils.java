package ru.ifmo.rain.shelepov.hello;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

class PacketUtils {

    static DatagramPacket buildPacketWithMessage(String request, SocketAddress socketAddress) {
        byte[] byteRequest = request.getBytes(Charset.forName("UTF-8"));
        return new DatagramPacket(byteRequest, byteRequest.length, socketAddress);
    }

    static DatagramPacket buildEmptyPacket(DatagramSocket socket) throws SocketException {
        byte[] byteRespond = new byte[socket.getReceiveBufferSize()];
        return new DatagramPacket(byteRespond, byteRespond.length);
    }

    static String getPacketMessage(DatagramPacket respondPacket) {
        return new String(respondPacket.getData(), respondPacket.getOffset(), respondPacket.getLength(), Charset.forName("UTF-8"));
    }
}
