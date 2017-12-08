package com.jmw.rd.oddplay.download;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.util.Calendar;


public class Alarm extends BroadcastReceiver {

    public static void set(Context context, int minutesFromMidnight) {
        cancel(context);
        if (minutesFromMidnight > -1) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                throw new RuntimeException("Cannot get phone's alarm manager");
            }
            Intent notificationIntent = new Intent(context, Alarm.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int hour = minutesFromMidnight / 60;
            int minute = minutesFromMidnight % 60;
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

        }
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            throw new RuntimeException("Cannot get phone's alarm manager");
        }
        Intent notificationIntent = new Intent(context, Alarm.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);
        alarmManager.cancel(alarmIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DownloadController downloadController = new DownloadController(context);
        try {
            downloadController.downloadAllEpisodes();
        } catch (NoWifiException e) {
            StorageUtil.getStorage(context).fast.setStartupMessage("OddPlayer could not perform scheduled download because data turned off");
        }
    }
}
