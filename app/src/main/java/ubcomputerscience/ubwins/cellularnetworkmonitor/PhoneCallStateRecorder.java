package ubcomputerscience.ubwins.cellularnetworkmonitor;

import android.content.Context;
import android.util.Log;

import java.util.Date;

/**
 * Created by Gautam on 7/3/16.
 * MBP111.0138.B16
 * System Serial: C02P4SP9G3QH
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */
public class PhoneCallStateRecorder extends PhoneCallState
{
    static final String TAG = "[CELNETMON-PCSR]";
    @Override
    protected  void onIncomingCallStarted(Context ctx, String number, Date start)
    {

    }

    @Override
    protected  void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {

    }

    @Override
    protected  void onOutgoingCallStarted(Context ctx, String number, Date start)
    {

    }

    @Override
    protected  void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {

    }

    @Override
    protected  void onMissedCall(Context ctx, String number, Date start)
    {
        
    }
}
