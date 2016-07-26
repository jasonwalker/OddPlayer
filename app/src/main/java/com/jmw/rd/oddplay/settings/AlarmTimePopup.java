package com.jmw.rd.oddplay.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TimePicker;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

public class AlarmTimePopup extends PopupDialogFragment {
    private TimePicker alarmTimePicker;
    private Context context;

    public static AlarmTimePopup newInstance() {
        AlarmTimePopup popup = new AlarmTimePopup();
        Bundle args = new Bundle();
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.set_download_time_dialog, null);
        alarmTimePicker = (TimePicker) dialogLayout.findViewById(R.id.alarmTimePicker);
        Button applyAlarmTime = (Button) dialogLayout.findViewById(R.id.applyAlarmTimeButton);
        applyAlarmTime.setOnTouchListener(new SetAlarmTimeListener());
        Button cancelButton = (Button) dialogLayout.findViewById(R.id.cancelSetApplyAlarmTimeButton);
        cancelButton.setOnTouchListener(new CancelAlarmTimeListener());
        return dialogLayout;
    }

    private class SetAlarmTimeListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                int hour = alarmTimePicker.getCurrentHour();
                int minute = alarmTimePicker.getCurrentMinute();
                int time = (hour * 60) + minute;
                StorageUtil.getStorage(context).fast.setDownloadScheduleTime(time);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, new Intent());
                dismiss();
            }
            return false;
        }
    }

    private class CancelAlarmTimeListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, new Intent());
            dismiss();
            return false;
        }
    }


}

