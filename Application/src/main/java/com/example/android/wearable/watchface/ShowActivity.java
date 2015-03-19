package com.example.android.wearable.watchface;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;


public class ShowActivity extends Activity {

    private SpiceManager spiceManager = new SpiceManager(SpiceService.class);
    private String lastRequestCacheKey;

    @Override
    public void onStart()
    {
        super.onStart();
        spiceManager.start(this);
    }

    @Override
    public void onStop()
    {
        if (spiceManager.isStarted())
            spiceManager.shouldStop();
        super.onStop();
    }

    protected SpiceManager getSpiceManager()
    {
        return spiceManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        Button btn = (Button) findViewById(R.id.btnSpice);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StockQuoteRequest request = new StockQuoteRequest("NYSE:GLOB");
                lastRequestCacheKey = request.createCacheKey();
                spiceManager.execute(request, lastRequestCacheKey, DurationInMillis.ONE_MINUTE, new StockQuoteListener());
            }
        });
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
                String StockValue = stockQuote.getL();
                String Percentage = stockQuote.getCp();

            }

        }
    }
}
