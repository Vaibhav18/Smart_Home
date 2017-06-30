package com.example.hp.mqttclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MainActivity extends AppCompatActivity {

    public static final String MyPREFERENCES = "MQTT_Details";
    public static final String IP_ADDRESS = "sharedPreference_ip";
    public static final String YOUR_TOPIC = "sharedPreference_topic";

    public Button connect_Button;
    public EditText broker_ipAddress;
    public String ipAddress_broker;
    public EditText topic;
    public static MqttAndroidClient client;
    public static String clientId;
    public Boolean exists=false;

    private SharedPreferences sharedpreferences;

    public String shared_ip, shared_topic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        if(sharedpreferences.contains(IP_ADDRESS) && sharedpreferences.contains(YOUR_TOPIC)){
            exists = true;
            sharedPreferences();          //if user is currently logged in
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        connect_Button = (Button)findViewById(R.id.connect);
        broker_ipAddress = (EditText)findViewById(R.id.broker_ip);
        topic = (EditText)findViewById(R.id.your_topic);

        connect_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shared_ip = broker_ipAddress.getText().toString();
                shared_topic = topic.getText().toString();
                connectMQTT();

            }
        });
    }

    public void connectMQTT(){

        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(MainActivity.this, "tcp://" + shared_ip,
                clientId);
        try{
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    // Connected successfully
                    Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_LONG).show();

                    if(exists == false) {
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(IP_ADDRESS, shared_ip);
                        editor.commit();
                    }
                    subscribeMQTT();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "unsuccessful", Toast.LENGTH_LONG).show();
                }
            });

        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void subscribeMQTT(){

        try {
            IMqttToken subToken = client.subscribe(shared_topic, 1);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    // The message was published
                    //Toast.makeText(MainActivity.this, "subscribed", Toast.LENGTH_LONG).show();
                    client.setCallback(new ReceiveMessage(MainActivity.this));
                    if(exists==false)
                    {
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(YOUR_TOPIC, shared_topic);
                        editor.commit();
                    }
                    Intent intent = new Intent(MainActivity.this, PublishActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                    Toast.makeText(MainActivity.this, "not subscribed", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sharedPreferences(){

             shared_ip =sharedpreferences.getString("sharedPreference_ip", null);
             shared_topic = sharedpreferences.getString("sharedPreference_topic", null);
             connectMQTT();         //if user is currently logged in;

    }
}
