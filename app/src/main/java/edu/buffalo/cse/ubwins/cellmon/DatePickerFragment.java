package edu.buffalo.cse.ubwins.cellmon;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.R.attr.max;
import static android.R.attr.minDate;
import static android.media.CamcorderProfile.get;
import static android.os.Build.VERSION_CODES.M;

/**
 * Created by pcoonan on 3/31/17.
 */

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    public final String TAG = "[CELNETMON-DATEFRAG]";
//    public static String selectedItem;
    public static Integer selectedYear;
    public static Integer selectedMonth;
    public static Integer selectedDay;

    public static DatePickerFragment newInstance(long mindate){
        DatePickerFragment fragment = new DatePickerFragment();

        Bundle args = new Bundle();
        args.putLong("mindate", mindate);
        fragment.setArguments(args);

        return fragment;
    }
        @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Long mindate = getArguments().getLong("mindate");
        final Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DATE, -2);

        int year = maxDate.get(Calendar.YEAR);
        int month = maxDate.get(Calendar.MONTH);
        int day = maxDate.get(Calendar.DAY_OF_MONTH);

        selectedYear = (selectedYear == null) ? year : selectedYear;
        selectedMonth = (selectedMonth == null) ? month : selectedMonth;
        selectedDay = (selectedDay == null) ? day : selectedDay;
        DatePickerDialog dpd = new DatePickerDialog(getActivity(),
                this, selectedYear, selectedMonth, selectedDay);

        dpd.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        dpd.getDatePicker().setMinDate(mindate);
        return dpd;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//        Log.d(TAG, "Date selected: DAY:" + dayOfMonth + " MONTH:" + monthOfYear + " YEAR:" + year);
        selectedDay = dayOfMonth;
        selectedMonth = monthOfYear;
        selectedYear = year;
        sendBackResult();
    }

    public void sendBackResult(){
        DateSelectedListener listener = (DateSelectedListener) getTargetFragment();
        // Need to add one to month index for conversion back to millis
        listener.onFinishSelect("day", (selectedMonth+1) + "/" + selectedDay + "/" + selectedYear);
        dismiss();
    }
}
