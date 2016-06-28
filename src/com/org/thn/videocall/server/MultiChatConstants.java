package com.org.thn.videocall.server;

/**
 * Created by IntelliJ IDEA.
 * User: Steve
 * Date: May 6, 2005
 * Time: 3:00:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiChatConstants {
    public static final String BREAKER = "KAERB";
    public static final int maxTxtConvoSize = 1024;

    /**
     * the maximum size of each packet in bytes
     */
    // Why the +1 ?
    public final static int bytesize = 1024 + 1;
    public final static String USER_ID="00";
}
