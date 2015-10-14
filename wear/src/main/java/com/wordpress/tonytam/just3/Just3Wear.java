package com.wordpress.tonytam.just3;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Just3Wear extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    public static int LONG_PRESS_TIME = 500; // Time in miliseconds

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    public int numLeft;
    HashMap<Integer, Integer> colorMapOn;
    HashMap<Integer, Integer> colorMapOff;
    HashMap<Integer, Integer> itemState;
    private float origTextSize;
    final Handler _handler = new Handler();
    private TextView longPressedView;

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

        // touchy feely
        attachEventsItems();

         mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        // Setup color map
        colorMapOn = new HashMap<Integer, Integer>(3);
        colorMapOn.put(R.id.item1, R.color.item1_on);
        colorMapOn.put(R.id.item2, R.color.item2_on);
        colorMapOn.put(R.id.item3, R.color.item3_on);

        colorMapOff = new HashMap<Integer, Integer>(3);
        colorMapOff.put(R.id.item1, R.color.item1_off);
        colorMapOff.put(R.id.item2, R.color.item2_off);
        colorMapOff.put(R.id.item3, R.color.item3_off);

        refreshViewWithData();

        // Map color

        updateDisplay();
        Log.d("Just3War:onCreate - ", "STARTED");
    }

    Runnable _longPressed = new Runnable() {
        public void run() {
            if (longPressedView != null) {
                displaySpeechRecognizer();
            }
            Log.i("info","LongPress");
        }
    };

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
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            longPressedView = (TextView) v;
                            _handler.postDelayed(_longPressed, LONG_PRESS_TIME);
                            onClick(v);
                            return true;
                        case MotionEvent.ACTION_UP:

                            _handler.removeCallbacks(_longPressed);
                            return true;
                    }
                    return false;
                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClick(v);
                    return true;
                }
            });
        }
    }

    public void onLongClick(View v) {
        Log.d("onLongClick", v.toString());
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

        v.setFocusable(true);
        v.setEnabled(true);
        v.setClickable(true);
        v.setFocusableInTouchMode(true);

        int c = textView.getCurrentTextColor();
        if (c != Color.GRAY) {
            setItemNew(textView);

        } else {
            setItemDone(textView);
        }
        updateDisplay();

        // save Data
        sendTriData();
        if (numLeft == 0) {
            displaySpeechRecognizer();
        }
    }

    private
    void setItemDone(TextView textView) {
        textView.setTextColor(Color.GRAY);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (origTextSize - 2.0));

        //textView.setBackgroundColor(getResources().getColor(colorMapOff.get(textView.getId())));
        animateBackground(textView,
                getResources().getColor(colorMapOn.get(textView.getId())),
                getResources().getColor(colorMapOff.get(textView.getId())),
                Color.BLACK,
                Color.GRAY
        );
        textView.setTextColor(Color.GRAY);

        numLeft--;


    }
    private
    void setItemNew(TextView textView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, origTextSize);

        textView.setBackgroundColor(getResources().getColor(colorMapOn.get(textView.getId())));
        animateBackground(textView,
                getResources().getColor(colorMapOff.get(textView.getId())),
                getResources().getColor(colorMapOn.get(textView.getId())),
                Color.GRAY,
                Color.BLACK
        );
        textView.setTextColor(Color.BLACK);
        numLeft++;
    }
    private
    void animateBackground(final TextView textView,
                           int colorFrom, int colorTo,
                           int textColorFrom, int textColorTo) {
        ValueAnimator colorAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        colorFrom, colorTo)
                .setDuration(300);
        ValueAnimator colorBackgroundAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        textColorFrom, textColorTo)
                        .setDuration(300);
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

    public void refreshViewWithData() {
        // Load Data
        ArrayList<String> data = loadDataFromPreferences();
        ((TextView) findViewById(R.id.item1)).setText(data.get(0));
        ((TextView) findViewById(R.id.item2)).setText(data.get(1));
        ((TextView) findViewById(R.id.item3)).setText(data.get(2));
    }

    public ArrayList<String> loadDataFromPreferences() {
        ArrayList<String> result = new ArrayList<String>(3);

        Context context = getApplicationContext();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        result.add(sharedPref.getString("item1", ""));
        result.add(sharedPref.getString("item2", ""));
        result.add(sharedPref.getString("item3", ""));
        return result;
    }

    public void sendTriData() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/v1/tri-data");
        putDataMapRequest.getDataMap().putString("item1", ((TextView) findViewById(R.id.item1)).getText().toString());
        putDataMapRequest.getDataMap().putString("item2", ((TextView) findViewById(R.id.item2)).getText().toString());
        putDataMapRequest.getDataMap().putString("item3", ((TextView) findViewById(R.id.item3)).getText().toString());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.d("debug", "Fail to send item");

                        } else {
                            Log.d("debug", "Successfully saved item");
                        }
                        // https://www.udacity.com/course/viewer#!/c-ud875A/l-4582940110/m-4580800289
                    }
                });

        // Save data into preferences
        Context context = getApplicationContext();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("item1", ((TextView) findViewById(R.id.item1)).getText().toString());
        editor.putString("item2", ((TextView) findViewById(R.id.item2)).getText().toString());
        editor.putString("item3", ((TextView) findViewById(R.id.item3)).getText().toString());
        editor.commit();
    }


    public void sendStepCount(int steps, long timestamp) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/v1/step-count");

        putDataMapRequest.getDataMap().putInt("step-count", steps);
        putDataMapRequest.getDataMap().putLong("timestamp", timestamp);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.d("debug", "Fail to send step count");

                        } else {
                            Log.d("debug", "Successfully send step count");
                        }
                        // https://www.udacity.com/course/viewer#!/c-ud875A/l-4582940110/m-4580800289
                    }
                });
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            if (longPressedView != null) {
                TextView v = (TextView) longPressedView;
                v.setText(spokenText);
                setItemNew(v);
            }
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}

