/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BLEChat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;


import java.util.ArrayList;
import java.util.List;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {
    //2 steps
    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    //XMPP codes
    private static final int REQUEST_XMPP_CONNECT = 4;
    private static final int REQUEST_XMPP_LOGIN = 5;

    // Layout Views
    ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private MessageAdapter mConversationArrayAdapter;
    private List<CheckMessage> mdata;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BLEChatService mChatService = null;

    //auto
    ArrayList<BluetoothDevice> device = new ArrayList<BluetoothDevice>();
    //XMPP
    public static XMPPService mXMPPService = null;
    private  boolean XMPPing = false;
    private  String mXMPPname = null;
    private  String username = "rabbithsuqqq";
    private  String password = "123456";

    //DB
    public static MitemDB itemDB;
    private List<CheckMessage> items= new ArrayList<>();

    //three
    private String MyName = "USERv";//Guest";

    //json
    private String JSONString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


        // 建立資料庫物件
        itemDB = new MitemDB(getActivity().getApplicationContext());
        // 如果資料庫是空的，就建立一些範例資料
        // 這是為了方便測試用的，完成應用程式以後可以拿掉
        /*if (itemDB.getCount() == 0) {
            itemDB.sample();
        }*/

        // 取得所有記事資料
        items = itemDB.getAll();

        /*for(CheckMessage i : items){
            itemDB.delete(i.getId());
        }
        items.clear();*/

        //itemDB.sample();
        // Get local Bluetooth adapter

        //Toast.makeText(getActivity(), "onCreat.", Toast.LENGTH_LONG).show();
        BluetoothManager mBluetoothManager = (BluetoothManager) getActivity().getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        MyName = mBluetoothAdapter.getName();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        //XMPPconnect();

    }


    @Override
    public void onStart() {
        super.onStart();
        //Toast.makeText(getActivity(), "onStart.", Toast.LENGTH_LONG).show();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            while (!mBluetoothAdapter.isEnabled()) {

            }
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(getActivity(), "onDestroy.", Toast.LENGTH_LONG).show();
        if (mChatService != null) {
            mChatService.stop();
            mChatService = null;
        }
        //stopAdvertising();

    }

    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(getActivity(), "onResume.", Toast.LENGTH_LONG).show();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        //getActivity().registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //getActivity().registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        if (mChatService != null) {

            // Only if the state is STATE_NONE, do we know that we haven't started already
            //Toast.makeText(getActivity(), mChatService.getState()+"", Toast.LENGTH_LONG).show();
            if (mChatService.getState() == BLEChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
                //mChatService = null;
                //setupChat();
            }
            //mChatService.start();// = null;
            //setupChat();

        }
        else {
            setupChat();
        }


    }
    @Override
    public void onStop() {
        super.onStop();
        //Toast.makeText(getActivity(), "onStop.", Toast.LENGTH_LONG).show();
        device.clear();
        /*if(mChatService != null) {
            mChatService.stop();
        }*/
    }
    @Override
    public void onPause() {
        super.onPause();
        //Toast.makeText(getActivity(), "onPause.", Toast.LENGTH_LONG).show();
        try {
            getActivity().unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e){

        }

    }
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = (ListView) view.findViewById(R.id.chat);
        mConversationView =(ListView) view.findViewById(R.id.chat);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);

    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");
        //startAdvertising();
        // Initialize the array adapter for the conversation thread
        mdata = LoadData();

        mConversationArrayAdapter = new MessageAdapter(getActivity(), mdata);

        mConversationView.setAdapter(mConversationArrayAdapter);
        mConversationArrayAdapter.Refresh();

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message, MyName);
                }
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BLEChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        //Intent serverIntent = new Intent(getActivity(), LoginXMPPActivity.class);
        //startActivityForResult(serverIntent, REQUEST_XMPP_LOGIN);
        //XMPPconnect();
        mChatService.start();
        //try auto
        //Toast.makeText(getActivity(), "Start try.", Toast.LENGTH_SHORT).show();
        //doDiscovery();


    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);
            //startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message, String name) {
        // Check that we're actually connected before trying anything


        // Check that there's actually something to send
        if (message.length() > 0) {
            String namemessage = message;
            //itemDB.insert(new CheckMessage(0, tsLong, CheckMessage.MessageType_From, name, message));
            mChatService.write(message);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message, MyName);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    setStatus(R.string.connected);
                    /*switch (msg.arg1) {
                        case BLEChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //ArrayAdapter.clear();
                            //mdata.clear();
                            //mConversationArrayAdapter.Refresh();
                            break;
                        case BLEChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BLEChatService.STATE_LISTEN:
                        case BLEChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }*/
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    mdata.add(new CheckMessage(0, 0, CheckMessage.MessageType_From, MyName, writeMessage));
                    mConversationArrayAdapter.Refresh();
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf);
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    CheckMessage tmp;
                    tmp = new CheckMessage(0, 0, CheckMessage.MessageType_To,
                            "A", readMessage);
                    mdata.add(tmp);
                    mConversationArrayAdapter.Refresh();
                    break;
                    //Log.d(TAG, readMessage);
                    /*String rMessage = readMessage.split("##")[1];
                    CheckMessage tmp;

                    try {
                        if ((filter(Long.parseLong(readMessage.split("##")[2]), readMessage.split("##")[0])))
                            break;
                    }catch (Exception ex){

                        Log.e(TAG, "Error: " + readMessage);
                        break;
                    }
                    if(readMessage.split("##")[0].equals(MyName)&&(!MyName.equals("Guest"))){
                        tmp = new CheckMessage(0, Long.parseLong(readMessage.split("##")[2]), CheckMessage.MessageType_From,
                                readMessage.split("##")[0], rMessage);
                    }
                    else{
                        tmp = new CheckMessage(0, Long.parseLong(readMessage.split("##")[2]), CheckMessage.MessageType_To,
                                readMessage.split("##")[0], rMessage);
                    }
                    itemDB.insert(tmp);
                    mdata.add(tmp);

                    //relay!?
                    if(XMPPing){
                        mXMPPService.relaying(readMessage);
                    }
                    mChatService.relaying(readMessage);

                    mConversationArrayAdapter.Refresh();
                    break;*/
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_XMPP_READ:
                    String read = (String) msg.obj;
                    if(read.split("##").length < 3){
                        read = "unknownXMPP##"+read+"##0";
                    }
                    String rread = read.split("##")[1];
                    CheckMessage ttmp;

                    if ((filter(Long.parseLong(read.split("##")[2]),read.split("##")[0])) )
                        break;

                    if(read.split("##")[0].equals(MyName)&&(!MyName.equals("Guest"))) {
                        ttmp = new CheckMessage(0, Long.parseLong(read.split("##")[2]), CheckMessage.MessageType_From,
                                read.split("##")[0], rread);
                    }
                    else{
                        ttmp = new CheckMessage(0, Long.parseLong(read.split("##")[2]), CheckMessage.MessageType_To,
                                read.split("##")[0], rread);
                    }
                    itemDB.insert(ttmp);
                    mdata.add(ttmp);

                    //relay!?
                    if(mChatService.getState() == BLEChatService.STATE_CONNECTED) {
                        // Get the message bytes and tell the BluetoothChatService to write
                        //byte[] Xsend = read.getBytes();
                        mChatService.relaying(read);
                    }

                    mConversationArrayAdapter.Refresh();
                    break;
                case Constants.MESSAGE_XMPP_WRITE:

                    //avoid UI dup
                    if(mChatService.getState()==BLEChatService.STATE_CONNECTED){
                        break;
                    }

                    String write = (String) msg.obj;
                    String wwrite = write.split("##")[1];
                    mdata.add(new CheckMessage(0, Long.parseLong(write.split("##")[2]), CheckMessage.MessageType_From, MyName, wwrite));
                    mConversationArrayAdapter.Refresh();
                    break;
            }
        }
    };
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //XMPPing = false;
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //XMPPing = false;
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            case REQUEST_XMPP_CONNECT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    /*if(mChatService != null) {
                        mChatService.stop();
                    }*/
                    connectXMPPUser(data);

                }
                break;
            case REQUEST_XMPP_LOGIN:
                //LOGIN INFORMATION
                if (resultCode == Activity.RESULT_OK){
                    username = data.getExtras().getString("USER");
                    password = data.getExtras().getString("PW");
                    MyName = data.getExtras().getString("NAME");
                    XMPPconnect();
                }
                break;
            default:
                Log.d(TAG, "onActivity fail.");
                break;
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.xmpp_connect: {
                Intent serverIntent = new Intent(getActivity(), XMPPListActivity.class);
                startActivityForResult(serverIntent, REQUEST_XMPP_CONNECT);
                return true;
            }
            case R.id.DB_Clear: {
                for(CheckMessage i : items){
                    itemDB.delete(i.getId());
                }
                items.clear();
                mdata = LoadData();
                mConversationArrayAdapter.Refresh();

                return true;
            }
        }
        return false;
    }
    private List<CheckMessage> LoadData(){
        //List<CheckMessage> Messages=new ArrayList<CheckMessage>();
        //Messages = ;
        //Messages.add(new CheckMessage(CheckMessage.MessageType_To, ""));
        return items;
    }

    //auto receiver
    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(getActivity(), "Receive.", Toast.LENGTH_SHORT).show();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(getActivity(), "Add.", Toast.LENGTH_SHORT).show();
                BluetoothDevice aaa = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.add(aaa);
                Toast.makeText(getActivity(), "Added.", Toast.LENGTH_SHORT).show();

            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getActivity(), "Finish.", Toast.LENGTH_SHORT).show();
                getActivity().unregisterReceiver(receiver);
                if(!device.isEmpty()){
                    //Autochat();
                }

            }

        }
    };
    /*
    private synchronized void Autochat() {
        boolean cflag = false;
        Toast.makeText(getActivity(), "In auto.", Toast.LENGTH_SHORT).show();
        if (device.isEmpty()){
            Toast.makeText(getActivity(), "Empty.", Toast.LENGTH_SHORT).show();
        }
        else {
            for(int count = 0; count < device.size(); count++) {
                Toast.makeText(getActivity(), "Connecting.", Toast.LENGTH_SHORT).show();
                cflag = mChatService.Autoconnect(device.get(count), false);
                if(cflag) {
                    Toast.makeText(getActivity(), "Success.", Toast.LENGTH_SHORT).show();
                    device.clear();
                    break;
                }
                //mChatService.failed();
            }
            Toast.makeText(getActivity(), "Fail.", Toast.LENGTH_SHORT).show();
            if(!cflag){
                Toast.makeText(getActivity(), "Set fails.", Toast.LENGTH_SHORT).show();
                device.clear();
                mChatService.failed();
            }

            /*while (!device.isEmpty()) {

                if (mChatService.Autoconnect(device.remove(0), false)){
                    //Toast.makeText(getActivity(), "break.", Toast.LENGTH_SHORT).show();
                    device.clear();
                    break;
                }
                else {
                    //Toast.makeText(getActivity(), "Fail.", Toast.LENGTH_SHORT).show();
                    if (device.isEmpty()) {
                        mChatService.failed();
                        //Toast.makeText(getActivity(), "Listen.", Toast.LENGTH_SHORT).show();
                    }

                }

            }
            //device.clear();
            lock = false;
        }
    }*/
    private void doDiscovery(){
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        getActivity().registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        getActivity().registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        mBluetoothAdapter.startDiscovery();
    }

    //XMPP
    public void XMPPconnect(){
        //MainActivity.check = true;
        mXMPPService = new XMPPService(getActivity(), mHandler, username, password);

        //chatroomtest
        XMPPing = true;
    }

    private void connectXMPPUser(Intent data){
        // Get user account
        XMPPing = true;
        Log.d(TAG, "XMPPing True.");
        //Toast.makeText(getActivity(), "XMPPing TRUE.", Toast.LENGTH_SHORT).show();
        String account = data.getExtras()
                .getString(XMPPListActivity.EXTRA_ACCOUNT);

        //mdata.clear();
        //mConversationArrayAdapter.Refresh();

        mXMPPService.startchat(account);
        //mXMPPname = account.split("@")[0];

    }
    public boolean filter(Long time, String name){

        return itemDB.Check(time,name);

    }

    public void relay(){

    }

/*
    public void setReceive(AbstractXMPPConnection connect) {
        //connection = connection;
        Chat chat = ChatManager.getInstanceFor(connect).createChat("jsmith@jivesoftware.com", new MessageListener() {
            @Override
            public void processMessage(Message message) {
                System.out.println("Received message: " + message);
            }
        });
    }*/

    private void startAdvertising() {
        Log.d(TAG, "Start fragment.");
        Context c = getActivity();
        c.startService(getServiceIntent(c));
    }


    private void stopAdvertising() {
        Log.d(TAG, "Stop fragment.");
        Context c = getActivity();
        c.stopService(getServiceIntent(c));

    }

    private static Intent getServiceIntent(Context c) {
        return new Intent(c, AdvertiserService.class);
    }
}
