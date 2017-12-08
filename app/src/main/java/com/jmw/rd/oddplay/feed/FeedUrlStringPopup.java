package com.jmw.rd.oddplay.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;

public class FeedUrlStringPopup extends PopupDialogFragment {
    private static final String URLSTRING = "url";
    private EditText enteredUrl;
    private FeedController feedController;

    public static FeedUrlStringPopup newInstance(String url) {
        FeedUrlStringPopup popup = new FeedUrlStringPopup();
        Bundle args = new Bundle();
        args.putString(URLSTRING, url);
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void dismiss() {
        Utils.hideKeyboard(getActivity(), enteredUrl);
        super.dismiss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Window window = getDialog().getWindow();
        if (window == null) {
            throw new RuntimeException("Cannot get window for feed url string popup.  Something is wrong with your phone");
        }
        window.requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = inflater.inflate(R.layout.feeds_enter_url_string, null);
        Button submitUrlButton = (Button) dialogLayout.findViewById(R.id.submitUrlButton);
        submitUrlButton.setOnTouchListener(new OnSubmitUrl());
        enteredUrl = (EditText) dialogLayout.findViewById(R.id.enteredURL);
        Button dismissButton = (Button) dialogLayout.findViewById(R.id.enterUrlDismissButton);
        dismissButton.setOnTouchListener(new OnCancelClickListener());
        feedController = new FeedController(getActivity());
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialogLayout;
    }

    @Override
    public void onResume(){
        super.onResume();
        String startingUrl = getArguments().getString(URLSTRING);
        enteredUrl.setText(startingUrl);
        enteredUrl.requestFocus();

    }


    private class OnSubmitUrl implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                feedController.broadcastAddFeed(enteredUrl.getText().toString());
                dismiss();
            }
            return false;
        }
    }

    private class OnCancelClickListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismiss();
            }
            return false;
        }
    }
}
