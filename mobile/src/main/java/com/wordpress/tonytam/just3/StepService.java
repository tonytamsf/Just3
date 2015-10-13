package com.wordpress.tonytam.just3;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class StepService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(
                        dataEvent.getDataItem())
                        .getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals("/v1/step-counter")) {
                    int steps = dataMap.getInt("step-count");
                    long time = dataMap.getLong("timstamp");
                }
            }
        }
    }
}
