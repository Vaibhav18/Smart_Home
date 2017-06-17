package com.example.hp.mqttclient;

import android.content.ContextWrapper;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by HP on 6/16/2017.
 */
public class ReceiveMessage implements MqttCallback{
    private ContextWrapper context;
    private String messageRecieved;

    public ReceiveMessage(ContextWrapper context) {
        this.context = context;
    }
    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        messageRecieved = new String(message.getPayload());
        Toast.makeText(this.context, messageRecieved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
