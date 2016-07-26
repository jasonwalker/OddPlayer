package com.jmw.rd.oddplay.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;
import com.jmw.rd.oddplay.widgets.SmallButton;


public class FeedsSearchInputPopup extends PopupDialogFragment {
    private EditText enteredSearchTerm;


    public static FeedsSearchInputPopup newInstance() {
        return new FeedsSearchInputPopup();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.search_feed_input_dialog, null);
        SmallButton searchFeedButton = (SmallButton) dialogLayout.findViewById(R.id.addSearchFeed);
        searchFeedButton.setOnTouchListener(new SearchForTerm());

        SmallButton cancelFeedButton = (SmallButton) dialogLayout.findViewById(R.id.cancelSearchFeed);
        cancelFeedButton.setOnTouchListener(new CancelTouchListener());

        enteredSearchTerm = (EditText) dialogLayout.findViewById(R.id.enteredFeedSearchTerm);

        enteredSearchTerm.requestFocus();
        // pull up keyboard immediately
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        cancelFeedButton.setOnTouchListener(new CancelTouchListener());
        return dialogLayout;
    }

    private class SearchForTerm implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dismiss();
            FeedsSearchPopup popup = FeedsSearchPopup.newInstance(enteredSearchTerm.getText().toString());
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            popup.show(transaction, "feedsSearch");
            return false;
        }
    }

    private class CancelTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dismiss();
            return false;
        }
    }
}

