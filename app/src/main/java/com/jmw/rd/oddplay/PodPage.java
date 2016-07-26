package com.jmw.rd.oddplay;


//import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.jmw.rd.oddplay.play.PlayController;

public class PodPage extends Fragment {

    protected PlayController playController;
    protected FragmentActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playController = new PlayController(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        playController.promptForPlayInfo();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (FragmentActivity) context;
    }

}


