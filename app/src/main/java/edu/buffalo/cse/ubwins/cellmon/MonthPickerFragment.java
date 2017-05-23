package edu.buffalo.cse.ubwins.cellmon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static android.R.attr.minDate;

/**
 * Created by pcoonan on 4/6/17.
 */

public class MonthPickerFragment extends DialogFragment {

    public final String TAG = "[CELNETMON-MONTHFRAG]";
    private static int selectedItem = 0;
    private static HashMap<Integer, String> monthMap = new HashMap<>();
    public final String[] months = {"January", "February", "March",
                                    "April", "May", "June",
                                    "July", "August", "September",
                                    "October", "November", "December"};

    public static MonthPickerFragment newInstance(long mindate){
        MonthPickerFragment fragment = new MonthPickerFragment();

        Bundle args = new Bundle();
        args.putLong("mindate", mindate);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Long mindate = getArguments().getLong("mindate");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Select month");

        Calendar minDate = Calendar.getInstance();
        minDate.setTimeInMillis(mindate);

        // Add a month to have the first full month of data
        minDate.add(Calendar.MONTH, 1);
        minDate.set(Calendar.DAY_OF_MONTH, 1);

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DATE, -2);
        maxDate.add(Calendar.MONTH, -1);
        ArrayList<String> monthList = new ArrayList<String>();

        int month = maxDate.get(Calendar.MONTH);
        int year = maxDate.get(Calendar.YEAR);
        int i = 0;
//        for(int i = 0; i < 24; ++i){
        while(maxDate.after(minDate)){
            monthList.add(months[month] + " " + year);
            monthMap.put(i, month + "/01/" + year);
            i++;
            maxDate.add(Calendar.MONTH, -1);

            month = maxDate.get(Calendar.MONTH);
            year = maxDate.get(Calendar.YEAR);
        }

        final CharSequence[] finalMonths = new CharSequence[monthList.size()];
        int index = 0;
        for(String mon : monthList){
            finalMonths[index++] = mon;
        }
        alertDialogBuilder.setSingleChoiceItems(finalMonths, selectedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedItem = which;
                sendBackResult();
            }
        });
        return alertDialogBuilder.create();
    }

    public void sendBackResult(){
        DateSelectedListener listener = (DateSelectedListener) getTargetFragment();
        listener.onFinishSelect("month", monthMap.get(selectedItem));
        dismiss();
    }
}
