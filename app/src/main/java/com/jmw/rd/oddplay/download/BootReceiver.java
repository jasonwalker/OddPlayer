package com.jmw.rd.oddplay.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.jmw.rd.oddplay.storage.StorageUtil;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (StorageUtil.getStorage(context).fast.getUsingDownloadScheduleTime()) {
            int downloadTime = StorageUtil.getStorage(context).fast.getDownloadScheduleTime();
            Alarm.set(context, downloadTime);
        }
    }
}
