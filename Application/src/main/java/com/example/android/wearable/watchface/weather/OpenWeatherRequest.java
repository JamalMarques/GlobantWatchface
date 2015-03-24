package com.example.android.wearable.watchface.weather;

import android.net.Uri;

import com.octo.android.robospice.request.springandroid.SpringAndroidSpiceRequest;

import org.springframework.web.client.RestTemplate;

/**
 * Created by diego.mazzitelli on 23/03/2015.
 */
public class OpenWeatherRequest extends SpringAndroidSpiceRequest<OpenWeather> {

    private String latitud;
    private String longitud;
    private String unidad;


    public OpenWeatherRequest(String latitud, String longitud, String unidad) {
        super(OpenWeather.class);
        this.latitud = latitud;
        this.longitud = longitud;
        this.unidad = unidad;
    }

    @Override
    public OpenWeather loadDataFromNetwork() throws Exception {
        Uri.Builder uriBuilder = Uri.parse("http://api.openweathermap.org/data/2.5/weather").buildUpon();

        uriBuilder.appendQueryParameter("lat",this.latitud);
        uriBuilder.appendQueryParameter("lon",this.longitud);
        uriBuilder.appendQueryParameter("units",unidad);
        //uriBuilder.appendQueryParameter("lang","es");   Parametro para idioma en caso que se necesite

        String url = uriBuilder.build().toString();
        RestTemplate restTemplate = getRestTemplate();
        OpenWeather map = restTemplate.getForObject(url, OpenWeather.class);

        return map;

    }

    public String createCacheKey() {
        return "symbol." + latitud.substring(1,4);
    }
}
