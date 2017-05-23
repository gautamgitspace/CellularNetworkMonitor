package edu.buffalo.cse.ubwins.cellmon;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;



/**
 * Created by pcoonan on 4/3/17.
 */

public class WeekPickerFragment extends DialogFragment {
//    private EditText mEditText;
    private static int selectedItem = 0;
    private static HashMap<Integer, String> weekMap = new HashMap<>();
    public final String TAG = "[CELNETMON-WEEKFRAG]";

    public static WeekPickerFragment newInstance(long mindate){
        WeekPickerFragment fragment = new WeekPickerFragment();

        Bundle args = new Bundle();
        args.putLong("mindate", mindate);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Long mindate = getArguments().getLong("mindate");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Select week");

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DATE, -7);

        Calendar minDate = Calendar.getInstance();
        minDate.setTimeInMillis(mindate);

        // Get start of week where data was first recorded
        minDate.add(Calendar.DAY_OF_WEEK, minDate.getFirstDayOfWeek() - minDate.get(Calendar.DAY_OF_WEEK));
        // Add a week to have the first full week of data
        minDate.add(Calendar.DAY_OF_YEAR, 7);


        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        Calendar beginWeek = (Calendar) maxDate.clone();
        ArrayList<String> weeks = new ArrayList<String>();
        beginWeek.add(Calendar.DAY_OF_WEEK, beginWeek.getFirstDayOfWeek() - beginWeek.get(Calendar.DAY_OF_WEEK));
        Calendar endWeek = (Calendar) beginWeek.clone();
        endWeek.add(Calendar.DAY_OF_WEEK, 6);

        int day = beginWeek.get(Calendar.DAY_OF_MONTH);
        int month = beginWeek.get(Calendar.MONTH);
        int year = beginWeek.get(Calendar.YEAR);
//        for(int i = 0; i < 104; ++i){
        int i = 0;
        while(beginWeek.after(minDate)){
            weeks.add(df.format(beginWeek.getTime()) + "-" + df.format(endWeek.getTime()));
            weekMap.put(i, month + "/" + day + "/" + year);
            i++;
            beginWeek.add(Calendar.DAY_OF_YEAR, -7);
            endWeek.add(Calendar.DAY_OF_YEAR, -7);

            day = beginWeek.get(Calendar.DAY_OF_MONTH);
            month = beginWeek.get(Calendar.MONTH);
            year = beginWeek.get(Calendar.YEAR);
        }

        final CharSequence[] finalWeeks = new CharSequence[weeks.size()];
        int index = 0;
        for(String week : weeks){
            finalWeeks[index++] = week;
        }
        alertDialogBuilder.setSingleChoiceItems(finalWeeks, selectedItem, new DialogInterface.OnClickListener() {
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
        listener.onFinishSelect("week", weekMap.get(selectedItem));
        dismiss();
    }
}
