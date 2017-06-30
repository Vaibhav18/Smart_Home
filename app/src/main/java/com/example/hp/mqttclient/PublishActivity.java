package com.example.hp.mqttclient;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.ParcelUuid;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;

public class PublishActivity extends AppCompatActivity {


    protected static final int RESULT_SPEECH = 2;

    public static MqttAndroidClient client = MainActivity.client;

    public static String clientId = MainActivity.clientId;


    public Button publish_Button;

    public EditText message,topic;

    public String publish_message, publish_topic;

    public Button speakBtn;

    public TextView speech,speech2;

    private StringBuilder sb = new StringBuilder();
    List<ScanResult> scanList;
    Button scan;
    ListView listview;
    WifiManager wifiManager;
    String AP_SSID;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        SharedPreferences sp1=this.getSharedPreferences("MQTT_Details", MODE_PRIVATE);

        String ip =sp1.getString("sharedPreference_ip", null);
        String tp = sp1.getString("sharedPreference_topic", null);
        String test= ip+"   "+tp;

        Toast.makeText(PublishActivity.this, test, Toast.LENGTH_LONG).show();
        //Toast.makeText(PublishActivity.this, tp, Toast.LENGTH_LONG).show();


        publish_Button = (Button)findViewById(R.id.publish);
        message = (EditText) findViewById(R.id.message);
        topic = (EditText) findViewById(R.id.topicToSubscribe);
        speakBtn = (Button)findViewById(R.id.speak);
        speech = (TextView) findViewById(R.id.speech);
        speech2 = (TextView) findViewById(R.id.speech2);

        scan = (Button)findViewById(R.id.scanWifi);
        listview = (ListView)findViewById(R.id.list);




        speakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    speech.setText("");
                    speech2.setText("");

                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Opps! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        publish_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publish_message = message.getText().toString();
                publish_topic = topic.getText().toString();

                byte[] encodedPayload = new byte[0];
                try{
                    encodedPayload = publish_message.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    client.publish(publish_topic, message);
                }catch (UnsupportedEncodingException | MqttException e){
                    e.printStackTrace();
                }
            }
        });


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tv.setText("scanning");
                getWifiNetworksList();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    speech.setText(text.get(0));
                    speech2.setText(text.get(1));
                }
                break;

            }

        }
    }


    private void getWifiNetworksList(){


        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiManager =
                (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);;
        registerReceiver(new BroadcastReceiver(){

            @SuppressLint("UseValueOf") @Override
            public void onReceive(Context context, Intent intent) {
                sb = new StringBuilder();
                scanList = wifiManager.getScanResults();
                sb.append("\n  Number Of Wifi connections :" + " " +scanList.size()+"\n\n");
                final ArrayList<String> list = new ArrayList<String>();
                for(int i = 0; i < scanList.size(); i++){
                    sb.append(new Integer(i+1).toString() + ". ");
                    sb.append((scanList.get(i)).toString());
                    list.add(scanList.get(i).toString());


                    sb.append("\n\n");
                }
                final StableArrayAdapter adapter = new StableArrayAdapter(PublishActivity.this,
                        android.R.layout.simple_list_item_1, list);
                listview.setAdapter(adapter);

                //tv.setText(sb);

                listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, final View view,
                                            int position, long id) {
                        final String item = (String) parent.getItemAtPosition(position);

                        AP_SSID = scanList.get(position).SSID;
                        connectToAP(AP_SSID,"");
                        Intent i = new Intent(PublishActivity.this,Configure.class);
                        startActivity(i);
                    }

                });
            }

        },filter);
        wifiManager.startScan();
    }


    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }



        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    public void connectToAP(String ssid, String passkey) {
        //Log.i(TAG, "* connectToAP");

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        String networkSSID = ssid;
        String networkPass = passkey;

        //  Log.d(TAG, "# password " + networkPass);

        for (ScanResult result : scanList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    // Log.d(TAG, "# add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    // Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);

                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    // Log.d(TAG, "### 1 ### add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    // Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);
                }

                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                int res = wifiManager.addNetwork(wifiConfiguration);
                // Log.d(TAG, "### 2 ### add Network returned " + res);

                wifiManager.enableNetwork(res, true);

                boolean changeHappen = wifiManager.saveConfiguration();

                if(res != -1 && changeHappen){
                    // Log.d(TAG, "### Change happen");

                    // AppStaticVar.connectedSsidName = networkSSID;

                }else{
                    // Log.d(TAG, "*** Change NOT happen");
                }

                wifiManager.setWifiEnabled(true);
            }
        }
    }


    public String getScanResultSecurity(ScanResult scanResult) {
        //Log.i(TAG, "* getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WEP", "PSK", "EAP" };

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }


}
