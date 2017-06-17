package com.example.hp.mqttclient;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A simple {@link Fragment} subclass.
 */
public class MQTTFragment extends Fragment implements MqttCallback {


    public static final String TAG = "MQTTFragment.tag";

    public static MqttAndroidClient client;
    public String clientId;

    public static Context mainContext , subContext;
    //Context context;

    public MQTTFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setRetainInstance(true);
    }

    public void connectMQTT(Context context){

        mainContext = context;
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(mainContext, "tcp://192.168.0.6",
                clientId);
        try{
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("hi","onSuccess");
                    Toast.makeText(mainContext, "connected", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("hi", "onFailure");
                    // Toast.makeText(context, "not connected",
                    //  Toast.LENGTH_LONG).show();
                }
            });


        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void subscribeMQTT(Context context, String topic){
        int qos = 1;
        subContext = context;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published

                    Toast.makeText(subContext, "subscribed", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                    Toast.makeText(subContext, " not subscribed", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
    }




    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
