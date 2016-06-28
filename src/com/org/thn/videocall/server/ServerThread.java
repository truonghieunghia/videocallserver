package com.org.thn.videocall.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import java.util.Map.Entry;



public class ServerThread extends Thread {
	public static HashMap<String, ServerThread> LIST_CLIENT = new HashMap<>();
	Socket mSocket = null;
	String mClientID="";
	private InputStream mIn;
	private OutputStream mOut;
	private boolean alreadyclosed = false;
	CommonSoundClass cs = new CommonSoundClass();
	private boolean keepGoing = false;
	Vector<VectorAndSize> mRecievedByteVector = new Vector<VectorAndSize>();
	private byte[] breaker = MultiChatConstants.BREAKER.getBytes();

	public ServerThread(Socket socket) {
		mSocket = socket;	
	}

	public synchronized void sendData() {

	}

	public void reciverData() {

	}

	public Socket getSocket() {
		return mSocket;
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
			mIn = mSocket.getInputStream();
			SenderThread senderThread = new SenderThread(this);
			TimeOut t = new TimeOut(this);
			t.start();
			t.shuldclose = false;
			senderThread.start();
			keepGoing = true;
		} catch (IOException e) {
			System.out.println("IO error in server thread");
		}
		try {
			ReceivedQueueProcessorThread il = new ReceivedQueueProcessorThread(this);
			il.start();
			byte[] mybyte = new byte[1024 * 3];

            int j = 0;
			while (keepGoing) {

				byte[] bytepassedObj = new byte[MultiChatConstants.bytesize];
				int sizeread = mIn.read(bytepassedObj, 0, MultiChatConstants.bytesize);
				for (int i = 0; i < sizeread; i++, j++) {
                    mybyte[j] = bytepassedObj[i];

                    // Read up to 3071 (though we're only currently reading up to 1025 bytes in)
                    // or if this has the breaker at the location we're currently at
                    if (j == (1024 * 3 - 1) || (j >= 4 && mybyte[j - 4] == breaker[0]
                            && mybyte[j - 3] == breaker[1] && mybyte[j - 2] == breaker[2]
                            && mybyte[j - 1] == breaker[3] && mybyte[j] == breaker[4])) {
                    	mRecievedByteVector.addElement(new VectorAndSize(mybyte, j + 1));
                        j = -1;
                        // Create a new container
                        mybyte = new byte[1024 * 3];
                    }
                }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if (! alreadyclosed) {
                alreadyclosed = true;
                try {
					mIn.close();
					mOut.close();
	                mSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
            }
			synchronized (LIST_CLIENT) {
				LIST_CLIENT.remove(mClientID);
			}
		}
	}

	class SenderThread extends Thread {
		ServerThread mServerThread;

		public SenderThread(ServerThread serverThread) {
			mServerThread = serverThread;
		}

		@Override
		public void run() {
			while (keepGoing) {
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

	class VectorAndSize implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public byte[] b;
		public int size;

		public VectorAndSize(byte[] b, int size) {
			this.b = b;
			this.size = size;
		}
	}

	class ReceivedQueueProcessorThread extends Thread {
		ServerThread mPtrToThis = null;

		public ReceivedQueueProcessorThread(ServerThread ptrtoThis) {
			this.mPtrToThis = ptrtoThis;
		}

		@Override
		public void run() {
			while (keepGoing) {
				if (mRecievedByteVector.size() > 0) {
					VectorAndSize vs = mRecievedByteVector.remove(0);
					byte[] bytepassedObj = vs.b;
					int sizeread = vs.size;

					processData(sizeread, bytepassedObj);
				} else {
					try {
						synchronized (this) {
							wait(10);
						}
					} catch (Exception mexp) {
						mexp.printStackTrace();
					}
				}
			}
		}

		private void processData(int sizeread, byte[] bytepassedObj) {
			synchronized (LIST_CLIENT) {
				String passedObj = "";
				if (sizeread < 100 && sizeread >= 0) {
                    passedObj = new String(bytepassedObj, 0, sizeread);
                }

                byte[] b = new byte[sizeread];
                for (int x = 0 ; x < sizeread; x++) {
                    b[x] = bytepassedObj[x];
                }
				// add client id
				if ((sizeread > 2 && sizeread < 100 && passedObj.length() >= 2
						&& passedObj.substring(0, 2).equals(MultiChatConstants.USER_ID)) && mPtrToThis.mClientID == "") {
					if (LIST_CLIENT.containsKey(passedObj.substring(2, passedObj.length() - 5))) {
						LIST_CLIENT.remove(passedObj.substring(2, passedObj.length() - 5));
					}
					if (keepGoing) {
						if (passedObj.length() > 6) {
							mPtrToThis.mClientID = passedObj.substring(2, passedObj.length() - 5);
							LIST_CLIENT.put(mPtrToThis.mClientID, mPtrToThis);
							System.out.println(LIST_CLIENT.size() + "Client");
						}
					}

				}
			}
		}
	}
	class TimeOut extends Thread{
		ServerThread fr ;
		public boolean shuldclose = true;
		public TimeOut (ServerThread serverThread){
			fr=serverThread;
		}
		@Override
		public void run() {
			 try {
	                sleep(9000);
	                if (shuldclose && !alreadyclosed) {
	                    alreadyclosed = true;
	                    if (fr.mIn != null) {
	                        fr.mIn.close();
	                    }
	                    if (fr.mOut != null) {
	                        fr.mOut.close();
	                    }
	                    if (fr.mSocket != null) {
	                        fr.mSocket.close();
	                    }
	                }
	            } catch (Exception exp) {
	            }
		}
	}
}
