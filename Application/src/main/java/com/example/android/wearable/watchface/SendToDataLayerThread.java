package com.example.android.wearable.watchface;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by yamil on 23/03/15.
 */
public class SendToDataLayerThread extends Thread{
    private String path;
    private DataMap dataMap;
    private GoogleApiClient googleApiClient;

    public SendToDataLayerThread(String path,DataMap dataMap, GoogleApiClient googleApiClient){
        this.path = path;
        this.dataMap = dataMap;
        this.googleApiClient = googleApiClient;
    }

    @Override
    public void run() {

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        if(nodes != null) {
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send DataMap");
                }
                //googleApiClient.disconnect();
            }
        }
    }
}
