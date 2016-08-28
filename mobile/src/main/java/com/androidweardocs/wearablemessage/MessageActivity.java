package com.androidweardocs.wearablemessage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MessageActivity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    GoogleApiClient googleClient;
    File fileWrite;
    String directory;
    JSONObject obj_hr, obj2;
    JSONObject obj_x;
    JSONObject obj_y;
    JSONObject obj_z;
    Button btn_send;
    EditText edit_msg, edit_receive;
    int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 1;
    JSONArray sendMSG;

    Calendar calendar;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    String serverURL = "http://202.30.29.209:14242/api/put";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        btn_send = (Button) findViewById(R.id.send_btn);
        edit_msg = (EditText) findViewById(R.id.edit_msg);
        edit_receive = (EditText) findViewById(R.id.edit_receive);
        directory = "/sdcard/wearable/";
        btn_send.setOnClickListener(this);
        String currentDateTimeString = dateFormat.format(new Date(System.currentTimeMillis()));
        String filename = currentDateTimeString + ".csv";
        fileWrite = new File(directory+filename);

        if(fileWrite.exists() == false) {
            BufferedWriter bufferedWriter = null;
            String header = "timestamp, HR, acc_x, acc_y, acc_z\n";
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(fileWrite, true));
                bufferedWriter.write(header);
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Build a new GoogleApiClient that includes the Wearable API
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    public void onClick(View v){
        if(v == btn_send) {
            Toast toast = Toast.makeText(this, "SEND", Toast.LENGTH_SHORT);
            toast.show();
            String message = edit_msg.getText().toString();
            new SendToDataLayerThread("/message_path", message).start();
        }
    }
    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Message Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.androidweardocs.wearablemessage/http/host/path")
        );
        AppIndex.AppIndexApi.start(googleClient, viewAction);
    }

    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
        String message = "what the..";
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/message_path", message).start();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Message Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.androidweardocs.wearablemessage/http/host/path")
        );
        AppIndex.AppIndexApi.end(googleClient, viewAction);
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    // Unused project wizard code
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }


    public String sendJSON(String jsonMSG, String serverURL){


        OutputStream os = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        HttpURLConnection conn = null;
        String response = "";
        URL url = null;
        try {
            url = new URL(serverURL);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(5*1000);
            conn.setReadTimeout(5*1000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cache-control", "no-cache");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            os = conn.getOutputStream();
            os.write(jsonMSG.getBytes());
            os.flush();
            int responseCode = conn.getResponseCode();
            Log.v("dd", String.valueOf(responseCode));

            if(responseCode == HttpURLConnection.HTTP_OK)
            {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                byte[] byteBuffer = new byte[1024];
                byte[] byteData = null;
                int nLength = 0;
                while((nLength = is.read(byteBuffer, 0, byteBuffer.length)) != -1){
                    baos.write(byteBuffer, 0, nLength);
                }
                byteData = baos.toByteArray();
                response = new String(byteData);

                Log.v("dd", "DATA response = " + response);

            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            /*
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileWrite, true));
                if(!message.equals("ON"))
                    bufferedWriter.write(message+"\n");
                bufferedWriter.close();
                //TODO:메세지 형식이 맞으면 입력하기
            } catch (IOException e) {
                e.printStackTrace();
            }
*/
            if(!message.equals("ON")) {
                String[] parse = message.split(",");
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                Date date = null;
                try {
                    date = dateFormat.parse(parse[0]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long time = date.getTime();
                new Timestamp(time);

                System.out.println(time);
                time = time / 1000;
                obj_hr = new JSONObject();
                obj_x = new JSONObject();
                obj_y = new JSONObject();
                obj_z = new JSONObject();

                sendMSG = new JSONArray();
                try {
                    obj_hr.put("metric", edit_msg.getText());
                    obj_hr.put("timestamp", time);
                    obj_hr.put("value", parse[1]);
                    obj2 = new JSONObject();
                    obj2.put("host", "hr");
                    obj2.put("cpu", 0);
                    obj_hr.put("tags", obj2);
                    Log.v("dd", obj_hr.toString());

                    sendMSG.put(obj_hr);

                    obj_x.put("metric", edit_msg.getText());
                    obj_x.put("timestamp", time);
                    obj_x.put("value", parse[2]);
                    obj2 = new JSONObject();
                    obj2.put("host", "x");
                    obj2.put("cpu", 0);
                    obj_x.put("tags", obj2);
                    Log.v("dd", obj_x.toString());

                    sendMSG.put(obj_x);

                    obj_y.put("metric", edit_msg.getText());
                    obj_y.put("timestamp", time);
                    obj_y.put("value", parse[3]);
                    obj2 = new JSONObject();
                    obj2.put("host", "y");
                    obj2.put("cpu", 0);
                    obj_y.put("tags", obj2);
                    Log.v("dd", obj_y.toString());

                    sendMSG.put(obj_y);

                    obj_z.put("metric", edit_msg.getText());
                    obj_z.put("timestamp", time);
                    obj_z.put("value", parse[4]);
                    obj2 = new JSONObject();
                    obj2.put("host", "z");
                    obj2.put("cpu", 0);
                    obj_z.put("tags", obj2);
                    Log.v("dd", obj_z.toString());

                    sendMSG.put(obj_z);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendJSON(sendMSG.toString(), serverURL);

                    }
                }).start();
            }
            edit_receive.setText(message);
        }
    }
}