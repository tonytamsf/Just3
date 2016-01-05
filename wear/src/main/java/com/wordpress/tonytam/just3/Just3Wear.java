package com.wordpress.tonytam.just3;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// TODO: Add alarm?  file:///Users/tonytam/Library/Android/sdk/docs/training/wearables/apps/always-on.html
public class Just3Wear extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // TODO: Maybe display a simple clock when in ambient
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    // Time in miliseconds for how long is a long hold to kick off changing
    // the data
    public static int LONG_PRESS_TIME = 1500;
    public static int HACK_NO_TOUCH_AREA = 150;
    private BoxInsetLayout mContainerView;
    private GoogleApiClient mGoogleApiClient;
    public int numLeft;
    HashMap<Integer, Integer> colorMapOn;
    HashMap<Integer, Integer> colorMapOff;
    HashMap<Integer, Boolean> itemDoneState;
    private float origTextSize;
    private TextView longPressedView;
    final Handler _handler = new Handler();
    private static final int SPEECH_REQUEST_CODE = 0;

    private static Boolean inPressed = false;

    Just3Wear that = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize state
        initState();

        setContentView(R.layout.activity_main_stub);
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub watchViewStub) {
                // NOTE: Don't hog batteries
                // file:///Users/tonytam/Library/Android/sdk/docs/training/wearables/apps/always-on.html
                // setAmbientEnabled();

                mContainerView = (BoxInsetLayout) findViewById(R.id.container);
                // mTextView = (TextView) findViewById(R.id.title);

                // Look at the first item text size, we'll downsize when an item is marked done
                origTextSize = ((TextView) findViewById(R.id.item1)).getTextSize();

                // We want touch events
                attachEventsItems();

                // TODO: Send data to the phone
                // I can't think of why, except to edit data on the phone which
                // I don't really want to
                mGoogleApiClient = new GoogleApiClient.Builder(that)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(that)
                        .addOnConnectionFailedListener(that)
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

                itemDoneState = new HashMap<Integer, Boolean>(3);


                refreshViewWithData();
                handleDifferentScreens();
                updateDisplay();
                Log.d("Just3War:onCreate - ", "STARTED");
            }
        });


    }

    // handle custom work for
    // 1) Moto 360 - leave the 20 pixel of bevel on the bottom, or hide it for others
    private void handleDifferentScreens() {
        // Darn Moto 360, the bottom bevel does not make my code elegant :)
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d("Screen",
                String.valueOf(width)
                        + " x "
                        + String.valueOf(height));
        View motoBox = findViewById(R.id.moto360filler);
        if (motoBox != null) {
            motoBox.getLayoutParams().height = width - height;
            motoBox.setLayoutParams(motoBox.getLayoutParams());
        }

        // a little left of center
        HACK_NO_TOUCH_AREA = (width / 2) - 20;
    }

    // Custom handler for detecting long press
    Runnable _longPressed = new Runnable() {
        public void run() {
            if (longPressedView != null) {
                displaySpeechRecognizer();
            }
            Log.i("info","LongPress");
        }
    };

    // Initialize my brand new state
    private void initState() {
        numLeft = 3;
    }

    // When simple tap, mark items as done or undone
    // When tap and hold for 800ms, then bring up the voice input
    // The ACTION_MOVE is to cancel the long hold because it may be a swipe to get back to the
    //    watch
    private void attachEventsItems() {
        int ids[] = {R.id.item1, R.id.item2, R.id.item3};

        for (int i : ids) {
            TextView v = (TextView) findViewById(i);
            v.setTag(i + 1);
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("onTouch",
                            String.valueOf(event.getAction())
                            + " X="
                                    + String.valueOf(event.getX()));
                    //
                    // Do not want to confuse a swipe to the left
                    // If a user touches any area to the left of HACK_NO_TOUCH_AREA, then we don't care
                    if (event.getX() < HACK_NO_TOUCH_AREA) {
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            longPressedView = (TextView) v;
                            onClick(v, event);
                            Log.d("onTouch", "action down");
                            return true;
                        case MotionEvent.ACTION_UP:
                            longPressedView = (TextView) v;

                            onClick(v, event);
                            Log.d("onTouch", "action up");

                            return true;
                    }
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

    // TODO: refresh the UI based on data changes
    // handle ambient mode
    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(R.color.item_text_on));
            // mTextView.setTextColor(Color.DKGRAY);

            // mTextView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            // mTextView.setTextColor(getResources().getColor(R.color.item_text_on));
            if (numLeft == 0) {
                // mTextView.setTextColor(Color.GREEN);
                // mTextView.setText(getResources().getString(R.string.done));
            } else {
                // mTextView.setText(String.format(getResources().getString(R.string.title), numLeft));
            }
        }
    }

    public void onClick(View v, MotionEvent event) {

        final TextView textView = (TextView) v;
        Log.d("onClick : set Item", textView.getTag().toString());

        if (inPressed) {
            inPressed = false;
            return;
        }
        if (! itemDoneState.get(textView.getTag())) {
            // Mark item as done
            Log.d("item state A", itemDoneState.get(textView.getTag()).toString());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setItemDone(textView);
                itemDoneState.put((Integer) textView.getTag(), true);

                // When user just tap item, mark it in state map
                // When they let go, we don't care awbout that event
                inPressed = true;

                /*
                Intent intent = new Intent(this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                        ConfirmationActivity.SUCCESS_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                        getString(R.string.tap_again_for_new));
                startActivity(intent);
                */

                Toast.makeText(getApplicationContext(),
                        getApplicationContext().
                                getString(R.string.tap_again_for_new),
                                 Toast.LENGTH_LONG).show();

            }
        } else {
            // Mark item as not done yet
            Log.d("item state B", itemDoneState.get(textView.getTag()).toString());
            if (event.getAction() == MotionEvent.ACTION_UP) {
                longPressedView = (TextView) v;

                displaySpeechRecognizer();

            }
        }

        updateDisplay();
        savePreferences();
    }

    private
    void setItemDone(TextView textView) {
        textView.setTextColor(Color.GRAY);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (origTextSize - 2.0));

        //textView.setBackgroundColor(getResources().getColor(colorMapOff.get(textView.getId())));
        animateBackground(textView,
                getResources().getColor(colorMapOn.get(textView.getId())),
                getResources().getColor(colorMapOff.get(textView.getId())),
                getResources().getColor(R.color.item_text_on),
                getResources().getColor(R.color.item_text_off)
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
                getResources().getColor(R.color.item_text_off),
                getResources().getColor(R.color.item_text_on)
        );
        textView.setTextColor(getResources().getColor(R.color.item_text_on));
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
        ArrayList<String> data = loadPreferences();

        ((TextView) findViewById(R.id.item1)).setText(data.get(0));
        ((TextView) findViewById(R.id.item2)).setText(data.get(1));
        ((TextView) findViewById(R.id.item3)).setText(data.get(2));
        int ids[] = {R.id.item1, R.id.item2, R.id.item3};

        for (int i = 1; i <= 3; i++) {
            if(itemDoneState.get(findViewById(ids[i-1]).getTag())) {
                Log.d("setItemDone", findViewById(ids[i-1]).getTag().toString());
                setItemDone((TextView) findViewById(ids[i - 1]));
            } else {
                Log.d("setItemNew", findViewById(ids[i-1]).getTag().toString());
                setItemNew((TextView) findViewById(ids[i - 1]));
            }
        }
    }

    // TODO: After loading data, recalculate what is numLeft
    public ArrayList<String> loadPreferences() {
        ArrayList<String> result = new ArrayList<>(3);
        int ids[] = {R.id.item1, R.id.item2, R.id.item3};

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        result.add(sharedPref.getString("item1", getResources().getString(R.string.item1)));
        result.add(sharedPref.getString("item2", getResources().getString(R.string.item2)));
        result.add(sharedPref.getString("item3", getResources().getString(R.string.item3)));

        // NOTE: Weird, Boolean.loadValue() doesn't work well here
        itemDoneState.put((int) findViewById(ids[0]).getTag(), sharedPref.getString("item1State", "false").equals("true"));
        itemDoneState.put((int) findViewById(ids[1]).getTag(), sharedPref.getString("item2State", "false").equals("true"));
        itemDoneState.put((int) findViewById(ids[2]).getTag(), sharedPref.getString("item3State", "false").equals("true"));

        Log.d("lP:itemState1", sharedPref.getString("item1State", "false"));
        Log.d("lP:itemState2", sharedPref.getString("item2State", "false"));
        Log.d("lP:itemState3", sharedPref.getString("item3State", "false"));

        return result;
    }

    // Save data as preferences
    // item[1-3] : text of the items
    // item[1-3]State : true if the item is done
    public void savePreferences() {
        int ids[] = {R.id.item1, R.id.item2, R.id.item3};

        //  TODO: send data to phone, what purpose?
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

        // TODO: refactor this
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("item1", ((TextView) findViewById(R.id.item1)).getText().toString());
        editor.putString("item2", ((TextView) findViewById(R.id.item2)).getText().toString());
        editor.putString("item3", ((TextView) findViewById(R.id.item3)).getText().toString());

        editor.putString("item1State", itemDoneState.get(findViewById(ids[0]).getTag()).toString());
        editor.putString("item2State", itemDoneState.get(findViewById(ids[1]).getTag()).toString());
        editor.putString("item3State", itemDoneState.get(findViewById(ids[2]).getTag()).toString());

        Log.d("save item1State", itemDoneState.get(findViewById(ids[0]).getTag()).toString());
        Log.d("save item2State", itemDoneState.get(findViewById(ids[1]).getTag()).toString());
        Log.d("save item3State", itemDoneState.get(findViewById(ids[2]).getTag()).toString());

        editor.commit();
    }

    // Create an intent that can start the Speech Recognizer activity
    // TODO: emoji as well?
    // http://developer.android.com/training/wearables/notifications/voice-input.html
    private void displaySpeechRecognizer() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,
                1);
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES,
                0.8);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                true);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);

    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        TextView v = longPressedView;

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);


            if (longPressedView != null) {
                v.setText(spokenText);

            }
        }

        if (longPressedView != null) {
            itemDoneState.put((Integer) longPressedView.getTag(), false);
            setItemNew(v);
            savePreferences();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

