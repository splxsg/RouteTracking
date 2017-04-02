package com.bfbstudio.routetracking.UI;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.bfbstudio.routetracking.R;
import com.bfbstudio.routetracking.rest.Utility;

/**
 * Created by Blues on 31/03/2017.
 */

public class SettingDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.setting_menu_title)
        .setSingleChoiceItems(R.array.setting_unit_radio_group, Utility.getUnitKey(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which != -1)
                    switch(which)
                    {
                        case 0:
                            Utility.setUnitKey(Utility.DISTANCE_MILES);
                            break;
                        case 1:
                            Utility.setUnitKey(Utility.DISTANCE_KILOMETERS);
                            break;
                    }



            }
        });

        return builder.create();
    }
}
