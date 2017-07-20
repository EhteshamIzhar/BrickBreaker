package com.example.ehte6848.brickbreaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.InflateException;
import android.view.View;

/**
 * Created by ehte6848 on 19-07-2017.
 */
public class AboutBox {
    private static final String TAG = BreakoutActivity.TAG;


    public static void display(Activity caller) {
       String versionStr = "new";
               //getVersionString(caller);
        String aboutHeader = caller.getString(R.string.app_name) + " v" + versionStr;

        // Manually inflate the view that will form the body of the dialog.
        View aboutView;
        try {
            aboutView = caller.getLayoutInflater().inflate(R.layout.about, null);
        } catch (InflateException ie) {
            Log.e(TAG, "Exception while inflating about box: " + ie.getMessage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(caller);
        builder.setTitle(aboutHeader);
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setCancelable(true);        // implies setCanceledOnTouchOutside
        builder.setPositiveButton(R.string.ok, null);
        builder.setView(aboutView);
        builder.show();
    }
}
