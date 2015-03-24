package com.example.android.wearable.watchface;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by yamil.marques on 3/16/15.
 */
public class ListenerDataService extends WearableListenerService{// implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private static final String WEARABLE_DATA_PATH_1 = "/wearable_data";
    private static final String WEARABLE_DATA_PATH_2 = "/wearable_data/receiver";
    private static final String WEARABLE_DATA_PATH_3 = "/wearable_data/weather";

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
                    boolean isActionUp = dataMap.getBoolean(Constants.MAP_IS_ACTION_UP);
                    int widgetMode = dataMap.getInt(Constants.MAP_WIDGET_MODE);
                    int colorMode = dataMap.getInt(Constants.MAP_COLOR_MODE);
                    String percentajeChange = dataMap.getString(Constants.MAP_PERCENTAJE_CHANGE);

                    DigitalWatchFaceService.globActions = actionNumber;
                    DigitalWatchFaceService.isActionUp = isActionUp;
                    DigitalWatchFaceService.widgetMode = widgetMode;
                    DigitalWatchFaceService.colorMode = colorMode;
                    DigitalWatchFaceService.percentajeActionChange = Double.valueOf(percentajeChange);

                }else{
                    if(path.equals(WEARABLE_DATA_PATH_2)){  //Actions
                        dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        String actionNumber = dataMap.getString(Constants.MAP_ACTION_NUMBER);
                        boolean isActionUp = dataMap.getBoolean(Constants.MAP_IS_ACTION_UP);
                        String percentajeChange = dataMap.getString(Constants.MAP_PERCENTAJE_CHANGE);

                        DigitalWatchFaceService.globActions = actionNumber;
                        DigitalWatchFaceService.percentajeActionChange = Double.valueOf(percentajeChange);
                        DigitalWatchFaceService.isActionUp = isActionUp;
                    }else{
                        if(path.equals(WEARABLE_DATA_PATH_3)){  //Weather
                            dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                            Double temperature = dataMap.getDouble(Constants.MAP_TEMPERATURE);
                            String city = dataMap.getString(Constants.MAP_CITY);

                            String[] stringTemp = String.valueOf(temperature).split("\\.");
                            DigitalWatchFaceService.temperature = stringTemp[0];
                            DigitalWatchFaceService.shortLocation = city.substring(0,2);
                        }
                    }
                }
            }
        }
    }

}
