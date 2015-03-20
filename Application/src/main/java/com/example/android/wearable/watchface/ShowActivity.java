package com.example.android.wearable.watchface;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class ShowActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    private EditText etActions, etTemperature;
    private Button senderButton;
    private RadioButton rBlack,rWhite;
    private Spinner styleSpinner, refreshSpinner;
    private Switch isUpSwitch;

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

        etActions = (EditText)findViewById(R.id.editText);
        etTemperature = (EditText)findViewById(R.id.editText2);
        rBlack = (RadioButton)findViewById(R.id.rBlack);
        rWhite = (RadioButton)findViewById(R.id.rWhite);
        rBlack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    rWhite.setChecked(false);
            }
        });
        rWhite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    rBlack.setChecked(false);
            }
        });
        senderButton = (Button)findViewById(R.id.senderButton);
        senderButton.setOnClickListener(this);

        styleSpinner = (Spinner)findViewById(R.id.spinner);
        styleSpinner.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.type_widget_array, android.R.layout.simple_spinner_item));
        refreshSpinner = (Spinner)findViewById(R.id.spinnerRefresh);
        refreshSpinner.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.refresh_array, android.R.layout.simple_spinner_item));
        isUpSwitch = (Switch)findViewById(R.id.switchIsUp);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.globant, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
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
            if( (etActions.getText().length() > 0) && (etTemperature.getText().length() > 0) ) {

                String actionMsj = etActions.getText().toString();
                String temperatureMsj = etTemperature.getText().toString();
                boolean isActionUp = ( isUpSwitch.isChecked())? true : false;
                int widgetMode = styleSpinner.getSelectedItemPosition();
                String locale = getApplicationContext().getResources().getConfiguration().locale.getISO3Country();
                int colorMode = (rBlack.isChecked())? Constants.BACKGROUND_BLACK : Constants.BACKGROUND_WHITE;

                DataMap dataMap = new DataMap();
                dataMap.putString(Constants.MAP_ACTION_NUMBER, actionMsj);
                dataMap.putString(Constants.MAP_TEMPERATURE_NUMBER, temperatureMsj);
                dataMap.putInt(Constants.MAP_WIDGET_MODE,widgetMode);
                dataMap.putBoolean(Constants.MAP_IS_ACTION_UP,isActionUp);
                dataMap.putString(Constants.MAP_LOCATION_SHORT,locale);
                dataMap.putInt(Constants.MAP_COLOR_MODE,colorMode);
                new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();

            }else{
                Toast.makeText(this,getResources().getString(R.string.complete_field),Toast.LENGTH_SHORT).show();
            }
        }
    }


    public class SendToDataLayerThread extends Thread{
        private String path;
        private DataMap dataMap;

        public SendToDataLayerThread(String path,DataMap dataMap){
            this.path = path;
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
        }
    }

}
