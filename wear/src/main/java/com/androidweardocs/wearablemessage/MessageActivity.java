package com.androidweardocs.wearablemessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MessageActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    GoogleApiClient googleClient;

    Sensor accSensor;
    Sensor heartSensor;
    Sensor pedoSensor;
    String message;
    private SensorManager sensorManager;
    private SensorEventThread sensorThread;

    double temp_acc_x = 0;
    double temp_acc_y = 0;
    double temp_acc_z = 0;
    double temp_heart = 0;

    private Button btn_start;
    private Button btn_stop;
    private EditText heart;

    Calendar calendar;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    runThread recodingThread = new runThread();

    //Thread a;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        calendar = Calendar.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorThread = new SensorEventThread("SensorThread");

        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    // 가속도 센서
        heartSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        pedoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
/*
        a = new Thread(new Runnable() {
            public void run() {
                Log.v("dd", "a0");

                while (isRunning) {
                    try {
                        Thread.sleep(900);

                        if (temp_heart != 0) {
                            String currentDateTimeString = dateFormat.format(new Date(System.currentTimeMillis()));
                            message = currentDateTimeString + "," + String.valueOf(temp_heart)+ "," + String.valueOf(temp_acc_x).substring(0,6)
                                    + "," + String.valueOf(temp_acc_y).substring(0,6) + "," + String.valueOf(temp_acc_z).substring(0,6);
                            Log.v("dd", message);
                            new SendToDataLayerThread("/message_path", message).start();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        */

        recodingThread.start();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                heart = (EditText) findViewById(R.id.heartTEXT);
                btn_start = (Button) findViewById(R.id.btn_start);
                btn_stop = (Button) findViewById(R.id.btn_stop);

                btn_start.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if(v == btn_start)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "START", Toast.LENGTH_SHORT);
                            toast.show();
                            recodingThread.setRunningState(true);
                            startCapturing();
                        }
                    }
                });
                btn_stop.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if(v == btn_stop)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "STOP", Toast.LENGTH_SHORT);
                            toast.show();
                            recodingThread.setRunningState(false);
                            stopTest();
                        }
                    }
                });
            }
        });

        setAmbientEnabled();

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);


    }
    private void startCapturing() {
        Log.v("SensorTest", "Registering listeners for sensors");
        sensorManager.registerListener(sensorThread, accSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorThread.getHandler());
        sensorManager.registerListener(sensorThread, heartSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorThread.getHandler());
        //sensorManager.registerListener(sensorThread, pedoSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorThread.getHandler());
    }

    public void stopTest() {

        Log.v("SensorTest", "Un-Register Listner");
        sensorManager.unregisterListener(sensorThread);
        sensorThread.quitLooper();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        String message = "ON";
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/message_path", message).start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v("myTag", "Main activity received message: " + message);
            // Display message in UI
          //  mTextView.setText(message);
        }
    }

    class SensorEventThread extends HandlerThread implements
            SensorEventListener, Runnable {

        Handler handler;

        public SensorEventThread(String name) {
            super(name);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //Log.v("SensorTest", "onSensorChanged");
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if(Math.abs(temp_acc_x - event.values[0]) > 0.3 ||Math.abs(temp_acc_y - event.values[1]) > 0.3||Math.abs(temp_acc_z - event.values[2]) > 0.3)
                {
                    //Log.v("SensorTest", String.valueOf(event.values[0]) +" "+ String.valueOf(event.values[1])+" "+ String.valueOf(event.values[2]));
                    temp_acc_x = event.values[0];
                    temp_acc_y = event.values[1];
                    temp_acc_z = event.values[2];
                }
            }
            else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                temp_heart = event.values[0];
                heart.setText(String.valueOf(event.values[0]));

            }
            //else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
               // pedo.setText(String.valueOf(event.values[0]));
             //   Log.v("Pedo", String.valueOf(event.values[0]));

          //  }
          /*  if(temp_heart != 0) {
                String message;
                message = dateFormat.format(calendar.getTime())+" "+String.valueOf(temp_heart) + " " + String.valueOf(temp_acc_x) + " " + String.valueOf(temp_acc_y) + " " + String.valueOf(temp_acc_z);
                new SendToDataLayerThread("/message_path", message).start();
            }*/
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            handler = new Handler(sensorThread.getLooper());
        }

        public Handler getHandler() {
            return handler;
        }

        public void quitLooper() {
            if (sensorThread.isAlive()) {
                sensorThread.getLooper().quit();
            }
        }

    }

    class runThread extends Thread {

        private boolean isRunning  = true;

        public void run() {
            Log.v("dd", "a0");
            while(true){
                while (!isRunning) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                    try {
                        Thread.sleep(900);

                        if (temp_heart != 0) {
                            String currentDateTimeString = dateFormat.format(new Date(System.currentTimeMillis()));
                            message = currentDateTimeString + "," + String.valueOf(temp_heart) + "," + String.valueOf(temp_acc_x).substring(0, 6)
                                    + "," + String.valueOf(temp_acc_y).substring(0, 6) + "," + String.valueOf(temp_acc_z).substring(0, 6);
                            Log.v("dd", message);
                            new SendToDataLayerThread("/message_path", message).start();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
           // }
        }
            public void setRunningState(boolean state) {
                isRunning = state;
            }
    }
}
