package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by khushbu on 3/14/16.
 */
public class MessagePackComparator implements Comparator<MessagePack> {
    @Override
    public int compare(MessagePack mp1,MessagePack mp2) {

        if (mp1.seqNum < mp2.seqNum) {
            return -1;
        } else if (mp1.seqNum == mp2.seqNum) {
            if(mp1.senderPort.compareToIgnoreCase(mp2.senderPort)>0){
                return 1;
            }
            else
                return -1;
        } else {
            return 1;
        }
    }
}
