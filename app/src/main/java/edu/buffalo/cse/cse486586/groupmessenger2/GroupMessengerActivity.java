package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static ArrayList<String> REMOTE_PORT;
    static final int SERVER_PORT = 10000;
    private final Uri mUri;
    private final ContentValues mContentValues;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static int key = 0;
    private final GroupMessengerProvider mProvider;
    private static int seqNum = 0;
    private static PriorityQueue<MessagePack> PQ;
    private static String myPort;
    private static String socketDead = "";
    private static boolean exceptionFlag = false;
    private static LinkedList<String> track;
    public GroupMessengerActivity() {
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        mContentValues = new ContentValues();
        mProvider = new GroupMessengerProvider();
        Comparator<MessagePack> comparator = new MessagePackComparator();
        PQ = new PriorityQueue<MessagePack>(40,comparator);
        track = new LinkedList<String>();
        REMOTE_PORT = new ArrayList<String>();
        REMOTE_PORT.add("11108");
        REMOTE_PORT.add("11112");
        REMOTE_PORT.add("11116");
        REMOTE_PORT.add("11120");
        REMOTE_PORT.add("11124");
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e("MyPort",myPort);
        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            serverSocket.setSoTimeout(2000);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SockedTimeOutException");
        } catch (IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendBtn = (Button) findViewById(R.id.button4);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            MessagePack incomingMessage = null;
            while(true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(3500);

                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    incomingMessage = (MessagePack)in.readObject(); //receive the packet

                    if(incomingMessage.type.equals("TEXT")) {
                        PQ.add(incomingMessage);
                        MessagePack seqPack = new MessagePack(myPort,incomingMessage.message,seqNum,"SEQ",false);
                        if(seqNum < incomingMessage.seqNum)
                            seqNum = incomingMessage.seqNum+1;
                        else
                            seqNum++;

                        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                        out.writeObject(seqPack);
                        track.add(incomingMessage.message);

                        out.close();
                    } else if (incomingMessage.type.equals("SEQ")){
                        track.remove(incomingMessage.message);
                        for(MessagePack p : PQ){
                            if(p.message.equals(incomingMessage.message)){
                                PQ.remove(p);
                                PQ.add(incomingMessage);
                                break;
                            }
                        }
                        if(seqNum < incomingMessage.seqNum)
                            seqNum = incomingMessage.seqNum+1;
                        else
                            seqNum++;
                        if(!PQ.isEmpty()){
                            this.publishProgress("");
                        }
                    }

                    in.close();
                    clientSocket.close();

                } catch (SocketTimeoutException e) {
//                    Log.e(TAG, "ServerTask socket SockedTimeOutException");
//                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "ServerTask Exception");
                    socketDead = incomingMessage.senderPort;
                    exceptionFlag = true;
                    e.printStackTrace();
                    this.publishProgress("");
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            final TextView tv = (TextView) findViewById(R.id.textView1);
            for(int i=0;i<2500;i++);
            if(exceptionFlag==true)
                Log.e("SD",socketDead);
            while(!PQ.isEmpty()) {
                MessagePack pq = PQ.peek();
                if (pq.mark == true || (exceptionFlag==true && pq.senderPort.equals(socketDead))) {
                    MessagePack p = PQ.poll();
                    if(p!=null) {
                        Log.e("IN",p.senderPort+" "+p.seqNum+" " + p.message);
                        tv.append(p.message + "\n");
                        mContentValues.put(KEY_FIELD, key + "");
                        mContentValues.put(VALUE_FIELD, p.message);
                        mProvider.insert(mUri, mContentValues);
                        key++;
                        if(track.contains(p.message)){
                            track.remove(p.message);
                        }
                    }
                }
                else{
                    break;
                }
            }
            for(String s : track){
                Log.e("track",s);
            }
        }
    }

    private class ClientTask extends AsyncTask<String, MessagePack, Void> {
        @Override
        protected Void doInBackground(String... msgs) {

            MessagePack msgToSend = new MessagePack(myPort,msgs[0],seqNum,"TEXT",false);

            int max = msgToSend.seqNum;
            MessagePack incomingMessage;

            for(String remotePort : REMOTE_PORT) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    if(exceptionFlag==false)
                            socketDead = remotePort;
                    socket.setSoTimeout(2500);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(msgToSend);

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    incomingMessage = (MessagePack) in.readObject();
                    int seq = incomingMessage.seqNum;
                    if (seq > max) {
                        max = seq;
                    }
                    in.close();
                    out.close();
                    socket.close();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask1 socket SockedTimeOutException");
                    e.printStackTrace();
                    Log.e(TAG, socketDead);
                    exceptionFlag = true;
                    continue;
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (Exception e) {
                    Log.e(TAG, "ClientTask1 socket IOException");
                    e.printStackTrace();
                    exceptionFlag = true;
                    continue;
                }
            }
            if(exceptionFlag==true && REMOTE_PORT.contains(socketDead)) {
                REMOTE_PORT.remove(socketDead);
                Log.e("1",msgToSend.message);
            }

            MessagePack proposedSeq = new MessagePack(myPort,msgs[0],max,"SEQ",true);

            for(String remotePort : REMOTE_PORT){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    if(exceptionFlag==false)
                        socketDead = remotePort;
                    socket.setSoTimeout(2500);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(proposedSeq);

                    out.close();
                    socket.close();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask2 socket SockedTimeOutException");
                    e.printStackTrace();
                    exceptionFlag = true;
                    continue;
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (Exception e) {
                    Log.e(TAG, "ClientTask2 socket IOException");
                    e.printStackTrace();
                    exceptionFlag = true;
                    continue;
                }
            }
            if(exceptionFlag==true && REMOTE_PORT.contains(socketDead)) {
                REMOTE_PORT.remove(socketDead);
                Log.e("2", proposedSeq.message);
            }

            int x = 0;
            MessagePack pingMsg = new MessagePack(myPort,"Ping",0,"PING",false);
            while(exceptionFlag==false && x<=5){
                x++;
                for(String remotePort : REMOTE_PORT){
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                        if(exceptionFlag==false)
                            socketDead = remotePort;

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(pingMsg);

                        socket.setSoTimeout(2500);
                        socket.close();
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ClientTask3 socket SockedTimeOutException");
                        e.printStackTrace();
                        exceptionFlag = true;
                        continue;
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (Exception e) {
                        Log.e(TAG, "ClientTask3 socket IOException");
                        e.printStackTrace();
                        exceptionFlag = true;
                        continue;
                    }
                }
                if(exceptionFlag==true && REMOTE_PORT.contains(socketDead)) {
                    REMOTE_PORT.remove(socketDead);
                    Log.e("3", pingMsg.message);
                }
            }
            return null;
        }
    }
}