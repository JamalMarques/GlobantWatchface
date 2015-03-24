package com.example.android.wearable.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.android.wearable.watchface.weather.OpenWeatherListener;
import com.example.android.wearable.watchface.weather.OpenWeatherRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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

/**
 * Created by yamil.marques on 3/20/15.
 */
public class RefreshReceiver extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

    private SpiceManager spiceManager;
    private String lastRequestCacheKey;
    private GoogleApiClient googleApiClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        spiceManager = new SpiceManager(SpiceService.class);
        spiceManager.start(context);

        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        StockQuoteRequest request = new StockQuoteRequest("NYSE:GLOB");
        lastRequestCacheKey = request.createCacheKey();
        spiceManager.execute(request, lastRequestCacheKey, DurationInMillis.ONE_MINUTE, new StockQuoteListener());

        OpenWeatherRequest weatherRequest = new OpenWeatherRequest("-37.982593","-57.554475","metric");
        lastRequestCacheKey = weatherRequest.createCacheKey();
        spiceManager.execute(weatherRequest,lastRequestCacheKey,DurationInMillis.ONE_HOUR,new OpenWeatherListener(googleApiClient));

        Toast.makeText(context,"IM SHOWING!!",Toast.LENGTH_SHORT).show();
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

    private class StockQuoteListener implements PendingRequestListener<StockQuote>
    {

        @Override
        public void onRequestNotFound() {

        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.d(spiceException.getMessage(), spiceException.getLocalizedMessage());
        }

        @Override
        public void onRequestSuccess(StockQuote stockQuote) {
            if (stockQuote != null)
            {
                String stockValue = stockQuote.getL();
                String percentageChange = stockQuote.getCp();
                boolean isActionUp = ( Float.valueOf(percentageChange) > 0)? true : false;

                DataMap dataMap = new DataMap();
                dataMap.putString(Constants.MAP_ACTION_NUMBER, stockValue);
                dataMap.putBoolean(Constants.MAP_IS_ACTION_UP, isActionUp);
                dataMap.putString(Constants.MAP_PERCENTAJE_CHANGE, percentageChange);
                new SendToDataLayerThread(Constants.WEARABLE_DATA_PATH_2, dataMap,googleApiClient).start();
            }

        }
    }

    /*public class SendToDataLayerThread extends Thread{
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
            googleApiClient.disconnect();
            spiceManager.shouldStop();
        }
    }*/


}
