package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by khushbu on 3/13/16.
 */
public class MessagePack implements Serializable{
    protected int seqNum;
    protected String message;
    protected String type;
    protected boolean mark;
    protected String senderPort;

    MessagePack(String senderPort,String msg, int seqNum, String type,boolean mark){
        this.senderPort = senderPort;
        this.message = msg;
        this.seqNum = seqNum;
        this.type = type;
        this.mark = mark;
    }
}
