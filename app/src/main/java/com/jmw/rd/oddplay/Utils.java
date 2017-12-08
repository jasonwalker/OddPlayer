package com.jmw.rd.oddplay;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.storage.StorageUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static final DateTimeFormatter timeFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");


    /**
     * writes all of input stream to output stream
     * @param is input stream to read
     * @param os output stream to write
     * @param sentListener a listener to periodically report amount written
     * @return long indicated total amount written
<<<<<<< HEAD
     * @throws IOException thrown when problem with input or output stream
     * @throws EmergencyDownloadStopException thrown when user stops download
=======
     * @throws IOException if can't read input or output stream
     * @throws EmergencyDownloadStopException if user stops download
>>>>>>> 15b7d753cfbb0fd5c4d2419f644aa972dd4dee62
     */
    public static long IsToOs(InputStream is, OutputStream os, AmountSentCommunicator sentListener)
            throws IOException, EmergencyDownloadStopException {
        byte[] buf = new byte[128 * 1024];
        int len = is.read(buf);
        if (sentListener != null) {
            sentListener.newDownload();
        }
        long total = 0;
        long sendTotal = 0;
        while (len > -1) {
            if (sentListener != null && sentListener.stopNow() != null) {
                throw sentListener.stopNow();
            }
            os.write(buf, 0, len);
            total += len;
            sendTotal += len;
            if (sentListener != null && sendTotal > 128000) {
                sentListener.totalToOutputStream(total);
                sendTotal = 0;
            }
            len = is.read(buf);
        }
        if (sentListener != null) {
            sentListener.totalToOutputStream(total);
        }
        return total;
    }

    public static String formatTime(long msTime) {
        final long hours = TimeUnit.MILLISECONDS.toHours(msTime);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(msTime);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(msTime);
        String formattedTime;
        if (hours > 0) {
            formattedTime = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds % 60);
        }
        return formattedTime;
    }

    public static String dateStringFromLong(long time) {
        return timeFormat.print(time);
    }

    public static boolean isDataOn(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager == null) {
            throw new RuntimeException("Could not get connectivity manager.  Something is wrong with your phone");
        }
        NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo ethernetNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if ((wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) || (ethernetNetworkInfo != null && ethernetNetworkInfo.isConnected())) {
            return true;
        } else if (!StorageUtil.getStorage(context).fast.getUseOnlyWIFI()) {
            NetworkInfo mobileNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobileNetworkInfo.isConnected();
        } else {
            return false;
        }
    }

    public static long saveStreamToFile(File outputDir, String fileName, InputStream is, AmountSentCommunicator sentListener)
            throws IOException, EmergencyDownloadStopException {
        File dataFile = new File(outputDir, fileName);
        try {
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                return Utils.IsToOs(is, fos, sentListener);
            }
        } catch (EmergencyDownloadStopException e) {
            dataFile.delete();
            throw e;
        }
    }
    public static String getBuildTime () {
        String buildTime = "";
        try  {
            buildTime = dateStringFromLong(BuildConfig.TIMESTAMP);
        } catch (Exception e) {
            //just return empty string
        }
        return buildTime;
    }

    public static String getIPAddress(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean wifiConnected = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        if (wifiConnected) {
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            return Formatter.formatIpAddress(ip);
        } else {
            return null;
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputMethodManager.isAcceptingText()) { // verify if the soft keyboard is open
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public static void showKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }
}
