package com.org.thn.videocall.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ServerThread extends Thread {
	public static HashMap<String, ServerThread> LIST_CLIENT = new HashMap<>();
	Socket mSocket = null;
	String mClientID;
	private InputStream mIn;
	private OutputStream mOut;
	CommonSoundClass cs = new CommonSoundClass();
	private boolean keepGoing = false;

	public ServerThread(Socket socket) {
		mSocket = socket;
		mClientID = mSocket.getRemoteSocketAddress().toString();
	}

	public synchronized void sendData() {

	}

	public void reciverData() {

	}

	public Socket getSocket() {
		return mSocket;
	}

	public void sendTarget(String key, String Data) {
		PrintWriter os_send = null;
		Iterator<Entry<String, ServerThread>> it = VideoCallApp.mlistClient.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ServerThread> pair = it.next();
			if (!pair.equals(key)) {
				ServerThread socket = pair.getValue();
				try {
					os_send = new PrintWriter(socket.getSocket().getOutputStream());
					os_send.println(Data);
					os_send.flush();
				} catch (IOException e) {
					System.out.println("IO error in server thread send");
				} finally {
				}
			}
		}
	}

	public void sendVideo() {
		URL path = VideoCallApp.class.getResource("mov_bbb.mp4");
		System.out.println(path.getFile());
		File file = new File(path.getFile());
		try {
			FileInputStream in = new FileInputStream(file);
			byte[] bytes = new byte[1024];
			int count = in.read(bytes, 0, 1024);
			OutputStream os_send = null;
			while (count != -1) {
				//
				Iterator<Entry<String, ServerThread>> it = VideoCallApp.mlistClient.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, ServerThread> pair = it.next();
					ServerThread socket = pair.getValue();
					try {
						os_send = socket.getSocket().getOutputStream();
						os_send.write(bytes, 0, 1024);
						os_send.flush();
						System.out.println(bytes.toString());
						count = in.read(bytes, 0, 1024);
					} catch (IOException e) {
						System.out.println("IO error " + e.getMessage());
					} finally {

					}
				}
				count = in.read(bytes, 0, 1024);
				//
			}
			in.close();
			System.out.println("send stream complele");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			mOut = mSocket.getOutputStream();
			mOut.flush();
			SenderThread senderThread = new SenderThread(this);
			senderThread.start();
			keepGoing = true;
			mIn = mSocket.getInputStream();			
		} catch (IOException e) {
			System.out.println("IO error in server thread");
		}
		synchronized (LIST_CLIENT) {
			LIST_CLIENT.put(mClientID, this);
		}
	}

	class SenderThread extends Thread {
		ServerThread mServerThread;

		public SenderThread(ServerThread serverThread) {
			mServerThread = serverThread;
		}

		@Override
		public void run() {
			while (true) {
				try {
					byte[] b = (byte[]) cs.readbyte();
					mServerThread.mOut.write(b);
					mServerThread.mOut.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}
}
