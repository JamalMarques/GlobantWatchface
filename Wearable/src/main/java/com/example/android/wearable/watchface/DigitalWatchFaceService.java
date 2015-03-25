package com.example.android.wearable.watchface;
/**
 * Created by yamil.marques on 3/3/15.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DigitalWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "DigitalWatchFaceService";

    public static String globActions, temperature, shortLocation;
    public static boolean isActionUp = true;
    public static int widgetMode = 0, colorMode = 0;
    public static double percentajeActionChange = 0.00;

    private static  Typeface BOLD_TYPEFACE;
    private static  Typeface NORMAL_TYPEFACE;

    private float degressOfSeconds = 0;
    private float extraHeight = 0;
    private Bitmap globantLogo, wearereadyLogo, rightRowAsset, leftRowAsset;

    private Paint mBackgroundPaint;
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mSecondPaint;
    private Paint mColonPaint;
    private Paint dayTextPaint;

    private int colorTextGeneral;
    private int backgroundModeSaved;

    private float mColonWidth;


    private static final long NORMAL_UPDATE_RATE_MS = 500; //500

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

        boolean mMute;

        Time mTime;

        boolean mShouldDrawColons;

        float mXCenter;
        float mYCenter;

        int mInteractiveBackgroundColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND;
        int mInteractiveHourDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
        int mInteractiveMinuteDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;
        int mInteractiveSecondDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS;

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
            NORMAL_TYPEFACE = Typeface.createFromAsset(getAssets(), "typography/Roboto-Black.ttf");
            BOLD_TYPEFACE = NORMAL_TYPEFACE;

            Resources resources = DigitalWatchFaceService.this.getResources();
            //mYCenter = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mHourPaint = createTextPaintTime(mInteractiveHourDigitsColor);
            mMinutePaint = createTextPaintTime(mInteractiveMinuteDigitsColor);
            mSecondPaint = createTextPaintTime(mInteractiveSecondDigitsColor);
            mColonPaint = createTextPaintTime(resources.getColor(R.color.digital_colons));

            globantLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logoglobant);
            wearereadyLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_weareready);
            rightRowAsset = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow);
            leftRowAsset = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrowreverse);
            colorTextGeneral = getResources().getColor(R.color.black);

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

        private Paint createTextPaintTime(int defaultInteractiveColor){
            return createTextPaint(defaultInteractiveColor, Typeface.createFromAsset(getAssets(), "typography/Roboto-Regular.ttf"));
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
            mXCenter = resources.getDimension( isRound? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset );

            float textSize = resources.getDimension( isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size );
            //float amPmSize = resources.getDimension( isRound ? R.dimen.digital_am_pm_size_round : R.dimen.digital_am_pm_size );

            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            mColonPaint.setTextSize(textSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            //mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

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
            adjustPaintColorToCurrentMode(mSecondPaint, mInteractiveSecondDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
            }
            invalidate();
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
            //Change drawables
            int constantColor = 0;
            if( color == Color.parseColor(getBaseContext().getString(R.string.color_black))) {
                constantColor = Constants.BACKGROUND_BLACK;
            }else{
                if(color == Color.parseColor(getBaseContext().getString(R.string.color_white))){
                    constantColor = Constants.BACKGROUND_WHITE;
                }
            }
            changeDrawables(constantColor);
            updatePaintIfInteractive(mBackgroundPaint, color);
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            shouldChangeColorMode();

            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            extraHeight = extraHeight(bounds.height());

            mYCenter = (bounds.height() + extraHeight) / 2;
            mXCenter = bounds.width() / 2;

            String hourString = String.valueOf(mTime.hour);
            String minuteString = formatTwoDigitNumber(mTime.minute);

            float timeTotalWidth = mHourPaint.measureText(hourString) + mColonWidth + mMinutePaint.measureText(minuteString);

            //Declares
            float mYTime = mYCenter + 20;
            float mXTimeStart = mXCenter - (timeTotalWidth/2) - 1;
            float mYRows = mYCenter - 28;
            float mXLeftRow = 20;
            float mXRightRow = (mXCenter *2) -rightRowAsset.getWidth() - 20 ;
            float mYGLogo = mYCenter - 165;
            float mXGlogo = mXCenter - (globantLogo.getWidth()/2);
            float mYWLogo = mYCenter + 40;
            float mXWLogo = mXCenter - (wearereadyLogo.getWidth()/2);

            drawTime(canvas,mXCenter,mYTime,hourString,minuteString);
            drawArrowsAndLogos(canvas,mXLeftRow,mYRows,mXRightRow,mXGlogo,mYGLogo,mXWLogo,mYWLogo);
            drawWidgets(canvas, globActions, temperature, shortLocation, isActionUp);

            if(isInAmbientMode())
                inAmbientMode();
            else
                drawSeconds(canvas, mTime);


            drawMoto360Line(canvas,Constants.DRAW_MOTO_360_LINE);
        }

        private float extraHeight(float height){
            if( height == 290){ //Moto360 watch
                return 30;
            }else
                return 0;
        }

        private void inAmbientMode(){
            // In ambient and mute modes, draw AM/PM. Otherwise, draw a second blinking
            // colon followed by the seconds.
            /*
            if (isInAmbientMode() || mMute) {
                x += mColonWidth;
                //canvas.drawText(getAmPmString(mTime.hour), x, mYCenter, mAmPmPaint); PAINT AM AND PM
            } else {
                if (mShouldDrawColons) {
                    canvas.drawText(COLON_STRING, x, mYCenter, mColonPaint);
                }
                x += mColonWidth;
                canvas.drawText(formatTwoDigitNumber(mTime.second), x, mYCenter,
                        mSecondPaint);
            }*/
        }

        private void drawTime(Canvas canvas,float mXCenter,float mYTime,String hourString, String minuteString){

            float x1 = 0;
            x1 = (mHourPaint.measureText(hourString) + mColonWidth + mMinutePaint.measureText(minuteString)) / 2;

            //Starting point
            float x = mXCenter - x1;
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
        }

        private void shouldChangeColorMode(){
            if(backgroundModeSaved != DigitalWatchFaceService.colorMode){
                if( DigitalWatchFaceService.colorMode == Constants.BACKGROUND_BLACK ){
                    mBackgroundPaint.setColor(getResources().getColor(R.color.black));
                    mBackgroundPaint.setAntiAlias(true);
                    changeDrawables(Constants.BACKGROUND_BLACK);
                    backgroundModeSaved = Constants.BACKGROUND_BLACK;
                }else{
                    if( DigitalWatchFaceService.colorMode == Constants.BACKGROUND_WHITE ){
                        mBackgroundPaint.setColor(getResources().getColor(R.color.white));
                        mBackgroundPaint.setAntiAlias(true);
                        changeDrawables(Constants.BACKGROUND_WHITE);
                        backgroundModeSaved = Constants.BACKGROUND_WHITE;
                    }
                }
            }
        }

        private void drawArrowsAndLogos(Canvas canvas,float mXLeftRow,float mYRows,float mXRightRow,float mXGlogo, float mYGLogo
                                        ,float mXWLogo,float mYWLogo){
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            //Drawing Left arrow
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrowreverse), mXLeftRow , mYRows ,paint);
            //Drawing right arrow
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow), mXRightRow , mYRows , paint);
            //Drawing "Globant" logo
            canvas.drawBitmap(globantLogo, mXGlogo , mYGLogo ,paint);
            //Drawing "We are ready" logo
            canvas.drawBitmap(wearereadyLogo, mXWLogo , mYWLogo ,paint);
        }


        private void drawWidgets(Canvas canvas,String globActions,String temperature,String shortLocation,boolean isActionUp){

            if( widgetMode >= 0 && widgetMode <= 1 ){

                Bitmap arrowBit = null;
                if(isActionUp){
                    arrowBit = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_up_green);
                }else{
                    arrowBit = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_down_red);
                }

                switch (widgetMode){
                    case 0:
                        drawWidgetsMode1(canvas,globActions,temperature,shortLocation,isActionUp,arrowBit);
                        break;
                    case 1:
                        drawWidgetsMode2(canvas,globActions,temperature,shortLocation,isActionUp,arrowBit);
                        break;
                }
            }

        }

        private void drawWidgetsMode1(Canvas canvas, String globActions,String temperature,String shortLocation,boolean isActionUp, Bitmap arrowBit){

            //Drawing Day of month circle
            /*Paint circlePaint = new Paint();
            circlePaint.setColor(getResources().getColor(R.color.globant_green));
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2);
            circlePaint.setTypeface(BOLD_TYPEFACE);
            canvas.drawCircle(mXCenter, (mYCenter *2) - 40, 20, circlePaint);

            //Drawing Day of month text
            dayTextPaint = new Paint();
            dayTextPaint.setAntiAlias(true);
            dayTextPaint.setColor(colorTextGeneral);
            dayTextPaint.setTextSize(20);
            dayTextPaint.setTypeface(BOLD_TYPEFACE);
            int day = mTime.monthDay;
            String dayToShow = day+"";
            float dayTextWidth = dayTextPaint.measureText(dayToShow);
            canvas.drawText( dayToShow , ( mXCenter - (dayTextWidth/2)) - 1 , (mYCenter *2) - 34, dayTextPaint);*/

            //Drawing Percentaje rec
            Paint percPaint = new Paint();
            percPaint.setAntiAlias(true);
            percPaint.setColor(colorTextGeneral);
            percPaint.setStyle(Paint.Style.STROKE);
            percPaint.setStrokeWidth(2);
            percPaint.setTypeface(BOLD_TYPEFACE);
            percPaint.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawRoundRect(new RectF(mXCenter - 10, mYCenter + 105, mXCenter +70, (mYCenter*2) - 35 ), 10, 10, percPaint);
            //block transparency
            percPaint.setColor(mBackgroundPaint.getColor());
            percPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mXCenter + 70, (mYCenter * 2) - 60, 25, percPaint);
            //Drawing percentaje text
            Paint textPercPaint = new Paint();
            textPercPaint.setAntiAlias(true);
            textPercPaint.setTypeface(BOLD_TYPEFACE);
            textPercPaint.setTextSize(14);
            int xplusnumber = 14;
            if(isActionUp) {
                textPercPaint.setColor(getResources().getColor(R.color.green));
            }
            else {
                textPercPaint.setColor(getResources().getColor(R.color.red));
            }

            canvas.drawText(percentajeActionChange +"%", mXCenter, (mYCenter*2) - 40,textPercPaint);

            //Widget mode 1 -------------------
            //Drawing widget 1
            Paint w1Paint = new Paint();
            w1Paint.setAntiAlias(true);
            w1Paint.setColor(colorTextGeneral);
            w1Paint.setStyle(Paint.Style.STROKE);
            w1Paint.setStrokeWidth(2);
            w1Paint.setTypeface(BOLD_TYPEFACE);
            w1Paint.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawCircle(mXCenter + 70, (mYCenter * 2) - 60, 25, w1Paint);
            //Drawing text widget 1
            Paint textPaintW1 = new Paint();
            textPaintW1.setAntiAlias(true);
            textPaintW1.setColor(colorTextGeneral);
            textPaintW1.setTextSize(18);
            textPaintW1.setTypeface(BOLD_TYPEFACE);
            //test
            if(globActions == null)
                globActions = "14.07";

            float x = textPaintW1.measureText(globActions);
            String[] aux = globActions.split("\\.");

            canvas.drawText( aux[0].toString() ,(mXCenter + 70) - (x/2) + 2, ((mYCenter * 2) - 60) + 7, textPaintW1);
            textPaintW1.setTextSize(12);
            float startDecimal = textPaintW1.measureText(aux[0].toString());
            if( aux.length > 1 ) {
                canvas.drawText("  ." + aux[1].toString(), (mXCenter + 70) - (x/2) + startDecimal + 2, ((mYCenter * 2) - 60) + 7, textPaintW1);
            }

            //Drawing widget 2
            //Drawing location rec
            /*Paint locatArcPaint = new Paint();
            locatArcPaint.setAntiAlias(true);
            locatArcPaint.setColor(colorTextGeneral);
            locatArcPaint.setStyle(Paint.Style.STROKE);
            locatArcPaint.setStrokeWidth(2);
            locatArcPaint.setTypeface(BOLD_TYPEFACE);
            locatArcPaint.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawRoundRect(new RectF( mXCenter - 70, mYCenter + 105, mXCenter - 10, (mYCenter*2) - 35 ), 10, 10, locatArcPaint);
            //block transparency
            locatArcPaint.setColor(mBackgroundPaint.getColor());
            locatArcPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mXCenter - 70, (mYCenter * 2) - 60, 25, locatArcPaint);*/
            //Drawing location text
            /*Paint locationTextPaint = new Paint();
            locationTextPaint.setAntiAlias(true);
            locationTextPaint.setTypeface(BOLD_TYPEFACE);
            locationTextPaint.setColor(getResources().getColor(R.color.orange_1));
            locationTextPaint.setTextSize(12);
            canvas.drawText( shortLocation , mXCenter - 40 , (mYCenter*2) - 40, locationTextPaint);*/
            //Finally widget
            Paint w2Paint = new Paint();
            w2Paint.setAntiAlias(true);
            w2Paint.setColor(colorTextGeneral);
            w2Paint.setStyle(Paint.Style.STROKE);
            w2Paint.setStrokeWidth(2);
            w2Paint.setTypeface(BOLD_TYPEFACE);
            w2Paint.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawCircle(mXCenter - 70, (mYCenter * 2) - 60, 25, w2Paint);
            //Drawing text widget 2
            Paint textPaintW2 = new Paint();
            textPaintW2.setAntiAlias(true);
            textPaintW2.setColor(colorTextGeneral);
            textPaintW2.setTextSize(25);
            textPaintW2.setTypeface(BOLD_TYPEFACE);
            //test
            if(temperature == null)
                temperature = "21";
            temperature += "º";
            float temperatureWidth = textPaintW2.measureText(temperature);
            canvas.drawText(temperature , (mXCenter - 70) - (temperatureWidth/2) , ((mYCenter * 2) - 60) + 10, textPaintW2);


            //Drawing location
            /*Paint locationPaint = new Paint();
            locationPaint.setAntiAlias(true);
            locationPaint.setColor(colorTextGeneral);
            locationPaint.setTextSize(10);
            locationPaint.setTypeface(BOLD_TYPEFACE);
            if(shortLocation.length() > 0) {
                float locationWidth = locationPaint.measureText(shortLocation);
                canvas.drawText(shortLocation, (mXCenter - 70) - (locationWidth / 2), ((mYCenter * 2) - 70), locationPaint);
            }*/

            //Draw circle for arrow
            Paint circlePaintArrow = new Paint();
            circlePaintArrow.setColor(colorTextGeneral);
            circlePaintArrow.setAntiAlias(true);
            circlePaintArrow.setStyle(Paint.Style.STROKE);
            circlePaintArrow.setStrokeWidth(2);
            circlePaintArrow.setTypeface(BOLD_TYPEFACE);
            circlePaintArrow.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawCircle( (mXCenter + 98) , ((mYCenter * 2) - 88), 15, circlePaintArrow);
            Paint arrowPaint = new Paint();
            arrowPaint.setAntiAlias(true);
            canvas.drawBitmap( arrowBit, (mXCenter + 98)  - (arrowBit.getWidth()/2) , ((mYCenter * 2) - 95) ,arrowPaint);

            //Draw Date
            Paint datePaint = new Paint();
            datePaint.setAntiAlias(true);
            datePaint.setColor(getResources().getColor(R.color.gray_1));
            datePaint.setTextSize(12);
            datePaint.setTypeface(BOLD_TYPEFACE);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK,mTime.weekDay+1);
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
            String dayOfTheWeek = sdf.format(cal.getTime());
            String dateToShow = dayOfTheWeek+", "+mTime.monthDay;
            float dateTextWidth = datePaint.measureText(dateToShow);
            canvas.drawText(dateToShow , (mXCenter - (dateTextWidth/2)) , mYCenter + 90 , datePaint);

        }

        private void drawWidgetsMode2(Canvas canvas,String globActions,String degressTemperature,String shortLocation,boolean isActionUp, Bitmap arrowBit){

            //Drawing Day of month circle
            Paint circlePaint = new Paint();
            circlePaint.setColor(getResources().getColor(R.color.globant_green));
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2);
            circlePaint.setTypeface(BOLD_TYPEFACE);
            canvas.drawCircle(mXCenter, (mYCenter *2) - 40, 15, circlePaint);

            //Drawing Day of month text
            dayTextPaint = new Paint();
            dayTextPaint.setAntiAlias(true);
            dayTextPaint.setColor(colorTextGeneral);
            dayTextPaint.setTextSize(15);
            dayTextPaint.setTypeface(BOLD_TYPEFACE);
            int day = mTime.monthDay;
            String dayToShow = day+"";
            float dayTextWidth = dayTextPaint.measureText(dayToShow);
            canvas.drawText( dayToShow , mXCenter - (dayTextWidth/2) -1, (mYCenter *2) - 40 + 5, dayTextPaint);

            //Widget Mode 2 -------------------
            //Drawing widget 1
            Paint wPaint = new Paint();
            wPaint.setAntiAlias(true);
            wPaint.setColor(colorTextGeneral);
            wPaint.setStyle(Paint.Style.STROKE);
            wPaint.setStrokeWidth(2);
            wPaint.setTypeface(BOLD_TYPEFACE);
            wPaint.setShadowLayer(1, 0, 0, colorTextGeneral);
            canvas.drawRoundRect(new RectF(0 + 70, mYCenter + 75, mXCenter - 23, (mYCenter * 2) - 30), 30, 30, wPaint);
            //left top right bottom
            //Draw text widget 1
            Paint textPaintW1 = new Paint();
            textPaintW1.setAntiAlias(true);
            textPaintW1.setColor(colorTextGeneral);
            textPaintW1.setTextSize(25);
            textPaintW1.setTypeface(BOLD_TYPEFACE);
            //test
            if(degressTemperature == null)
                degressTemperature = "27";

            degressTemperature += "º";
            float temperatureWidth = textPaintW1.measureText(degressTemperature);
            canvas.drawText(degressTemperature, (((mXCenter - 23) - (((mXCenter - 23) - (0 + 70)) / 2)) - (temperatureWidth / 2)), ((mYCenter * 2) - 40), textPaintW1);
            //Drawing location
            Paint locationPaint = new Paint();
            locationPaint.setAntiAlias(true);
            locationPaint.setColor(colorTextGeneral);
            locationPaint.setTextSize(13);
            locationPaint.setTypeface(BOLD_TYPEFACE);
            /*//Test
            shortLocation = "MDP";*/
            if(shortLocation.length() > 0) {
                float locationWidth = locationPaint.measureText(shortLocation);
                canvas.drawText(shortLocation, ( ((mXCenter - 23)-(((mXCenter - 23)-(0 + 70))/2)) - (locationWidth/2) ), ((mYCenter * 2) - 65), locationPaint);
            }

            //Drawing widget 2
            canvas.drawRoundRect(new RectF(mXCenter + 23, mYCenter + 75, mXCenter * 2 - 70, (mYCenter * 2) - 30), 30, 30, wPaint);
            //Draw text widget 2
            Paint textPaintW2 = new Paint();
            textPaintW2.setAntiAlias(true);
            textPaintW2.setColor(colorTextGeneral);
            textPaintW2.setTextSize(21);
            textPaintW2.setTypeface(BOLD_TYPEFACE);
            //test
            if(globActions == null)
                globActions = "14.07";

            float x = textPaintW2.measureText(globActions);
            String[] aux = globActions.split("\\.");

            canvas.drawText( aux[0].toString() , ( ((mXCenter*2 - 70) -(((mXCenter*2 - 70) - (mXCenter + 23))/2)) - (x/2) ) + 5, ((mYCenter * 2) - 58), textPaintW2);
            textPaintW2.setTextSize(12);
            float startDecimal = textPaintW2.measureText(aux[0].toString());
            if( aux.length > 1 ) {
                canvas.drawText("  ." + aux[1].toString(), ( ((mXCenter*2 - 70) -(((mXCenter*2 - 70) - (mXCenter + 23))/2)) - (x/2) + startDecimal + 9 ), ((mYCenter * 2) - 58), textPaintW2);
            }
            Paint arrowPaint = new Paint();
            arrowPaint.setAntiAlias(true);
            canvas.drawBitmap( arrowBit, ((mXCenter*2 - 70) -(((mXCenter*2 - 70) - (mXCenter + 23))/2)) - (arrowBit.getWidth()/2) , ((mYCenter * 2) - 52) ,arrowPaint);

        }

        private void drawSeconds(Canvas canvas, Time mTime){


            int seconds = mTime.second;
            if( seconds == 0){
                degressOfSeconds = 360;
            }else {
                if (degressOfSeconds > 360) {
                    degressOfSeconds = 6;
                } else {
                    degressOfSeconds = 6 * seconds;
                }
            }

            //SweepGradient gradient1 = new SweepGradient(200, 520,Color.WHITE, getResources().getColor(R.color.globant_green));

            Paint p = new Paint();
            p.setColor(getResources().getColor(R.color.globant_green));
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            //p.setAlpha(130);
            //p.setShader(gradient1);
            p.setShadowLayer(2, 0, 0, Color.WHITE);
            canvas.drawArc(0 + 5 ,0 + 5, (mXCenter *2) - 5 , (mYCenter *2) - 5, 90 , degressOfSeconds, false, p);
        }

        private void drawMoto360Line(Canvas canvas, boolean drawOrNot){
            if(drawOrNot) {
                Paint redMoto360Line = new Paint();
                redMoto360Line.setColor(Color.RED);
                redMoto360Line.setStrokeWidth(1);
                canvas.drawLine(0, (mYCenter * 2) - 30, mXCenter * 2, (mYCenter * 2) - 30, redMoto360Line);
            }
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
            } /*else if (configKey.equals(DigitalWatchFaceUtil.KEY_HOURS_COLOR)) {
                setInteractiveHourDigitsColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_MINUTES_COLOR)) {
                setInteractiveMinuteDigitsColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_SECONDS_COLOR)) {
                setInteractiveSecondDigitsColor(color);
            } else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
                return false;
            }*/
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

    private void changeDrawables(int backgroundColor){
        int color = 0;
        if(backgroundColor == Constants.BACKGROUND_BLACK){
            globantLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logoglobant);
            wearereadyLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_weareready);
            color = getResources().getColor(R.color.white);
        }else{
            if(backgroundColor == Constants.BACKGROUND_WHITE){
                globantLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logoglobant_black);
                wearereadyLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_weareready_black);
                color = getResources().getColor(R.color.black);
            }
        }
        mHourPaint.setColor(color);
        mMinutePaint.setColor(color);
        colorTextGeneral = color;
    }
}
