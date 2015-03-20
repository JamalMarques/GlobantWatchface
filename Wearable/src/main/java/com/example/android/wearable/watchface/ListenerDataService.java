package com.example.android.wearable.watchface;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by yamil.marques on 3/16/15.
 */
public class ListenerDataService extends WearableListenerService{// implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private static final String WEARABLE_DATA_PATH = "/wearable_data";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEARABLE_DATA_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    String actionNumber = dataMap.getString(Constants.MAP_ACTION_NUMBER);
                    String temperatureNumber = dataMap.getString(Constants.MAP_TEMPERATURE_NUMBER);
                    boolean isActionUp = dataMap.getBoolean(Constants.MAP_IS_ACTION_UP);
                    int widgetMode = dataMap.getInt(Constants.MAP_WIDGET_MODE);
                    String locationShort = dataMap.getString(Constants.MAP_LOCATION_SHORT);
                    int colorMode = dataMap.getInt(Constants.MAP_COLOR_MODE);

                    DigitalWatchFaceService.globActions = actionNumber;
                    DigitalWatchFaceService.degressTemperature = temperatureNumber;
                    DigitalWatchFaceService.isActionUp = isActionUp;
                    DigitalWatchFaceService.widgetMode = widgetMode;
                    DigitalWatchFaceService.shortLocation = locationShort;
                    DigitalWatchFaceService.colorMode = colorMode;

                }
            }
        }
    }

    /*@Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(WEARABLE_DATA_PATH)) {
            final String message = new String(messageEvent.getData());
            DigitalWatchFaceService.globActions = message;
        }
        else {
            super.onMessageReceived(messageEvent);
        }

    }


    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {

    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {

    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {

    }*/


//-----------------------------------------------
    /*public static interface onDataChanged{
        public void onDataReceived();
    }*/

    /*@Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEARABLE_DATA_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.v("myTag", "DataMap received on watch: " + dataMap);

                    // Broadcast message to wearable activity for display
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra(Constants.MAP_NUMBER, dataMap.getString(Constants.MAP_NUMBER));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                }
            }
        }
    }*/


}
