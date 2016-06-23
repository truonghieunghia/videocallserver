package com.org.thn.videocall.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class VideoCallApp {
	public static final int DEFAULT_PORT = 8400;
	public static Hashtable<String, Socket> mlistClient = new Hashtable<>();

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		Socket socket = null;

		try {
			System.out.println("ServerIP:" + InetAddress.getLocalHost().getHostAddress());
			serverSocket = new ServerSocket(DEFAULT_PORT);
			System.out.println("Listening on port " + DEFAULT_PORT);
			while (true) {
				socket = serverSocket.accept();
				socket.setSoTimeout(60000);
				System.out.println("Connection receive from " + socket.getRemoteSocketAddress());
				mlistClient.put(socket.getRemoteSocketAddress().toString(), socket);
				new ServerThread(socket).start();
				System.out.println("ListClinet:"+mlistClient.size());				
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
