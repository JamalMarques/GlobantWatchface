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

    private static final String WEARABLE_DATA_PATH_1 = "/wearable_data";
    private static final String WEARABLE_DATA_PATH_2 = "/wearable_data/receiver";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEARABLE_DATA_PATH_1)) {
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

                }else{
                    if(path.equals(WEARABLE_DATA_PATH_2)){
                        dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        String actionNumber = dataMap.getString(Constants.MAP_ACTION_NUMBER);
                        boolean isActionUp = dataMap.getBoolean(Constants.MAP_IS_ACTION_UP);

                        DigitalWatchFaceService.globActions = actionNumber;
                        DigitalWatchFaceService.isActionUp = isActionUp;
                    }
                }
            }
        }
    }

}
