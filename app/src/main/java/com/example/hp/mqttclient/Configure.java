package com.example.hp.mqttclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Configure extends AppCompatActivity {

    public EditText homeNetworkSSID;
    public EditText homeNetworkPwd;
    public EditText topicName;
    public EditText brokerIP;

    public TextView content;

    public Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        homeNetworkSSID = (EditText)findViewById(R.id.ssid);
        homeNetworkPwd = (EditText)findViewById(R.id.password);
        topicName = (EditText)findViewById(R.id.topic_name);
        brokerIP = (EditText)findViewById(R.id.broker);

        content = (TextView)findViewById(R.id.content);

        submit = (Button)findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestQueue queue = Volley.newRequestQueue(Configure.this);

                String url = "http://192.168.4.1";

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                content.setText(response);
                            }
                        }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        content.setText("That didn't work!");
                    }

                }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("SSID", homeNetworkSSID.getText().toString());
                        params.put("PWD", homeNetworkPwd.getText().toString());
                        params.put("TOPIC", topicName.getText().toString());
                        params.put("IP", brokerIP.getText().toString());
                        return params;
                    }

                };
                queue.add(stringRequest);


            }
        });

    }
}
