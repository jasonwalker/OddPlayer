package com.jmw.rd.oddplay.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Window;

import com.jmw.rd.oddplay.R;


public class Dialog {
    public static AlertDialog showOK(Context context, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg)
                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window == null) {
            throw new RuntimeException("Could not get dialog window, problem with phone");
        }
        window.getAttributes().windowAnimations = R.style.dialogSlide;
        dialog.show();
        return dialog;
    }

    public static AlertDialog showYesNo(Context context, String msg,  DialogInterface.OnClickListener onYes,
                                        DialogInterface.OnClickListener onNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg)
                .setPositiveButton(context.getString(R.string.yes), onYes)
                .setNegativeButton(context.getString(R.string.no), onNo);

        AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window == null) {
            throw new RuntimeException("Could not get dialog window, problem with phone");
        }
        window.getAttributes().windowAnimations = R.style.dialogSlide;
        dialog.show();
        return dialog;
    }

}
