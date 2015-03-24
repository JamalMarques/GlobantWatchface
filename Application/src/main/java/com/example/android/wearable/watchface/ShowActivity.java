package com.example.android.wearable.watchface;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
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
import android.widget.Toast;

import com.example.android.wearable.watchface.weather.OpenWeather;
import com.example.android.wearable.watchface.weather.OpenWeatherRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;

import java.util.Calendar;

/**
 * Created by yamil.marques on 3/17/15.
 */

public class ShowActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private static final String SHARED_PREFERENCES_NAME = "MyPreferences";

    private Button senderButton;
    private RadioButton rBlack,rWhite;
    private Spinner styleSpinner, refreshSpinner;

    private GoogleApiClient googleApiClient, positionGoogleApiClient;
    private AlarmManager alarmManager;
    private SharedPreferences sharedPreferences;

    private SpiceManager spiceManager = new SpiceManager(SpiceService.class);
    private String lastRequestCacheKey;

    private Location lastLocation;
    private String lastLong;
    private String lastLat;

    protected SpiceManager getSpiceManager()
    {
        return spiceManager;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        //Building a Google Api Client
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API)
                                                           .addConnectionCallbacks(this)
                                                           .addOnConnectionFailedListener(this)
                                                           .build();

        positionGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,0);

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
        refreshSpinner.setSelection(sharedPreferences.getInt(Constants.SHARED_PREFERENCES_TIME_TO_REFRESH,1)-1);

        //Refresh or not AlarmManager
        if( (refreshSpinner.getSelectedItemPosition()+1) != sharedPreferences.getInt(Constants.SHARED_PREFERENCES_TIME_TO_REFRESH,0)){
            //Refresh the AlarmManager
            GenerateAlarm(refreshSpinner.getSelectedItemPosition());
        }

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
        positionGoogleApiClient.connect();
        googleApiClient.connect();
        spiceManager.start(this);
    }

    @Override
    protected void onStop() {
        if( googleApiClient != null && googleApiClient.isConnected()){
            googleApiClient.disconnect();
        }
        if (spiceManager.isStarted()) {
            spiceManager.shouldStop();
        }
        if( positionGoogleApiClient != null && positionGoogleApiClient.isConnected()){
            positionGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}


    @Override
    public void onConnected(Bundle bundle) {
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                positionGoogleApiClient);
        if (lastLocation != null) {
            lastLat = String.valueOf(lastLocation.getLatitude());
            lastLong = String.valueOf(lastLocation.getLongitude());
        }
    }

    @Override
    public void onClick(View v) {
        if( v == senderButton){
                //Refresh the alarm
                GenerateAlarm(refreshSpinner.getSelectedItemPosition());

                StockQuoteRequest request = new StockQuoteRequest("NYSE:GLOB");
                lastRequestCacheKey = request.createCacheKey();
                spiceManager.execute(request, lastRequestCacheKey, DurationInMillis.ONE_MINUTE, new StockQuoteListener());

                OpenWeatherRequest weatherRequest = new OpenWeatherRequest(lastLat,lastLong,"metric");
                lastRequestCacheKey = weatherRequest.createCacheKey();
                spiceManager.execute(weatherRequest,lastRequestCacheKey,DurationInMillis.ONE_HOUR,new OpenWeatherListener());
        }
    }


    private class StockQuoteListener implements PendingRequestListener<StockQuote>
    {
        @Override
        public void onRequestNotFound() {

        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.d(spiceException.getMessage(),spiceException.getLocalizedMessage());
        }

        @Override
        public void onRequestSuccess(StockQuote stockQuote) {
            if (stockQuote != null)
            {
                String stockValue = stockQuote.getL();
                String percentageChange = stockQuote.getCp();
                int widgetMode = styleSpinner.getSelectedItemPosition();
                boolean isActionUp = ( Float.valueOf(percentageChange) > 0)? true : false;
                int colorMode = (rBlack.isChecked())? Constants.BACKGROUND_BLACK : Constants.BACKGROUND_WHITE;

                DataMap dataMap = new DataMap();
                dataMap.putString(Constants.MAP_ACTION_NUMBER, stockValue);
                dataMap.putInt(Constants.MAP_WIDGET_MODE, widgetMode);
                dataMap.putBoolean(Constants.MAP_IS_ACTION_UP, isActionUp);
                dataMap.putInt(Constants.MAP_COLOR_MODE, colorMode);
                dataMap.putString(Constants.MAP_PERCENTAJE_CHANGE, percentageChange);
                new SendToDataLayerThread(Constants.WEARABLE_DATA_PATH_1, dataMap,googleApiClient).start();
            }

        }
    }

    public class OpenWeatherListener implements PendingRequestListener<OpenWeather>{

        @Override
        public void onRequestNotFound() {

        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.d(spiceException.getMessage(), spiceException.getLocalizedMessage());
        }

        @Override
        public void onRequestSuccess(OpenWeather openWeather) {
            if (openWeather != null)
            {

                Double temperatura = openWeather.getMain().getTemp();
                String ciudad = openWeather.getName();

                DataMap dataMap = new DataMap();
                dataMap.putDouble(Constants.MAP_TEMPERATURE, temperatura);
                dataMap.putString(Constants.MAP_CITY,ciudad);
                new SendToDataLayerThread(Constants.WEARABLE_DATA_PATH_3, dataMap,googleApiClient).start();
            }

        }

    }



    private void GenerateAlarm(int alarmPositionTime){

        Calendar cal = Calendar.getInstance();
        long frequencyTime = 0;
        switch (alarmPositionTime){
            case Constants.HOURS_1:
                    frequencyTime = Constants.HALF_HOUR * 2;
                break;
            case Constants.HOURS_2:
                    frequencyTime = Constants.HALF_HOUR * 4;
                break;
            case Constants.HOURS_3:
                    frequencyTime = Constants.HALF_HOUR * 6;
                break;
            default:
                    frequencyTime = Constants.HALF_HOUR;
                break;
        }
        Intent intent = new Intent(this,RefreshReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,Constants.ID_PENDING_INTENT, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),frequencyTime,pendingIntent);

        //Saving the changes
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.SHARED_PREFERENCES_TIME_TO_REFRESH,alarmPositionTime);
    }

    private void ManualRefresh(){
        //TODO
    }


}
