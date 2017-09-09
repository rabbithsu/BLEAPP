package com.example.android.BLEChat;

import com.example.android.common.logger.Log;

import android.content.Context;
import android.os.Handler;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by nccu_dct on 15/8/29.
 */
public class XMPPService {
    private static final String TAG = "XMPPService";
    private AbstractXMPPConnection connection;
    private final Handler mHandler;
    private Chat XMPPchat;



    //XMPP
    public static final String HOST = "140.119.164.5";
    public static final int PORT = 5225;
    public static final String SERVICE = "140.119.164.5";
    public static String USERNAME;
    public static String PASSWORD;
    private ArrayList<String> messages = new ArrayList<String>();



    public XMPPService(Context context, Handler handler, String username, String pw) {
        mHandler = handler;
        XMPPchat = null;
        USERNAME = username;
        PASSWORD = pw;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                // Create a connection
                XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
                config.setServiceName(SERVICE);
                config.setHost(HOST);
                config.setPort(PORT);
                config.setUsernameAndPassword(USERNAME, PASSWORD);
                //config.setDebuggerEnabled(true);
                config.setCompressionEnabled(false);
                config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);


                //config.setSASLAuthenticationEnabled(false);
                connection = new XMPPTCPConnection(config.build());

                try {
                    connection.connect();
                    Log.d("XMPPChatDemoActivity",
                            "Connected to " + connection.getHost());
                } catch (Exception ex) {
                    Log.d("XMPPChatDemoActivity", "Failed to connect to "
                            + connection.getHost());
                    Log.d("XMPPChatDemoActivity", ex.toString());
                    //connection = null;
                }
                try {
                    // SASLAuthentication.supportSASLMechanism("PLAIN", 0);
                    connection.login(USERNAME, PASSWORD);
                    Log.d("XMPPChatDemoActivity",
                            "Logged in as " + connection.getUser());

                    // Set the status to available
                    Presence presence = new Presence(Presence.Type.available);
                    //Presence.Type.unavailable
                    connection.sendStanza(presence);
                    //setReceive(connection);

                    Roster roster = Roster.getInstanceFor(connection);
                    if (!roster.isLoaded())
                        roster.reloadAndWait();
                    Collection<RosterEntry> entries = roster.getEntries();

                    for (RosterEntry entry : entries) {
                        //System.out.println(entry);
                        Log.d("XMPPChatDemoActivity", "USER:  "
                                + entry.getUser());
                        //Toast.makeText(getActivity(), entry.getName(), Toast.LENGTH_SHORT).show();

                    }
                    //chat receiver
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    chatManager.addChatListener(
                            new ChatManagerListener() {
                                @Override
                                public void chatCreated(Chat chat, boolean createdLocally)
                                {
                                    if (!createdLocally)
                                        chat.addMessageListener(new ChatMessageListener(){
                                            @Override
                                            public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                                                Log.d("XMPPChatDemoActivity", "Receive: " + message.getBody());
                                                mHandler.obtainMessage(Constants.MESSAGE_XMPP_READ, message.getBody().length(), -1, message.getBody())
                                                        .sendToTarget();
                                            }

                                        });;
                                }
                            });
                } catch (Exception ex) {
                    Log.d("XMPPChatDemoActivity", "Failed to log in as "
                            + USERNAME);
                    Log.d("XMPPChatDemoActivity", ex.toString());
                    connection = null;
                }

                //chatroomtest
                startchat("all@broadcast.140.119.164.5");

                //dialog.dismiss();
            }
        });
        t.start();
    }

    public AbstractXMPPConnection getConnection(){
        return this.connection;
    }

    public void write(String out) {
        // Create temporary object

        // Perform the write unsynchronized
        try {
            XMPPchat.sendMessage(out);
            mHandler.obtainMessage(Constants.MESSAGE_XMPP_WRITE, -1, -1, out)
                    .sendToTarget();
        }
        catch (Exception ex){
            Log.d(TAG, "Send message failed.");
        }
    }

    public void relaying(String out) {
        // Create temporary object

        // Perform the write unsynchronized
        try {
            XMPPchat.sendMessage(out);
        }
        catch (Exception ex){
            Log.d(TAG, "Send message failed.");
        }
    }

    public void startchat(String account){
        Log.d(TAG, account);
        new XMPPThread(account);
    }


    private class XMPPThread{
        private final String USRID;


        public XMPPThread(String account) {
            USRID = account;
            Log.d(TAG, "Chat to user: "+USRID);
            Thread m = new Thread(new Runnable() {
                @Override
                public void run(){
                //chat test
                    Chat chat = ChatManager.getInstanceFor(connection).createChat(USRID, new ChatMessageListener() {
                        @Override
                        public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                            Log.d("XMPPChatDemoActivity", "Receive: " + message.getBody());
                            mHandler.obtainMessage(Constants.MESSAGE_XMPP_READ, message.getBody().length(), -1, message.getBody())
                                .sendToTarget();
                            }

                });
                XMPPchat = chat;

                }
            });
            m.start();

        }

    }
}
