package com.example.foozi.mythermalapplication;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.widget.TextView;

import com.flir.flironesdk.*;

import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements Device.Delegate, FrameProcessor.Delegate{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private View mContentView;
    private View mControlsView;
    private boolean mVisible;
    private FrameProcessor frameProcessor;
    private Paint horizontalCrosshairPaint;
    private Paint verticalCrosshairPaint;
    private Paint mShadowPaint;
    private Paint hollowPaint=new Paint();
    private float xRes = 0;
    private float yRes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        frameProcessor = new FrameProcessor(this,this, EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image)); //This is where you select the type of image
        setContentView(R.layout.activity_fullscreen);

        mShadowPaint = new Paint(0);
        mShadowPaint.setColor(0xff101010);

        hollowPaint.setStyle(Paint.Style.STROKE);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        xRes = size.x;
        yRes = size.y;


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.imageView).setOnTouchListener(mDelayHideTouchListener);
    }

    float xTouch =-1;
    float yTouch =-1;
/*    @Override
    public boolean onTouchEvent(MotionEvent e) {
        xTouch = e.getX();
        yTouch = e.getY();
        return true;
    }*/

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
           /* if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }*/
            xTouch = motionEvent.getX();
            yTouch = motionEvent.getY();
            return false;
        }
    };

    /*private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }*/

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    Device flirDevice;

    @Override
    protected void onResume(){
        super.onResume();
        Device.startDiscovery(this, this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Device.stopDiscovery();
    }

    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {

    }

    @Override
    public void onAutomaticTuningChanged(boolean b) {

    }

    private int frameCount = 0;
    @Override
    public void onDeviceConnected(Device device) {
        flirDevice = device;

        // globally
        final TextView temperatureText = (TextView)findViewById(R.id.temperatureText);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //temperatureText.setText("My Awesome Text");
                temperatureText.setVisibility(View.VISIBLE);
            }
        });

        device.startFrameStream(new Device.StreamDelegate() {
            @Override
            public void onFrameReceived(Frame frame) {
                if(frameCount%2 == 0) {
                    frameProcessor.processFrame(frame);
                }
                frameCount ++;
            }
        });
    }

    @Override
    public void onDeviceDisconnected(Device device) {

    }
short count = 0;

    @Override
    public void onFrameProcessed(RenderedImage renderedImage) {
        final Bitmap tempImageBitmap = Bitmap.createBitmap(renderedImage.width(), renderedImage.height(), Bitmap.Config.ARGB_8888);//something here
        tempImageBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(renderedImage.pixelData()));

        int height = tempImageBitmap.getHeight();
        int width = tempImageBitmap.getWidth();

        // create new matrix for transformation
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, -1.0f);
        final Bitmap imageBitmap = Bitmap.createBitmap(tempImageBitmap, 0, 0, tempImageBitmap.getWidth(), tempImageBitmap.getHeight(), matrix, true);

        //imageBitmap.
        final ImageView imageView = (ImageView)findViewById(R.id.imageView);
        final TextView temperatureText = (TextView)findViewById(R.id.temperatureText);
        final double centerTemperature = (xTouch/xRes)*imageBitmap.getWidth();//renderedImage.pixelData()[(height/2) + (width/2)];
        Canvas c = new Canvas(imageBitmap);

        float x =(xTouch/xRes)*imageBitmap.getWidth();
        float y =(yTouch/yRes)*imageBitmap.getHeight();

        c.drawCircle(x, y, 1, hollowPaint);
        c.drawLine(x-4,0,3,0,mShadowPaint);
        c.drawLine(x+2,0,3,0,mShadowPaint);
        //c.drawCircle(imageBitmap.getWidth()-50, imageBitmap.getHeight()-50, 2, mShadowPaint);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(imageBitmap);
                temperatureText.setText(centerTemperature + "ºC");
                temperatureText.setVisibility(View.VISIBLE);
            }
        });
    }

    public void drawCrosshair(float x, float y)
    {

    }
/*
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the shadow
        canvas.drawOval(10,10,10,10,mShadowPaint);
    }*/

}
