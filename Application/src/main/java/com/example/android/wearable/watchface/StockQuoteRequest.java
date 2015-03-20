package com.example.android.wearable.watchface;


import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.octo.android.robospice.request.springandroid.SpringAndroidSpiceRequest;


import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by diego.mazzitelli on 16/03/2015.
 */
public class StockQuoteRequest extends SpringAndroidSpiceRequest<StockQuote> {

    String symbol;


    public StockQuoteRequest(String symbol) {
        super(StockQuote.class);
        this.symbol = symbol;
    }

    @Override
    public StockQuote loadDataFromNetwork() throws Exception {
        Uri.Builder uriBuilder = Uri.parse("http://www.google.com/finance/info?q=NYSE:GLOB").buildUpon();


        String url = uriBuilder.build().toString();
        RestTemplate restTemplate = getRestTemplate();
        String json = restTemplate.getForObject(url, String.class);
        StockQuote map = new StockQuote();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        try {
            //convert JSON string to Map
            map = mapper.readValue(json, StockQuote.class);
        } catch (Exception e) {
            Log.d("Exception converting {} to map", json, e);
        }

        return map;

    }

    public String createCacheKey() {
        return "symbol." + symbol;
    }
}
