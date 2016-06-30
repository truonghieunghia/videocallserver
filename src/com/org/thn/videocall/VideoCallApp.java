package com.org.thn.videocall;

import com.org.thn.videocall.server.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by tnghia on 6/29/16.
 */
public class VideoCallApp {
    public static final int DEFAULT_PORT = 8400;

    public static void main(String[] args) {
        try {
            System.out.println("ServerIP:" + InetAddress.getLocalHost().getHostAddress());
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("Listening on port " + DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection receive from " + socket.getRemoteSocketAddress());
                new ServerThread(socket.getRemoteSocketAddress().toString(),socket).start();
            }
        } catch (UnknownHostException e) {
            System.out.println("VideoCallApp:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("VideoCallApp:" + e.getMessage());
        }
    }
}
