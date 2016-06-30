package com.org.thn.videocall;

import java.util.Vector;

/**
 * Created by tnghia on 6/29/16.
 */
public class DataSender {
    public Vector vec = new Vector();
    private byte b[];


    public DataSender() {
    }

    synchronized public Object readbyte() {
        if (!vec.isEmpty()) {
            b = (byte[]) vec.remove(0);
            return b;
        } else {
            byte[] b = new byte[5];
            return b;
        }

    }

    synchronized public void writebyte(Object e) {
        vec.addElement(e);
        notifyAll();
    }
}
