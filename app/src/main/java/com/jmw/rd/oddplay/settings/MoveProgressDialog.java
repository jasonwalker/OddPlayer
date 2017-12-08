package com.jmw.rd.oddplay.settings;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.ProgressBar;
import android.widget.TextView;
import com.jmw.rd.oddplay.R;


public class MoveProgressDialog  extends DialogFragment {

    private TextView fileCountView;
    private ProgressBar fileProgressBar;
    private ProgressBar overallProgressBar;

    public static MoveProgressDialog newInstance() {
        return new MoveProgressDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Window window = getDialog().getWindow();
        if (window == null) {
            throw new RuntimeException("Cannot get Move progress dialog.  Something is wrong with your phone");
        }
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View dialogLayout = inflater.inflate(R.layout.move_progress_dialog, container, false);
        fileCountView = (TextView) dialogLayout.findViewById(R.id.fileCountView);
        fileProgressBar = (ProgressBar) dialogLayout.findViewById(R.id.moveProgress);
        overallProgressBar = (ProgressBar) dialogLayout.findViewById(R.id.totalProgress);
        return dialogLayout;
    }

    public void setData(int overallTotal, int overallProgress, int fileTotal, int fileCurrent){
        this.overallProgressBar.setMax(overallTotal);
        this.overallProgressBar.setProgress(overallProgress);
        this.fileProgressBar.setMax(fileTotal);
        this.fileProgressBar.setProgress(fileCurrent);
    }

    public void setText(String text){
        fileCountView.setText(text);
    }

}
