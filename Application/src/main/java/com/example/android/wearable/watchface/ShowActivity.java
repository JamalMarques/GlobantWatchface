package com.example.android.wearable.watchface;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class ShowActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    private EditText editText;
    private Button senderButton;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        //Building a Google Api Client
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API)
                                                           .addConnectionCallbacks(this)
                                                           .addOnConnectionFailedListener(this)
                                                           .build();

        editText = (EditText)findViewById(R.id.editText);
        senderButton = (Button)findViewById(R.id.senderButton);
        senderButton.setOnClickListener(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.globant, menu);
        return true;
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


    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if( googleApiClient != null && googleApiClient.isConnected()){
            googleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}


    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onClick(View v) {
        if( v == senderButton){
            if(editText.getText().length() > 0) {

                String msj = editText.getText().toString();
                //new SendToDataLayerThread( WEARABLE_DATA_PATH, msj).start();*/

                // Create a DataMap object and send it to the data layer
                DataMap dataMap = new DataMap();
                dataMap.putString(Constants.MAP_ACTION_NUMBER, msj);
                //Requires a new thread to avoid blocking the UI
                new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();

            }else{
                Toast.makeText(this,getResources().getString(R.string.complete_field),Toast.LENGTH_SHORT).show();
            }
        }
    }


    public class SendToDataLayerThread extends Thread{
        private String path;
        //private String message;
        private DataMap dataMap;

        public SendToDataLayerThread(String path,DataMap dataMap/*String msj*/){
            this.path = path;
            //this.message = msj;
            this.dataMap = dataMap;
        }

        @Override
        public void run() {

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send DataMap");
                }
            }

            /*NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }*/
        }
    }

}
