package com.example.android.wearable.watchface.weather;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.android.wearable.watchface.Constants;
import com.example.android.wearable.watchface.SendToDataLayerThread;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;

/**
 * Created by diego.mazzitelli on 23/03/2015.
 */
public class OpenWeatherListener implements PendingRequestListener<OpenWeather>,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient googleApiClient;

    public OpenWeatherListener(GoogleApiClient googleApiClient){
        this.googleApiClient = googleApiClient;
    }

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
            /*GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            googleApiClient.connect();*/

            Double temperatura = openWeather.getMain().getTemp();
            String ciudad = openWeather.getName();

            DataMap dataMap = new DataMap();
            dataMap.putDouble(Constants.MAP_TEMPERATURE, temperatura);
            dataMap.putString(Constants.MAP_CITY,ciudad);
            new SendToDataLayerThread(Constants.WEARABLE_DATA_PATH_3, dataMap,googleApiClient).start();
        }

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
