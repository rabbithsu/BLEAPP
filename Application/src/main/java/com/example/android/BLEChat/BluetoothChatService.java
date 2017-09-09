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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.example.android.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService{
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    //xmpp
    //Multi connect
    private ArrayList<ConnectedThread> multiBT = new ArrayList<ConnectedThread>();
    private final String BTname;

    //ble auto connect
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private ScanResultAdapter mScanAdapter;
    private static final long SCAN_PERIOD = 5000;
    private Handler mScanHandler;
    boolean autoing = false;
    boolean tryed = false;
    private String scanaddress;
    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        BTname = mAdapter.getName();
        //
        mScanHandler = new Handler();
        mBluetoothLeScanner = mAdapter.getBluetoothLeScanner();
        //mScanCallback = new SampleScanCallback();
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        /*if (mAutothread != null) {
            mAutothread.cancel();
            mAutothread = null;
        }*/

        // Cancel any thread currently running a connection
        /*if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }*/
        if(multiBT.isEmpty()) {
            setState(STATE_LISTEN);
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
        try{
            //Thread.sleep(5000);
            if(mState != STATE_CONNECTED && !tryed){
                tryed = true;
                scanstart();
            }
        }
        catch (Exception e){
            Log.d(TAG, "Cant sleep." + e.toString());
        }

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
            /*if (mAutothread != null) {
                mAutothread.cancel();
                mAutothread = null;
            }*/
        }

        // Cancel any thread currently running a connection
        /*if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }*/

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        /*if (mAutothread != null) {
            mAutothread.cancel();
            mAutothread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        */
        // Start the thread to manage the connection and perform transmissions
        multiBT.add(new ConnectedThread(socket, socketType, multiBT.size()));
        multiBT.get(multiBT.size()-1).start();


        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
        tryed =false;

    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        /*if (mAutothread != null) {
            mAutothread.cancel();
            mAutothread = null;
        }*/

        /*if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }*/

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     */
    public void write(String out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (multiBT.isEmpty()) return;
            for(ConnectedThread w : multiBT){
                w.write(out);
            }
        }
    }
    public void relaying(String out){
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (multiBT.isEmpty()) return;
            for(ConnectedThread w : multiBT){
                w.relay(out);
            }
        }

    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        //if(!autoing){
            // Start the service over to restart listening mode
            BluetoothChatService.this.start();
        //}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(int num) {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        multiBT.remove(num);
        for(int co = num; co < multiBT.size(); co++){
            multiBT.get(co).setNum(co);
        }

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (true){//mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                                // Either not ready or already connected. Terminate new socket.
                                /*try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;*/
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;


        public ConnectThread(BluetoothDevice device, boolean secure) {


            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }


                connectionFailed();
                return;
            }


            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int BTnumber;

        public ConnectedThread(BluetoothSocket socket, String socketType, int num) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            Log.d(TAG, "ConnetedThread number: " + num);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            BTnumber = num;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            BluetoothChatService.this.start();
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            String message;

            //get sync
            boolean check = false;
            long ts = BluetoothChatFragment.itemDB.getMaxTs();
            List<CheckMessage> hdata = null;
            this.write("TimeStampCheck##"+ts+"##"+ts+"##");
            while(!check){
                try {
                    bytes = mmInStream.read(buffer);
                    message = new String(buffer);
                    String[] TsCheck = message.split("##");
                    if (TsCheck.length > 1 && TsCheck[0].equals("TimeStampCheck")){
                        check = true;
                        if (Long.parseLong(TsCheck[1]) < ts){
                            //List<CheckMessage> hdata = new ArrayList<>();

                            hdata = BluetoothChatFragment.itemDB.getHistory(Long.parseLong(TsCheck[1]), ts);
                            //hdata.add(new CheckMessage(0, ts, CheckMessage.MessageType_From, "Test", "TTTT"));
                            //hdata.add(new CheckMessage(0, ts, CheckMessage.MessageType_From, "Test2", "T222"));
                            /*for (CheckMessage m : hdata){
                                this.sendSync(m.getName() + "##" + m.getContent() + "##" + m.getTime());
                            }*/
                        }
                    }


                } catch (IOException e){
                    Log.e(TAG, "disconnected", e);
                    connectionLost(BTnumber);
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                    // Start the service over to restart listening mode
                    //BluetoothChatService.this.start();
                }
            }
            if (!(hdata == null)) {
                for (CheckMessage m : hdata) {
                    if(this.sendSync(m.getName() + "##" + m.getContent() + "##" + m.getTime())){

                    }
                    else{
                        Log.d(TAG, "Sync doesn't complete.");
                    }
                }
            }



            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.d(TAG, "");

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(BTnumber);
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param message The bytes to write
         */
        public void write(String message) {
            try {
                //byte[] buffer = (BTname+"##"+message).getBytes();
                byte[] buffer = (message).getBytes();
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                if(BTnumber ==0){
                    mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                }

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void relay(String message) {
            try {
                //byte[] buffer = (BTname+"##"+message).getBytes();
                byte[] buffer = (message).getBytes();
                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during relay.", e);
            }
        }

        public boolean sendSync(String message) {
            try {
                //byte[] buffer = (BTname+"##"+message).getBytes();
                byte[] buffer = (message).getBytes();
                mmOutStream.write(buffer);
                Log.d(TAG, message);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d(TAG, "error");
                }
                // Share the sent message back to the UI Activity
                /*if(BTnumber ==0){
                    mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                }*/
                return true;

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                return false;
            }
        }
        public boolean sync(){
            return true;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        public void setNum(int num){
            BTnumber = num;
        }
    }


    public void scanstart(){
        autoing = false;
        startScanning();
    }

    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                    ///autoing = false;
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

            /*String toastText = getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds);*/
            //Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_SHORT);
            Log.e(TAG, "Can't Starting Scanning");
        }
    }

    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        if(scanaddress!=null&&!autoing) {
            Log.d(TAG, String.valueOf(autoing));
            autoing = true;
            //connect(mAdapter.getRemoteDevice(scanaddress), false);


        }
        // Even if no new results, update 'last seen' times.
        //mScanAdapter.notifyDataSetChanged();
    }

    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "ScanResult s.");

            for (ScanResult result : results) {
                //mScanAdapter.add(result);
                Log.d(TAG, result.getDevice().getName());
                Log.d(TAG, result.getDevice().getAddress());
                try{
                    Log.d(TAG, new String(result.getScanRecord().getServiceData(Constants.Service_UUID)));
                    //scanaddress = result.getDevice().getAddress();
                    scanaddress = new String(result.getScanRecord().getServiceData(Constants.Service_UUID));
                }catch (Exception e){
                    Log.e(TAG, "No datas.");
                }
                //Log.d(TAG, new String(result.getScanRecord().getServiceData(Constants.Service_UUID)));
                //connect(result.getDevice(), false);
            }
            //mScanAdapter.notifyDataSetChanged();
        }


        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            /*mScanAdapter.add(result);
            mScanAdapter.notifyDataSetChanged();*/
            Log.d(TAG, "ScanResult:" +callbackType);
            Log.d(TAG, result.getDevice().getName());
            Log.d(TAG, result.getDevice().getAddress());
            try{
                Log.d(TAG, new String(result.getScanRecord().getServiceData(Constants.Service_UUID)));
                scanaddress = new String(result.getScanRecord().getServiceData(Constants.Service_UUID));
                if(!autoing) {
                    autoing = true;
                    connect(mAdapter.getRemoteDevice(scanaddress), false);
                }
                //scanaddress = result.getDevice().getAddress();

            }catch (Exception e){
                Log.e(TAG, "No data.");
            }
            //connect(result.getDevice(), false);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "ScanFailed.");
            //Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
            //        .show();
        }
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        return builder.build();
    }



}
