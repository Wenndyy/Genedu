package com.owlvation.project.genedu.Network;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.owlvation.project.genedu.R;

public class NetworkDialogHelper {

    public static void showNoInternetDialog(Context context) {
        if (context instanceof Activity) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.dialog_title_no_internet))
                    .setMessage(context.getString(R.string.dialog_message_no_internet))
                    .setCancelable(false)
                    .setPositiveButton(context.getString(R.string.dialog_button_reload), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((Activity) context).recreate();
                        }
                    })
                    .setNegativeButton(context.getString(R.string.dialog_button_exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((Activity) context).finishAffinity();
                        }
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }
}
