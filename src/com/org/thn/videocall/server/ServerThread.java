package com.org.thn.videocall.server;


import com.org.thn.videocall.DataSender;
import com.org.thn.videocall.MultiConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by tnghia on 6/29/16.
 */
public class ServerThread extends Thread {
    public static HashMap<String, ServerThread> LIST_CLIENT = new HashMap<>();
    private Socket mSocket;
    private InputStream mInputStream;
    private String mClientID = "";
    private OutputStream mOutputStream;
    private boolean mKeepGoing = false;
    private DataSender mDataSender = new DataSender();
    private Vector<VectorAndSize> mRecievedByteVector = new Vector<VectorAndSize>();
    private byte[] breaker = MultiConstants.BREAKER.getBytes();
    private String ipAddress = "";

    public ServerThread(String threadName, Socket socket) {
        super(threadName);
        mSocket = socket;
        ipAddress = threadName;
    }

    class ServerSender extends Thread {
        ServerThread serverThread;

        public ServerSender(ServerThread serverThread) {
            this.serverThread = serverThread;
        }

        @Override
        public void run() {
            try {
                while (mKeepGoing) {
                    byte[] b = (byte[]) mDataSender.readbyte();
                    serverThread.mOutputStream.write(b);
                    serverThread.mOutputStream.flush();
                }
            } catch (IOException e) {
                mKeepGoing = false;
                System.out.println("error_44:" + e.getMessage());
            } finally {
                mKeepGoing = false;
            }

        }
    }

    class VectorAndSize implements Serializable {
        public byte[] b;
        public int size;

        public VectorAndSize(byte[] b, int size) {
            this.b = b;
            this.size = size;
        }
    }

    class ReceivedQueueProcessorThread extends Thread {
        ServerThread serverThread;

        public ReceivedQueueProcessorThread(ServerThread serverThread) {
            this.serverThread = serverThread;
        }

        @Override
        public void run() {
            while (mKeepGoing) {
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
                    } catch (InterruptedException e) {
                        System.out.println("error_86:" + e.getMessage());
                    }
                }
            }
        }

        private void processData(int sizeread, byte[] bytepassedObj) {
            String passedObj = "";
            if (sizeread < 100 && sizeread >= 0) {
                passedObj = new String(bytepassedObj, 0, sizeread);
            }
            byte[] b = new byte[sizeread];
            for (int x = 0; x < sizeread; x++) {
                b[x] = bytepassedObj[x];
            }
            if ((sizeread > 2 && sizeread < 100 && passedObj.length() >= 2 && passedObj.substring(0, 2).equals(MultiConstants.USER_ID) && serverThread.mClientID.isEmpty())) {
                if (LIST_CLIENT.containsKey(passedObj.substring(2, passedObj.length() - 5))) {
                    try {
                        LIST_CLIENT.get(passedObj.substring(2, passedObj.length() - 5)).mKeepGoing = false;
                        LIST_CLIENT.get(passedObj.substring(2, passedObj.length() - 5)).mInputStream.close();
                        LIST_CLIENT.get(passedObj.substring(2, passedObj.length() - 5)).mOutputStream.close();
                        LIST_CLIENT.get(passedObj.substring(2, passedObj.length() - 5)).mSocket.close();
                    } catch (IOException e) {

                    }
                    LIST_CLIENT.remove(passedObj.substring(2, passedObj.length() - 5));
                }
                if (mKeepGoing) {
                    if (passedObj.length() > 6) {
                        serverThread.mClientID = passedObj.substring(2, passedObj.length() - 5);
                        LIST_CLIENT.put(passedObj.substring(2, passedObj.length() - 5), serverThread);
                        System.out.println(LIST_CLIENT.size() + ": Client Connect");
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            mOutputStream = mSocket.getOutputStream();
            mOutputStream.flush();
            mInputStream = mSocket.getInputStream();
            mKeepGoing = true;
            new ServerSender(this).start();
            new ReceivedQueueProcessorThread(this).start();
            byte[] mybyte = new byte[1024 * 3];
            int j = 0;
            while (mKeepGoing) {
                byte[] bytepassedObj = new byte[MultiConstants.bytesize];
                int sizeread = mInputStream.read(bytepassedObj, 0, MultiConstants.bytesize);
                if (sizeread == -1) {
                    mKeepGoing = false;
                }
                for (int i = 0; i < sizeread; i++, j++) {
                    mybyte[j] = bytepassedObj[i];
                    if (j == (1024 * 3 - 1) || (j >= 4 && mybyte[j - 4] == breaker[0] && mybyte[j - 3] == breaker[1]
                            && mybyte[j - 2] == breaker[2] && mybyte[j - 1] == breaker[3] && mybyte[j] == breaker[4])) {
                        mRecievedByteVector.addElement(new VectorAndSize(mybyte, j + 1));
                        j = -1;
                        // Create a new container
                        mybyte = new byte[1024 * 3];
                    }
                }
            }
        } catch (IOException e) {
            mKeepGoing = false;
            if (LIST_CLIENT.containsKey(mClientID)) {
                if (LIST_CLIENT.get(mClientID).ipAddress.equals(ipAddress)) {
                    LIST_CLIENT.remove(mClientID);
                }
            }
            System.out.println("error_40:" + e.getMessage());
        } finally {
            try {
                mKeepGoing = false;
                if (mInputStream != null) {
                    mInputStream.close();
                }
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
                if (mSocket != null) {
                    mSocket.close();
                }
                if (LIST_CLIENT.containsKey(mClientID)) {
                    if (LIST_CLIENT.get(mClientID).ipAddress.equals(ipAddress)) {
                        LIST_CLIENT.remove(mClientID);
                    }
                }
                System.out.println(LIST_CLIENT.size() + ": Client Connect");
            } catch (IOException e) {
                mKeepGoing = false;
                if (LIST_CLIENT.containsKey(mClientID)) {
                    if (LIST_CLIENT.get(mClientID).ipAddress.equals(ipAddress)) {
                        LIST_CLIENT.remove(mClientID);
                    }
                }
                System.out.println("error_40:" + e.getMessage());
            } finally {
                if (LIST_CLIENT.containsKey(mClientID)) {
                    if (LIST_CLIENT.get(mClientID).ipAddress.equals(ipAddress)) {
                        LIST_CLIENT.remove(mClientID);
                    }
                }
                mKeepGoing = false;
            }
        }

    }

    private void sendBroadcastMessage(String... sms) {
        String message = "";
        for (String str : sms) {
            message += str;
        }
        if (!message.isEmpty()){
            String action = message.substring(0,2);
            switch (action){
                case MultiConstants.SMS:
                    String toUser = message.substring(2,22);
                    ServerThread serverThread = LIST_CLIENT.get(toUser);
                    serverThread.mDataSender.writebyte(message.getBytes());
                    break;
            }
        }
    }
}
