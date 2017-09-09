package com.example.android.BLEChat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.util.Collection;
import com.example.android.common.logger.Log;

/**
 * Created by nccu_dct on 15/8/26.
 */
public class XMPPListActivity extends Activity {

    private static final String TAG = "XMPPListActivity";
    public static String EXTRA_ACCOUNT = "remote_account";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.xmpp_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        ArrayAdapter<String> XMPPDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);

        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(XMPPDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mXMPPClickListener);

        try {
            Roster roster = Roster.getInstanceFor(BluetoothChatFragment.mXMPPService.getConnection());
            if (!roster.isLoaded())
                roster.reloadAndWait();
            Collection<RosterEntry> entries = roster.getEntries();
            if (entries.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (RosterEntry entry : entries) {
                    XMPPDevicesArrayAdapter.add(entry.getName() + "\n" + entry.getUser());
                }
            } else {
                String noDevices = getResources().getText(R.string.none_paired).toString();
                XMPPDevicesArrayAdapter.add(noDevices);
            }
            //broadcast
            XMPPDevicesArrayAdapter.add("Broadcast"+"\n"+"All online users.");
        }
        catch (Exception ex){
            Log.d(TAG, "FAIL:" + ex.toString());
        }
        //for (RosterEntry entry : entries) {}

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private AdapterView.OnItemClickListener mXMPPClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            //mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String account = info.split("\n")[1];
            //if Broadcast
            if(account.equals("All online users.")){
                account = "all@broadcast.140.119.164.5";
            }

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_ACCOUNT, account);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
}
