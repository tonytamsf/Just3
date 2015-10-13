package com.wordpress.tonytam.just3;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Just3Wear extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    public int numLeft;
    HashMap<Integer, Integer> colorMapOn;
    HashMap<Integer, Integer> colorMapOff;
    private float origTextSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize state
        initState();

        setContentView(R.layout.activity_just3_wear);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        // mTextView = (TextView) findViewById(R.id.title);
        origTextSize = ((TextView) findViewById(R.id.item1)).getTextSize();
        attachEventsItems();

         mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        colorMapOn = new HashMap<Integer, Integer>(3);
        colorMapOn.put(R.id.item1, R.color.item1_on);
        colorMapOn.put(R.id.item2, R.color.item2_on);
        colorMapOn.put(R.id.item3, R.color.item3_on);

        colorMapOff = new HashMap<Integer, Integer>(3);
        colorMapOff.put(R.id.item1, R.color.item1_off);
        colorMapOff.put(R.id.item2, R.color.item2_off);
        colorMapOff.put(R.id.item3, R.color.item3_off);

        // Map color

        updateDisplay();
        Log.d("Just3War:onCreate - ", "STARTED");
    }

    private void initState() {
        numLeft = 3;
    }

    // love touch events
    private void attachEventsItems() {
        int ids[] = {R.id.item1, R.id.item2, R.id.item3};

        for (int i : ids) {
            TextView v = (TextView) findViewById(i);
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    onClick(v);

                    return false;
                }
            });
        }
    }
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(Color.BLACK);
            // mTextView.setTextColor(Color.DKGRAY);

            // mTextView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            // mTextView.setTextColor(Color.BLACK);
            if (numLeft == 0) {
                // mTextView.setTextColor(Color.GREEN);
                // mTextView.setText(getResources().getString(R.string.done));
            } else {
                // mTextView.setText(String.format(getResources().getString(R.string.title), numLeft));
            }
        }
    }

    public void onClick(View v) {

        final TextView textView = (TextView) v;
        Log.d("OnClick", textView.getText().toString());

        int c = textView.getCurrentTextColor();
        if (c != Color.GRAY) {
            textView.setTextColor(Color.GRAY);

            //textView.setBackgroundColor(getResources().getColor(colorMapOff.get(textView.getId())));
            animateBackground(textView,
                    getResources().getColor(colorMapOn.get(textView.getId())),
                    getResources().getColor(colorMapOff.get(textView.getId())),
                    Color.BLACK,
                    Color.GRAY
                    );
             numLeft--;
            textView.setTextSize((float)( origTextSize - 2.0));

        } else {
            textView.setBackgroundColor(getResources().getColor(colorMapOn.get(textView.getId())));
            animateBackground(textView,
                    getResources().getColor(colorMapOff.get(textView.getId())),
                    getResources().getColor(colorMapOn.get(textView.getId())),
                    Color.GRAY,
                    Color.BLACK
                    );
            textView.setTextColor(Color.BLACK);
            numLeft++;
            textView.setTextSize(origTextSize);
        }
        updateDisplay();
    }

    private
    void animateBackground(final TextView textView,
                           int colorFrom, int colorTo,
                           int textColorFrom, int textColorTo) {
        ValueAnimator colorAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        colorFrom, colorTo)
                .setDuration(1000);
        ValueAnimator colorBackgroundAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        textColorFrom, textColorTo)
                        .setDuration(1000);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                textView.setBackgroundColor((Integer) animator.getAnimatedValue());
            }

        });
        colorBackgroundAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                textView.setTextColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
        colorBackgroundAnimation.start();
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

    public void sendStepCount(int steps, long timestamp) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/step-count");

        putDataMapRequest.getDataMap().putInt("step-count", steps);
        putDataMapRequest.getDataMap().putLong("timestamp", timestamp);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.d("debug","Fail to send step count" );

                        } else {
                            Log.d("debug","Successfully send step count" );
                        }
                        // https://www.udacity.com/course/viewer#!/c-ud875A/l-4582940110/m-4580800289
                    }
                });
    }

}

