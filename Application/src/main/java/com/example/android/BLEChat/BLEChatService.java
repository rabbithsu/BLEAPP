package com.example.android.BLEChat;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rabbithsu on 2016/5/23.
 */
public class BLEChatService extends Service {
    // Debugging
    private static final String TAG = "BLEChatService";
    private static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_UUID = UUID
            .fromString("00002A19-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int mState;

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private final Handler ServiceHandler;
    private final Context mContext;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private static final int STATE_DISCONNECTED = 4;

    private BluetoothGattService mBluetoothGattService;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    //scan
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private ScanResultAdapter mScanAdapter;
    private static final long SCAN_PERIOD = 5000;
    private Handler mScanHandler;

    //advertise
    public static final String ADVERTISING_FAILED =
            "com.example.android.bluetoothadvertisements.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int ADVERTISING_TIMED_OUT = 6;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private Handler mTHandler;
    private Runnable timeoutRunnable;
    private String deviceAddress;

    //connect
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mCharacteristic;
    private boolean autoing = false ;
    private boolean CENTRAL = false;
    private boolean PERIPHERAL = false;



    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private String autoaddress = null;


    public BLEChatService(Context context, Handler handler) {

        Log.d(TAG, "in");
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mContext = context;
        mAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        ServiceHandler = new Handler(context.getMainLooper());
        mScanHandler = new Handler();
        mBluetoothLeScanner = mAdapter.getBluetoothLeScanner();

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        deviceAddress = "QQQQQ";

        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        BluetoothGattService mService = new BluetoothGattService(BATTERY_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mCharacteristic =
                new BluetoothGattCharacteristic(BATTERY_LEVEL_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        | BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mCharacteristic.setValue("");
        mService.addCharacteristic(mCharacteristic);
        mGattServer.addService(mService);

        initialize();




        //BTname = mAdapter.getName();



    }
    private void runOnUiThread(Runnable r) {
        ServiceHandler.post(r);
    }

    public BLEChatService(){
        mAdapter = null;
        mHandler = null;
        ServiceHandler = null;
        mContext = null;
    }

    public void start(){

    }

    public synchronized void stop() {
        Log.d(TAG, "stop");
    }

    public void connect(BluetoothDevice device, boolean secure){

    }

    public void write(String message){
        if(PERIPHERAL) {
            mCharacteristic.setValue(message.getBytes());
            byte[] buffers = message.getBytes();

            Log.w(TAG, "Write: " + message);
            //Log.w(TAG, "Write: " + );

            int bytess = buffers.length;



            mHandler.obtainMessage(Constants.MESSAGE_WRITE, bytess, -1, buffers)
                    .sendToTarget();

            sendNotificationToDevices(mCharacteristic);

            /*for(int i = 1; i <101; i++){
                message = i + "##" +System.currentTimeMillis()+"\n";
                mCharacteristic.setValue(message.getBytes());
                byte[] buffers = message.getBytes();

                Log.w(TAG, "Write: " + message);
                //Log.w(TAG, "Write: " + );

                int bytess = buffers.length;



                mHandler.obtainMessage(Constants.MESSAGE_WRITE, bytess, -1, buffers)
                        .sendToTarget();

                sendNotificationToDevices(mCharacteristic);
                try{
                    Thread.sleep(150);
                }catch (Exception e){
                    Log.w("Sleep.", e.getMessage());
                }

            }*/
        }

        if(CENTRAL){
            mNotifyCharacteristic.setValue(message.getBytes());
            mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);

            /*for(int i = 1; i <101; i++){
                message = i + "##" +System.currentTimeMillis()+"\n";
                mNotifyCharacteristic.setValue(message.getBytes());
                mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
                try{
                    Thread.sleep(150);
                }catch (Exception e){
                    Log.w("Sleep.", e.getMessage());
                }
            }*/


        }
        //bluewriteCharacteristic();
    }

    public void relaying(String message){

    }

    public int getState(){
        return mState;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }



    //scan
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                    Log.d("A","A");
                    if(autoaddress != null){
                        Log.d("B", "B");
                        //connect(autoaddress);
                    }
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
        if(autoaddress!=null&&!autoing) {
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
                    autoaddress = result.getDevice().getAddress();
                    if(!autoing) {
                        autoing = true;
                        connect(autoaddress);
                    }
                    //scanaddress = result.getDevice().getAddress();

                }catch (Exception e){
                    Log.e(TAG, "No data.");
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
                //Log.d(TAG, new String(result.getScanRecord().getServiceData(Constants.Service_UUID)));
                autoaddress = result.getDevice().getAddress();

                if(!autoing&&!CENTRAL&&!PERIPHERAL) {
                    autoing = true;
                    connect(autoaddress);
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

    //advertise
    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            //BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                deviceAddress = mBluetoothAdapter.getAddress();


                //deviceAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");


                Log.d(TAG, deviceAddress);
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, "bt_null", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "bt_null", Toast.LENGTH_LONG).show();
            }
        }
        startAdvertising();
        autoing = false;
        startScanning();

    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout(){
        mTHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AdvertiserService has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mTHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");

        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdvertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data,
                        mAdvertiseCallback);
            }
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
        }
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.Service_UUID);
        //dataBuilder.addServiceData(Constants.Service_UUID, deviceAddress.getBytes());
        //dataBuilder.setIncludeDeviceName(true);

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(TAG, "Advertising failed");
            sendFailureIntent(errorCode);
            stopSelf();

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the {@code AdvertiserFragment}.
     */
    private void sendFailureIntent(int errorCode){
        Intent failureIntent = new Intent();
        failureIntent.setAction(ADVERTISING_FAILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        mContext.sendBroadcast(failureIntent);
    }

    //connect
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Log.d(TAG,"connecting...");
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    //broadcast update
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (false){//UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data));// + "\n" + stringBuilder.toString());
            }
        }
        mContext.sendBroadcast(intent);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("BroadcastReceiver", action);
            if (ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, 0, -1).sendToTarget();
                CENTRAL = true;
                //invalidateOptionsMenu();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                //mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, 0, -1).sendToTarget();
                CENTRAL = false;
                //invalidateOptionsMenu();
                //clearUI();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(getSupportedGattServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(EXTRA_DATA));
            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //GATT service


    private void updateConnectedDevicesStatus() {
        final String message = "status_devicesConnected" +" "+//getString(R.string.status_devicesConnected) + " "
                + mBluetoothManager.getConnectedDevices(BluetoothGattServer.GATT).size();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, message);
                //mConnectionStatus.setText(message);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            byte[] buffers = data.getBytes();

            Log.w(TAG, data);

            int bytess = buffers.length;

            mHandler.obtainMessage(Constants.MESSAGE_READ, bytess, -1, buffers)
                    .sendToTarget();
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            Log.d(TAG, "AA");
            return;
        }
        Log.d(TAG, "AB");
        String uuid = null;
        String unknownServiceString = "unknown_service";//getResources().getString(R.string.unknown_service);
        String unknownCharaString = "unknown_characteristic;";//getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, "A111");
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            byte[] buffer = SampleGattAttributes.lookup(uuid, unknownServiceString).getBytes();
            int bytes = buffer.length;

            //mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
            //        .sendToTarget();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.d(TAG, "A2222");
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);


                //final int charaProp = gattCharacteristic.getProperties();
                //if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                //if(uuid.equals(BATTERY_LEVEL_UUID)){
                    if (mNotifyCharacteristic != null) {
                        setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    readCharacteristic(gattCharacteristic);
                //}
                //if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = gattCharacteristic;
                    setCharacteristicNotification(
                            gattCharacteristic, true);
                //}
                //}


                if(mBluetoothGatt.readCharacteristic(gattCharacteristic))
                    Log.d(TAG,"True");
                byte[] buffers = gattCharacteristic.getValue();
                if(buffers==null){
                    Log.w(TAG, "Null.");
                    continue;
                }
                int bytess = buffers.length;

                //mHandler.obtainMessage(Constants.MESSAGE_READ, bytess, -1, buffers)
                //        .sendToTarget();
            }
            mGattCharacteristics.add(charas);


            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        /*SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );*/

        //mGattServicesList.setAdapter(gattServiceAdapter);
    }

    public int writesCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        Log.d(TAG, "Writing...");
        mCharacteristic.setValue(value);
        return 0;
        //throw new UnsupportedOperationException("Method writeCharacteristic not overriden");
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, String.valueOf(resourceId));
                //mConnectionState.setText(resourceId);
            }
        });
    }
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    //server
    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                    PERIPHERAL = true;

                    mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, 0, -1).sendToTarget();
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Disconnected from device");
                    PERIPHERAL = false;
                }
            } else {
                mBluetoothDevices.remove(device);
                updateConnectedDevicesStatus();
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                final String errorMessage = "status_errorWhenConnecting " + ": " + status;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
            int status = 0;//
            //writeCharacteristic(characteristic, offset, value);
            mCharacteristic.setValue(value);
            int bytes = value.length;

            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, value)
                    .sendToTarget();
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            if(responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };
    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        return new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG,"Set");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/
    }
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG,"Read");
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothDevices.isEmpty()) {
            Toast.makeText(this, "No device.", Toast.LENGTH_SHORT).show();
        } else {
            boolean indicate = (characteristic.getProperties()
                    & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                    == BluetoothGattCharacteristic.PROPERTY_INDICATE;
            for (BluetoothDevice device : mBluetoothDevices) {
                // true for indication (acknowledge) and false for notification (unacknowledge).
                mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
            }
        }
    }



    //error control
}
