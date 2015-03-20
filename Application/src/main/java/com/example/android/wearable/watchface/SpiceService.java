package com.example.android.wearable.watchface;

import android.app.Application;

import com.octo.android.robospice.SpringAndroidSpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.binary.InFileInputStreamObjectPersister;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.springandroid.json.jackson.JacksonObjectPersisterFactory;
import com.octo.android.robospice.persistence.string.InFileStringObjectPersister;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by diego.mazzitelli on 16/03/2015.
 */
public class SpiceService extends SpringAndroidSpiceService {

    private static final int WEBSERVICE_TIMEOUT = 8000;

    public SpiceService() {

    }

    public CacheManager createCacheManager(Application application) throws CacheCreationException {
        CacheManager cacheManager = new CacheManager();

        InFileStringObjectPersister inFileStringObjectPersister = new InFileStringObjectPersister(application);
        InFileInputStreamObjectPersister inFileInputStreamObjectPersister = new InFileInputStreamObjectPersister(application);
        JacksonObjectPersisterFactory inJSonFileObjectPersisterFactory = new JacksonObjectPersisterFactory(application);

        inFileStringObjectPersister.setAsyncSaveEnabled(true);
        inFileInputStreamObjectPersister.setAsyncSaveEnabled(true);
        inJSonFileObjectPersisterFactory.setAsyncSaveEnabled(true);

        cacheManager.addPersister(inFileInputStreamObjectPersister);
        cacheManager.addPersister(inFileStringObjectPersister);
        cacheManager.addPersister(inJSonFileObjectPersisterFactory);
        return cacheManager;
    }


    public RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(WEBSERVICE_TIMEOUT);
        httpRequestFactory.setConnectTimeout(WEBSERVICE_TIMEOUT);
        restTemplate.setRequestFactory(httpRequestFactory);

        final List<HttpMessageConverter<?>> listHttpMessageConverters = new ArrayList<HttpMessageConverter<?>>(3);

        listHttpMessageConverters.add(new MappingJacksonHttpMessageConverter());
        listHttpMessageConverters.add(new FormHttpMessageConverter());
        listHttpMessageConverters.add(new StringHttpMessageConverter());
        restTemplate.setMessageConverters(listHttpMessageConverters);

        return restTemplate;
    }

}
