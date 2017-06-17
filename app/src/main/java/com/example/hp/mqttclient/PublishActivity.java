package com.example.hp.mqttclient;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class PublishActivity extends AppCompatActivity {


    protected static final int RESULT_SPEECH = 2;

    public static MqttAndroidClient client = MainActivity.client;

    public static String clientId = MainActivity.clientId;


    public Button publish_Button;

    public EditText message,topic;

    public String publish_message, publish_topic;

    public Button speakBtn;

    public TextView speech,speech2;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        SharedPreferences sp1=this.getSharedPreferences("MQTT_Details", MODE_PRIVATE);

        String ip =sp1.getString("sharedPreference_ip", null);
        String tp = sp1.getString("sharedPreference_topic", null);

        Toast.makeText(PublishActivity.this, ip, Toast.LENGTH_LONG).show();

        Toast.makeText(PublishActivity.this, tp, Toast.LENGTH_LONG).show();



        publish_Button = (Button)findViewById(R.id.publish);

        message = (EditText) findViewById(R.id.message);
        topic = (EditText) findViewById(R.id.topicToSubscribe);

        speakBtn = (Button)findViewById(R.id.speak);

        speech = (TextView) findViewById(R.id.speech);

        speech2 = (TextView) findViewById(R.id.speech2);


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
}
