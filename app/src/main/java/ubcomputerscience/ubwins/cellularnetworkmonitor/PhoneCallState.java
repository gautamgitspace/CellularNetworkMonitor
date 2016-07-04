package ubcomputerscience.ubwins.cellularnetworkmonitor;

/**
 * Created by Gautam on 7/3/16.
 * MBP111.0138.B16
 * System Serial: C02P4SP9G3QH
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneCallState extends BroadcastReceiver
{
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent)
    {
      if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL"))
      {
          savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
      }
      else
      {
          String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
          String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
          int state = 0;
          if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE))
          {
              state = TelephonyManager.CALL_STATE_IDLE;
          }
          else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
          {
              state = TelephonyManager.CALL_STATE_OFFHOOK;
          }
          else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING))
          {
              state = TelephonyManager.CALL_STATE_RINGING;
          }

          onCallStateChanged(context, state, number);
      }

    }

    //TODO Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start){}
    protected void onOutgoingCallStarted(Context ctx, String number, Date start){}
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end){}
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end){}
    protected void onMissedCall(Context ctx, String number, Date start){}


    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number)
    {
        if(lastState == state)
        {
            return;
        }
        switch (state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if(lastState != TelephonyManager.CALL_STATE_RINGING)
                {
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if(lastState == TelephonyManager.CALL_STATE_RINGING)
                {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                }
                else if(isIncoming)
                {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                else
                {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }

}
