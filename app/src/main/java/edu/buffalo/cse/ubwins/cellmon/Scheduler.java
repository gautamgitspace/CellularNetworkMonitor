/**
 * Created by Gautam on 7/26/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */

package edu.buffalo.cse.ubwins.cellmon;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;


public class Scheduler
{
    ScheduleIntentReceiver scheduleIntentReceiver = new ScheduleIntentReceiver();
    private static final String TAG = "[CELMON-SCHEDULER]";
    final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static ScheduledFuture<?> beeperHandle=null;

    public void beep(final Context context)
    {
        final Runnable beeper = new Runnable()
        {
            public void run()
            {
                //Log.v(LOG, "BEEP");
                try {
                    scheduleIntentReceiver.onScheduleIntentReceiver(context);
                }
                catch (Exception e)
                {
                    Log.e(TAG,"error in executing: It will no longer be run!: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        beeperHandle = scheduler.scheduleAtFixedRate(beeper, 0, 10, SECONDS);
    }
    
    public static void stopScheduler()
    {
        scheduler.schedule(new Runnable()
        {
            public void run()
            {
                beeperHandle.cancel(true);
            }
        }, 1, SECONDS);
    }
}
