package com.jmw.rd.oddplay.settings;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.util.Log;

import com.jmw.rd.oddplay.storage.StorageUtil;

import java.io.IOException;


public class BackupService extends IntentService {

    public static final String START_SERVER = "com.jmw.rd.oddplayer.settings.action.START_SERVER";
    public static final String STOP_SERVER = "com.jmw.rd.oddplayer.settings.action.STOP_SERVER";
    public static final String BACKUP_PORT = "com.jmw.rd.oddplayer.settings.action.PORT";
    public static final String RESULT_RECEIVER = "com.jmw.rd.oddplayer.RESULT_RECEIVER";
    public static final int SUCCESS = 0;
    public static final int ERROR = -1;
    public static final String ERROR_STRING = "com.jmw.rd.oddplayer.ERROR_STRING";
    private static PowerManager.WakeLock wakeLock;
    private static BackupFileServer backupServer;
    private ResultReceiver resultReceiver;

    public BackupService() {
        super("BackupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case START_SERVER:
                    int port = intent.getIntExtra(BACKUP_PORT, 9999);
                    resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
                    startServer(port);
                    break;
                case STOP_SERVER:
                    stopServer();
                    break;

            }
        }
    }


    private void startServer(int port) {
        try{
            startForeground(1995, new Notification());
            PowerManager mgr = (PowerManager) BackupService.this.getSystemService(POWER_SERVICE);
            wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
            wakeLock.acquire();
            backupServer = new BackupFileServer(this, StorageUtil.getStorage(this), port, resultReceiver);
            backupServer.start();
        } catch(IOException e) {
            if (resultReceiver != null) {
                Bundle bundle = new Bundle();
                bundle.putString(BackupService.ERROR_STRING, String.format("Could not start server: %s", Log.getStackTraceString(e)));
                resultReceiver.send(BackupService.ERROR, bundle);
            }
        }
    }

    private void stopServer() {
        if (backupServer != null) {
            backupServer.stop();
            backupServer = null;
        }
        if (wakeLock != null) {
            wakeLock.release();
        }
        stopForeground(true);
        BackupService.this.stopSelf();
    }

    public static boolean isRunning() {
        return backupServer != null;
    }


}
