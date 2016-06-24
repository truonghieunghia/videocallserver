package com.org.thn.videocall.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ServerThread extends Thread {
	String line = null;
	BufferedReader is = null;
	PrintWriter os = null;
	Socket mSocket = null;
	String mClientID;

	public ServerThread(Socket socket) {
		mSocket = socket;
		mClientID = mSocket.getRemoteSocketAddress().toString();
	}

	public void sendTarget(String key, String Data) {
		PrintWriter os_send = null;
		Iterator<Entry<String, Socket>> it = VideoCallApp.mlistClient.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Socket> pair = it.next();
			if (!pair.equals(key)) {
				Socket socket = pair.getValue();
				try {
					os_send = new PrintWriter(socket.getOutputStream());
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
			int off = 0;
			while (count != -1) {
				//			
				Iterator<Entry<String, Socket>> it = VideoCallApp.mlistClient.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Socket> pair = it.next();
					Socket socket = pair.getValue();
					try {
						os_send = socket.getOutputStream();
						os_send.write(bytes, 0, 1024);
						os_send.flush();
						System.out.println(bytes.toString());
						count = in.read(bytes, 0, 1024);
					} catch (IOException e) {
						System.out.println("IO error "+e.getMessage());
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
			is = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			os = new PrintWriter(mSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("IO error in server thread");
		}
		try {
			line = is.readLine();
			while (line.compareTo("QUIT") != 0) {

				// os.println(line);
				// os.flush();
				System.out.println("Response to Client  :  " + line);
				sendTarget(mClientID, line);
				
				new Thread(new Runnable() {
					
					@Override
					public void run() {					
						sendVideo();
					}
				}).start();
				line = is.readLine();
			}
			os.println("QUIT");
			os.flush();
		} catch (IOException e) {
			line = this.getName(); // reused String line for getting thread name
			System.out.println("IO Error/ Client " + line + " terminated abruptly");
		} catch (NullPointerException e) {
			line = this.getName(); // reused String line for getting thread name
			System.out.println("Client " + line + " Closed");
		} finally {
			try {
				System.out.println("Connection Closing..");
				if (is != null) {
					is.close();
					System.out.println(" Socket Input Stream Closed");
				}

				if (os != null) {
					os.close();
					System.out.println("Socket Out Closed");
				}
				if (mSocket != null) {
					mSocket.close();
					VideoCallApp.mlistClient.remove(mClientID);
					System.out.println("Socket Closed");
					System.out.println("ListClinet:" + VideoCallApp.mlistClient.size());
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error");
			}
		}
	}
	
}
