package com.jmw.rd.oddplay.widgets;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Window;

import com.jmw.rd.oddplay.R;

public class PopupDialogFragment extends DialogFragment{

    @Override
    public android.app.Dialog onCreateDialog(Bundle saveInstanceState) {
        final android.app.Dialog dialog = super.onCreateDialog(saveInstanceState);
        Window window = dialog.getWindow();
        if (window == null) {
            throw new RuntimeException("Could not get dialog window, problem with phone");
        }
        window.getAttributes().windowAnimations = R.style.dialogSlide;
        return dialog;
    }

}
