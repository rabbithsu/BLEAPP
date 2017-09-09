package com.example.android.BLEChat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.common.logger.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * Created by nccu_dct on 15/9/22.
 */
public class LoginXMPPActivity extends Activity {

    private static final String TAG = "LoginXMPPActivity";

    private EditText UserEditText;
    private EditText PwEditText;
    private Button SigninButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d(TAG, "Login");

        // Setup the window
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_xmpplogin);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        // Set content
        UserEditText = (EditText) findViewById(R.id.editText);
        PwEditText = (EditText) findViewById(R.id.editText2);
        SigninButton = (Button) findViewById(R.id.button);
        SigninButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView userview = (TextView) findViewById(R.id.editText);
                TextView pwview = (TextView) findViewById(R.id.editText2);
                TextView nameview = (TextView) findViewById(R.id.editText3);
                String username = userview.getText().toString();
                String pw = pwview.getText().toString();
                String name = nameview.getText().toString();
                //pwview.setText("");
                Login(username, pw, name);

            }
        });

    }
    private void Login(String u, String p, String n){
        final String username = u;
        final String pw = p;
        final String name = n;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                AbstractXMPPConnection connection;
                XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
                config.setServiceName("140.119.164.5");
                config.setHost("140.119.164.5");
                config.setPort(5225);
                config.setUsernameAndPassword(username, pw);
                //config.setDebuggerEnabled(true);
                config.setCompressionEnabled(false);
                config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);


                //config.setSASLAuthenticationEnabled(false);
                connection = new XMPPTCPConnection(config.build());
                try {

                    connection.connect();
                    Log.d(TAG, "Connected to " + connection.getHost());
                    connection.disconnect();
                    //PwEditText.setText("");
                    // Create the result Intent and include the MAC address
                    Intent intent = new Intent();
                    intent.putExtra("USER", username);
                    intent.putExtra("PW", pw);
                    intent.putExtra("NAME", name);

                    // Set result and finish this Activity
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (Exception ex) {
                    //PwEditText.setText("NO");
                    Log.d(TAG, "Failed to connect to "
                            + connection.getHost());
                    Log.d(TAG, ex.toString());
                }
            }});
        t.start();


    }

}
