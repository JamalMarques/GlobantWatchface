/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DigitalWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "DigitalWatchFaceService";

    private static  Typeface BOLD_TYPEFACE;
    private static  Typeface NORMAL_TYPEFACE;

    private int i = 0;


    /**
     * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
     * a second to blink the colons.
     */
    private static final long NORMAL_UPDATE_RATE_MS = 50; //500

    /**
     * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
     */
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        static final String COLON_STRING = ":";

        /** Alpha value for drawing time when in mute mode. */
        static final int MUTE_ALPHA = 100;

        /** Alpha value for drawing time when not in mute mode. */
        static final int NORMAL_ALPHA = 255;

        static final int MSG_UPDATE_TIME = 0;

        /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        /** Handler to update the time periodically in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(DigitalWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        //Paint mAmPmPaint;
        Paint mColonPaint;

        float mColonWidth;

        boolean mMute;

        Time mTime;

        boolean mShouldDrawColons;

        float mXOffset;
        float mYOffset;

        String mAmString;
        String mPmString;

        int mInteractiveBackgroundColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND;
        int mInteractiveHourDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
        int mInteractiveMinuteDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;
        int mInteractiveSecondDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            //BOLD_TYPEFACE = Typeface.createFromAsset(getAssets(), "typography/Roboto-Thin.ttf");
            NORMAL_TYPEFACE = Typeface.createFromAsset(getAssets(), "typography/Roboto-Regular.ttf");
            BOLD_TYPEFACE = NORMAL_TYPEFACE;

            Resources resources = DigitalWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            //mAmString = ""/*resources.getString(R.string.digital_am)*/;
            //mPmString = ""/*resources.getString(R.string.digital_pm)*/;

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mHourPaint = createTextPaint(mInteractiveHourDigitsColor);
            mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
            mSecondPaint = createTextPaint(mInteractiveSecondDigitsColor);

            //mAmPmPaint = createTextPaint(resources.getColor(R.color.digital_am_pm));
            mColonPaint = createTextPaint(resources.getColor(R.color.digital_colons));

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DigitalWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }


        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = DigitalWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension( isRound? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset );

            float textSize = resources.getDimension( isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size );
            //float amPmSize = resources.getDimension( isRound ? R.dimen.digital_am_pm_size_round : R.dimen.digital_am_pm_size );

            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            //mAmPmPaint.setTextSize(amPmSize);
            mColonPaint.setTextSize(textSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                        + ", low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
            adjustPaintColorToCurrentMode(mHourPaint, mInteractiveHourDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            adjustPaintColorToCurrentMode(mMinutePaint, mInteractiveMinuteDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);
            // Actually, the seconds are not rendered in the ambient mode, so we could pass just any
            // value as ambientColor here.
            adjustPaintColorToCurrentMode(mSecondPaint, mInteractiveSecondDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                //mAmPmPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            }
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            // We only need to update once a minute in mute mode.
            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                mHourPaint.setAlpha(alpha);
                mMinutePaint.setAlpha(alpha);
                mColonPaint.setAlpha(alpha);
                //mAmPmPaint.setAlpha(alpha);
                invalidate();
            }
        }

        public void setInteractiveUpdateRateMs(long updateRateMs) {
            if (updateRateMs == mInteractiveUpdateRateMs) {
                return;
            }
            mInteractiveUpdateRateMs = updateRateMs;

            // Stop and restart the timer so the new update rate takes effect immediately.
            if (shouldTimerBeRunning()) {
                updateTimer();
            }
        }

        private void updatePaintIfInteractive(Paint paint, int interactiveColor) {
            if (!isInAmbientMode() && paint != null) {
                paint.setColor(interactiveColor);
                paint.setAntiAlias(true);
            }
        }

        private void setInteractiveBackgroundColor(int color) {
            mInteractiveBackgroundColor = color;
            updatePaintIfInteractive(mBackgroundPaint, color);
        }

        private void setInteractiveHourDigitsColor(int color) {
            mInteractiveHourDigitsColor = color;
            updatePaintIfInteractive(mHourPaint, color);
        }

        private void setInteractiveMinuteDigitsColor(int color) {
            mInteractiveMinuteDigitsColor = color;
            updatePaintIfInteractive(mMinutePaint, color);
        }

        private void setInteractiveSecondDigitsColor(int color) {
            mInteractiveSecondDigitsColor = color;
            updatePaintIfInteractive(mSecondPaint, color);
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        private int convertTo12Hour(int hour) {
            int result = hour % 12;
            return (result == 0) ? 12 : result;
        }

        private String getAmPmString(int hour) {
            return (hour < 12) ? mAmString : mPmString;
        }



        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            mYOffset = bounds.height() / 2;
            mXOffset = bounds.width() / 2;

            String hourString = String.valueOf(mTime.hour/*convertTo12Hour(mTime.hour)*/);
            String minuteString = formatTwoDigitNumber(mTime.minute);

            float timeTotalWidth = mHourPaint.measureText(hourString) + mColonWidth + mMinutePaint.measureText(minuteString);

            //Declares
            float mYTime = mYOffset + 20;
            float mXTimeStart = mXOffset - (timeTotalWidth/2) - 1;
            float mYRows = mYOffset - 28;
            float mXLeftRow = 20;
            float mXRightRow = (mXOffset*2) - 80 ;
            float mYGLogo = mYOffset - 165;
            float mXGlogo = mXOffset - 110 ;
            float mYWLogo = mYOffset + 40;
            float mXWLogo = mXOffset - 75;

            //Starting point
            float x = mXTimeStart;
            //Draw the hours
            canvas.drawText(hourString, x, mYTime, mHourPaint);
            x += mHourPaint.measureText(hourString);
            //Draw colons
            if (isInAmbientMode() || mMute || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, mYTime, mColonPaint);
            }
            x += mColonWidth;
            // Draw the minutes.
            canvas.drawText(minuteString, x, mYTime, mMinutePaint);
            x += mMinutePaint.measureText(minuteString);


            // In ambient and mute modes, draw AM/PM. Otherwise, draw a second blinking
            // colon followed by the seconds.
            /*
            if (isInAmbientMode() || mMute) {
                x += mColonWidth;
                //canvas.drawText(getAmPmString(mTime.hour), x, mYOffset, mAmPmPaint); PAINT AM AND PM
            } else {
                if (mShouldDrawColons) {
                    canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
                }
                x += mColonWidth;
                canvas.drawText(formatTwoDigitNumber(mTime.second), x, mYOffset,
                        mSecondPaint);
            }*/


            Paint paint = new Paint();
            paint.setAntiAlias(true);
            //Drawing Left arrow
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrowreverse), mXLeftRow , mYRows ,paint);
            //Drawing right arrow
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow), mXRightRow , mYRows , paint);
            //Drawing "Globant" logo
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_logoglobant), mXGlogo , mYGLogo ,paint);
            //Drawing "We are ready" logo
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_weareready), mXWLogo , mYWLogo ,paint);

            //Drawing battery percentage circle
            Paint circlePaint = new Paint();
            circlePaint.setColor(getResources().getColor(R.color.globant_green));
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2);
            circlePaint.setTypeface(BOLD_TYPEFACE);
            canvas.drawCircle( mXOffset , (mYOffset*2) - 40, 20, circlePaint);

            //Drawing battery percentage
            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(15);
            textPaint.setTypeface(BOLD_TYPEFACE);
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            canvas.drawText( level+"%" , mXOffset - 13, (mYOffset*2) - 40 + 5, textPaint);

            //Widget mode 1 -------------------
            //Drawing widget 1
            Paint w1Paint = new Paint();
            w1Paint.setAntiAlias(true);
            w1Paint.setColor(Color.WHITE/*getResources().getColor(R.color.globant_green)*/);
            w1Paint.setStyle(Paint.Style.STROKE);
            w1Paint.setStrokeWidth(2);
            w1Paint.setTypeface(BOLD_TYPEFACE);
            w1Paint.setShadowLayer(2, 0, 0, Color.WHITE);
            canvas.drawCircle(mXOffset + 70, (mYOffset * 2) - 60, 25, w1Paint);

            //Drawing widget 2
            Paint w2Paint = new Paint();
            w2Paint.setAntiAlias(true);
            w2Paint.setColor(Color.WHITE/*getResources().getColor(R.color.globant_green)*/);
            w2Paint.setStyle(Paint.Style.STROKE);
            w2Paint.setStrokeWidth(2);
            w2Paint.setTypeface(BOLD_TYPEFACE);
            w2Paint.setShadowLayer(2, 0, 0, Color.WHITE);
            canvas.drawCircle( mXOffset - 70 , (mYOffset*2) - 60, 25, w2Paint);
            //---------------------------------

            //Widget Mode 2 -------------------
            /*//Drawing widget 1
            Paint wPaint = new Paint();
            wPaint.setAntiAlias(true);
            wPaint.setColor(getResources().getColor(R.color.globant_green));
            wPaint.setStrokeWidth(2);
            wPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(new RectF(0, mYOffset + 75, mXOffset - 35, (mYOffset*2) - 30 ), 30, 30, wPaint);
           //left top right bottom

            //Drawing widget 2
            canvas.drawRoundRect(new RectF( mXOffset + 35, mYOffset + 75, mXOffset*2 , (mYOffset*2) - 30), 30, 30, wPaint);*/
            //----------------------------------


            //Testing -----------------
            if(i > 360)
                i = 0;
            else
                i++;

            //SweepGradient gradient1 = new SweepGradient(200, 520,Color.WHITE, getResources().getColor(R.color.globant_green));

            Paint p = new Paint();
            p.setColor(getResources().getColor(R.color.globant_green));
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            //p.setAlpha(130);
            //p.setShader(gradient1);
            canvas.drawArc(0 ,0, (mXOffset*2), (mYOffset*2), 90 , i , false, p);
            //--------------------------

            Paint redMoto360Line = new Paint();
            redMoto360Line.setColor(Color.RED);
            redMoto360Line.setStrokeWidth(1);
            canvas.drawLine(0, (mYOffset*2)-30,mXOffset*2,(mYOffset*2)-30,redMoto360Line);


        }


        private void drawRows(int positions){

        }

        private void drawWidgets(){

        }


        private void drawTime(){

        }

        private void drawLogos(){

        }



        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }


        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void updateConfigDataItemAndUiOnStartup() {
            DigitalWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new DigitalWatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
        }

        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_HOURS_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_MINUTES_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_SECONDS_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);
        }

        private void addIntKeyIfMissing(DataMap config, String key, int color) {
            if (!config.containsKey(key)) {
                config.putInt(key, color);
            }
        }

        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    if (!dataItem.getUri().getPath().equals(
                            DigitalWatchFaceUtil.PATH_WITH_FEATURE)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                    updateUiForConfigDataMap(config);
                }
            } finally {
                dataEvents.close();
            }
        }

        private void updateUiForConfigDataMap(final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) {
                    continue;
                }
                int color = config.getInt(configKey);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found watch face config key: " + configKey + " -> "
                            + Integer.toHexString(color));
                }
                if (updateUiForKey(configKey, color)) {
                    uiUpdated = true;
                }
            }
            if (uiUpdated) {
                invalidate();
            }
        }


        private boolean updateUiForKey(String configKey, int color) {
            if (configKey.equals(DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR)) {
                setInteractiveBackgroundColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_HOURS_COLOR)) {
                setInteractiveHourDigitsColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_MINUTES_COLOR)) {
                setInteractiveMinuteDigitsColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_SECONDS_COLOR)) {
                setInteractiveSecondDigitsColor(color);
            } else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
                return false;
            }
            return true;
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup();
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }
    }
}
