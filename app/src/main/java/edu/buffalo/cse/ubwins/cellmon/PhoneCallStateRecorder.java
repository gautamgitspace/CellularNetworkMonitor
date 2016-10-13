package edu.buffalo.cse.ubwins.cellmon;

import android.content.Context;

import java.util.Date;

/**
 * Created by Gautam on 7/3/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */
public class PhoneCallStateRecorder extends PhoneCallState
{
    public static  int call_state = 0;
    static final String TAG = "[CELNETMON-PCSR]";
    @Override
    protected  void onIncomingCallStarted(Context ctx, String number, Date start)
    {
        //Log.v(TAG, "Call Received from: " + number + " at: " + start);
        call_state = 1;
    }

    @Override
    protected  void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        //Log.v(TAG, "Call Received from: " + number + " at: " + start + " ended at: " + end);
        call_state = 0;
    }

    @Override
    protected  void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        //Log.v(TAG, "Outgoing call to: " + number + " started at: " + start);
        call_state = 1;
    }

    @Override
    protected  void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        //Log.v(TAG, "Outgoing call to: " + number + " started at: " + start + " ended at: " + end);
        call_state = 0;
    }

    @Override
    protected  void onMissedCall(Context ctx, String number, Date start)
    {
        //Log.v(TAG, "Missed call from: " + number + " at: " + start);
    }
}
