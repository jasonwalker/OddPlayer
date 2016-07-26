package com.jmw.rd.oddplay.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.jmw.rd.oddplay.AmountSentCommunicator;
import com.jmw.rd.oddplay.BuildConfig;
import com.jmw.rd.oddplay.PodPage;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.Alarm;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.widgets.Dialog;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class SettingsViewPage extends PodPage {
    private TextView buildDateView;
    private Storage storage;
    private CheckBox useOnlyWIFI;
    private CheckBox deleteAfterListening;
    private CheckBox useScheduledDownloadTime;
    private TextView downloadTimeTextView;
    private EditText maxDownloadsPerFeed;
    private EditText numberMsToSkipInput;
    private Spinner externalStorageSelector;
    private ArrayAdapter<String> storageSelector;
    private TextView settingsInfo;
    private Button exportDataStartButton;
    private Button exportDataStopButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = StorageUtil.getStorage(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View fragmentLayout = inflater.inflate(R.layout.main_view_settings, container, false);
        buildDateView = (TextView) fragmentLayout.findViewById(R.id.buildDate);
        settingsInfo = (TextView) fragmentLayout.findViewById(R.id.settingsInfo);
        maxDownloadsPerFeed = (EditText) fragmentLayout.findViewById(R.id.maxDownloadsPerFeed);
        maxDownloadsPerFeed.addTextChangedListener(new MaxDownloadsChangedListener());
        numberMsToSkipInput = (EditText) fragmentLayout.findViewById(R.id.numberMsToSkipInput);
        numberMsToSkipInput.addTextChangedListener(new NumberMSPerSkipChangedListener());
        useOnlyWIFI = (CheckBox) fragmentLayout.findViewById(R.id.useOnlyWifiCheckbox);
        useOnlyWIFI.setOnClickListener(new UseWifiClickListener());
        deleteAfterListening = (CheckBox) fragmentLayout.findViewById(R.id.deleteAfterListening);
        deleteAfterListening.setOnClickListener(new DeleteAfterListeningListener());
        useScheduledDownloadTime = (CheckBox) fragmentLayout.findViewById(R.id.downloadTimeSet);
        useScheduledDownloadTime.setOnClickListener(new DownloadTimeSetListener());
        downloadTimeTextView = (TextView) fragmentLayout.findViewById(R.id.downloadTimeTextView);
        if (storage.fast.getUsingDownloadScheduleTime()) {
            setAlarmTimeDisplay(storage.fast.getDownloadScheduleTime());
        } else {
            setAlarmTimeDisplay(-1);
        }
        externalStorageSelector = (Spinner) fragmentLayout.findViewById(R.id.externalStorageSelector);
        storageSelector = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item);
        externalStorageSelector.setAdapter(storageSelector);
        externalStorageSelector.setOnItemSelectedListener(new OnSelectStorageListener());
        exportDataStartButton = (Button) fragmentLayout.findViewById(R.id.exportDataStartButton);
        exportDataStartButton.setOnTouchListener(new ExportDataStartListener());
        exportDataStartButton.setEnabled(true);
        exportDataStopButton = (Button) fragmentLayout.findViewById(R.id.exportDataStopButton);
        exportDataStopButton.setOnTouchListener(new ExportDataStopListener());
        exportDataStopButton.setEnabled(false);
        PopulateViewTask populate = new PopulateViewTask();
        populate.execute();
        return fragmentLayout;
    }
    private class PopulateViewTask extends AsyncTask<Void, Void, Void> {
        private String buildDate;
        private boolean onlyWifi;
        private boolean deleteAfter;
        private int selectedStorage;
        private String maxDownloads;
        private String msToSkip;
        private List<String> storageOptions;

        @Override
        protected Void doInBackground(Void... unused) {
            buildDate = Utils.getBuildTime(activity);
            onlyWifi = storage.fast.getUseOnlyWIFI();
            deleteAfter = storage.fast.getDeleteAfterListening();
            selectedStorage = storage.getSelectedStorage();
            maxDownloads = Long.toString(storage.fast.getMaxDownloadsPerFeed());
            msToSkip = Integer.toString(storage.fast.getSkipDistance());
            storageOptions = storage.getStorageOptionsList(activity);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            buildDateView.setText(buildDate);
            useOnlyWIFI.setChecked(onlyWifi);
            deleteAfterListening.setChecked(deleteAfter);
            externalStorageSelector.setSelection(selectedStorage);
            maxDownloadsPerFeed.setText(maxDownloads);
            numberMsToSkipInput.setText(msToSkip);
            storageSelector.addAll(storageOptions);
        }
    }

    private void setAlarmTimeDisplay(int minutes) {
        String time;
        if (minutes < 0) {
            time = "---";
            useScheduledDownloadTime.setChecked(false);
        } else {
            useScheduledDownloadTime.setChecked(true);
            int hour = minutes / 60;
            int minute = minutes % 60;
            if (DateFormat.is24HourFormat(activity)) {
                time = String.format("%d:%02d", hour, minute);
            } else {
                if (hour > 12) {
                    time = String.format("%d:%02d %s", hour - 12, minute, "pm");
                } else {
                    time = String.format("%d:%02d %s", hour, minute, "am");
                }
            }
        }
        downloadTimeTextView.setText(time);
    }


    @Override
    public void onResume() {
        super.onResume();
        exportDataStartButton.setEnabled(!BackupService.isRunning());
        exportDataStopButton.setEnabled(BackupService.isRunning());
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
    }

    class MoveData{
        public final int overallTotal;
        public final int overallProgress;
        public final int fileTotal;
        public final int fileProgress;
        public final String text;

        public MoveData(int overallTotal, int overallProgress, int fileTotal, int fileProgress, String text) {
            this.overallTotal = overallTotal;
            this.overallProgress = overallProgress;
            this.fileTotal = fileTotal;
            this.fileProgress = fileProgress;
            this.text = text;
        }
    }

    class MoveFilesTask extends AsyncTask<Void, MoveData, Void> {
        File srcDir;
        File dstDir;
        int newPosition;
        MoveProgressDialog dialog;

        public MoveFilesTask(File srcDir, File dstDir, int newPosition, MoveProgressDialog dialog) {
            if (BuildConfig.DEBUG) {
                if (!srcDir.isDirectory()) {
                    throw new RuntimeException("SrcDir must be a dir");
                }
                if (!dstDir. isDirectory()) {
                    throw new RuntimeException("DstDir must be a dir");
                }
            }
            this.srcDir = srcDir;
            this.dstDir = dstDir;
            this.newPosition = newPosition;
            this.dialog = dialog;
        }



        class FileSentListener implements AmountSentCommunicator {

            final int currNumber;
            final int total;
            final int fileSize;

            public FileSentListener(int downloadNumber, int totalFiles, long fileSize){
                this.currNumber = downloadNumber;
                this.total = totalFiles;
                this.fileSize = (int) fileSize;
            }

            @Override
            public void newDownload() {

            }

            @Override
            public EmergencyDownloadStopException stopNow() {
                return null;
            }

            @Override
            public void totalToOutputStream(long amt) {
                publishProgress(new MoveData(total, currNumber, fileSize, (int)amt, null));
            }
        }

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                File[] files = this.srcDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    publishProgress(new MoveData(0, 0, 0, 0, String.format(activity.getString(R.string.movingFile), i+1, files.length)));
                    storage.moveFile(files[i], dstDir, new FileSentListener(i, files.length, files[i].length()));
                }
                storage.setSelectedStorage(this.newPosition);
                publishProgress(new MoveData(0, 0, 0, 0, String.format(activity.getString(R.string.completedMovingFile), files.length)));
                return null;
            }catch (IOException e) {
                publishProgress(new MoveData(0, 0, 0, 0, String.format(activity.getString(R.string.fileMoveFailed), e.getMessage())));
            }catch(EmergencyDownloadStopException e) {
                Toast.makeText(activity, R.string.emergencyShutdown, Toast.LENGTH_LONG).show();
            } finally {
                this.dialog.dismiss();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(MoveData... info) {
            MoveData current = info[info.length-1];
            if (current.text != null) {
                this.dialog.setText(current.text);
            } else {
                this.dialog.setData(current.overallTotal, current.overallProgress, current.fileTotal, current.fileProgress);
            }
        }

    }

    private class OnSelectStorageListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
            if (position == storage.getSelectedStorage()){
                return;
            }
            DialogInterface.OnClickListener dialogYesClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MoveProgressDialog popup = MoveProgressDialog.newInstance();
                    try {
                        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                        popup.show(transaction, "dialog");
                        final MoveFilesTask deleteTask = new MoveFilesTask(storage.getEpisodesDirForStoragePosition(storage.getSelectedStorage()),
                                storage.getEpisodesDirForStoragePosition(position), position, popup);
                        deleteTask.execute();
                    } catch (ResourceAllocationException e){
                        settingsInfo.setText(activity.getString(R.string.couldNotMoveFiles) + e.getMessage());
                    }
                }
            };
            DialogInterface.OnClickListener dialogNoClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    externalStorageSelector.setSelection(storage.getSelectedStorage());
                }
            };
            Dialog.showYesNo(activity, activity.getString(R.string.confirmChangeStorage),
                    dialogYesClickListener, dialogNoClickListener);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class MaxDownloadsChangedListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String val = s.toString();
            long toEnter = -1;
            try {
                if (val.length() > 0) {
                    toEnter = Long.parseLong(val);
                }
                storage.fast.setMaxDownloadsPerFeed(toEnter);
            } catch (NumberFormatException e) {
                Dialog.showOK(activity, String.format(activity.getString(R.string.notValidNumber), val));
            }
        }
    }

    private class NumberMSPerSkipChangedListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String val = s.toString();
            int toEnter = 15000;
            try {
                if (val.length() > 0) {
                    toEnter = Integer.parseInt(val);
                }
                storage.fast.setSkipDistance(toEnter);
            } catch (NumberFormatException e) {
                Dialog.showOK(activity, String.format(activity.getString(R.string.notValidNumber), val));
            }
        }
    }

    private class UseWifiClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (useOnlyWIFI.isChecked()) {
                storage.fast.setUseOnlyWIFI(true);
            } else {
                DialogInterface.OnClickListener dialogYesClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        storage.fast.setUseOnlyWIFI(false);
                    }
                };
                DialogInterface.OnClickListener dialogNoClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        useOnlyWIFI.setChecked(true);
                    }
                };
                Dialog.showYesNo(activity, activity.getString(R.string.confirmEnableMobileData),
                        dialogYesClickListener, dialogNoClickListener);
            }
        }
    }

    private class DeleteAfterListeningListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (deleteAfterListening.isChecked()) {
                DialogInterface.OnClickListener dialogYesClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        storage.fast.setDeleteAfterListening(true);
                    }
                };
                DialogInterface.OnClickListener dialogNoClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        storage.fast.setDeleteAfterListening(false);
                        deleteAfterListening.setChecked(false);
                    }
                };
                Dialog.showYesNo(activity, activity.getString(R.string.deleteEpisodeAfterListening),
                        dialogYesClickListener, dialogNoClickListener);
            } else {
                storage.fast.setDeleteAfterListening(false);
            }
        }
    }

    private class DownloadTimeSetListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (useScheduledDownloadTime.isChecked()) {
                StorageUtil.getStorage(activity).fast.setUsingDownloadScheduleTime(true);
                AlarmTimePopup popup = AlarmTimePopup.newInstance();
                popup.setTargetFragment(SettingsViewPage.this, 1);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                popup.show(transaction, "chooseDownloadTime");
            } else {
                Alarm.cancel(activity);
                StorageUtil.getStorage(activity).fast.setDownloadScheduleTime(-1);
                setAlarmTimeDisplay(-1);
                StorageUtil.getStorage(activity).fast.setUsingDownloadScheduleTime(false);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.e("JJJ", "onActivityResult: " + resultCode);
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            int downloadMinutes = StorageUtil.getStorage(activity).fast.getDownloadScheduleTime();
            setAlarmTimeDisplay(downloadMinutes);
            Alarm.set(activity, downloadMinutes);
        } else {
            useScheduledDownloadTime.setChecked(false);
            setAlarmTimeDisplay(-1);
            Alarm.cancel(activity);
            StorageUtil.getStorage(activity).fast.setUsingDownloadScheduleTime(false);
        }
    }

    private class ExportDataStartListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (Utils.isDataOn(activity)) {
                if (!BackupService.isRunning()) {
                    int port = 9999;
                    final Intent intent = new Intent(activity, BackupService.class);
                    intent.setAction(BackupService.START_SERVER);
                    intent.putExtra(BackupService.BACKUP_PORT, port);
                    intent.putExtra(BackupService.RESULT_RECEIVER, new ResultReceiver(null) {
                        @Override
                        protected void onReceiveResult(int resultCode, final Bundle resultData) {
                            if (resultCode == BackupService.ERROR) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Dialog.showOK(activity, resultData.getString(BackupService.ERROR_STRING));
                                    }
                                });
                            }
                        }
                    });
                    activity.startService(intent);
                    Dialog.showOK(activity, String.format(activity.getString(R.string.goToUrlForBackup),
                            Utils.getIPAddress(activity), port));
                    exportDataStartButton.setEnabled(false);
                    exportDataStopButton.setEnabled(true);
                }
            } else {
                if (storage.fast.getUseOnlyWIFI()) {
                    Dialog.showOK(activity, activity.getString(R.string.enableWifiForBackup));
                } else {
                    Dialog.showOK(activity, activity.getString(R.string.enableWifiOrMobileDataForBackup));
                }
            }
            return false;
        }

    }

    private class ExportDataStopListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (BackupService.isRunning()) {
                final Intent intent = new Intent(activity, BackupService.class);
                intent.setAction(BackupService.STOP_SERVER);
                activity.startService(intent);
                exportDataStartButton.setEnabled(true);
                exportDataStopButton.setEnabled(false);
            }
            //new UploadDB().execute();
            return false;
        }

    }



}
