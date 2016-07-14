/**
 * Created by Gautam on 7/9/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */
package ubcomputerscience.ubwins.cellularnetworkmonitor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


public class AlarmReceiver extends BroadcastReceiver
{
    LocationFinder locationFinder;
    CellularDataRecorder cdr;
    DBstore dbStore;
    Location location;
    MainActivity mainActivity;
    public final String TAG = "[CELNETMON-AlARMRCVR]";

    @Override
    public void onReceive(Context arg0, Intent arg1)
    {
        Log.v(TAG, "inside onReceive");
        locationFinder = new LocationFinder(arg0);

        location = locationFinder.getLocation();
        if(location==null)
        {
            Log.v(TAG, "Location object returned null");
        }
            final TelephonyManager telephonyManager = (TelephonyManager) arg0.getSystemService(Context.TELEPHONY_SERVICE);
            cdr = new CellularDataRecorder();
            Log.v(TAG, "Calling getLocalTimeStamp and getCellularInfo");
            String timeStamp = cdr.getLocalTimeStamp();
            String cellularInfo = cdr.getCellularInfo(telephonyManager);
            String dataActivity = cdr.getCurrentDataActivity(telephonyManager);
            String dataState = cdr.getCurrentDataState(telephonyManager);
            String mobileNetworkType = cdr.getMobileNetworkType(telephonyManager);
            String fusedApiLatitude = mainActivity.FusedApiLatitude;
            String fusedApiLongitude = mainActivity.FusedApiLongitude;

            Log.v(TAG, "TIME STAMP: " + timeStamp);
            Log.v(TAG, "CELLULAR INFO: " + cellularInfo);
            Log.v(TAG, "DATA ACTIVITY: " + dataActivity);
            Log.v(TAG, "DATA STATE: " + dataState);
            Log.v(TAG, "MOBILE NETWORK TYPE: " + mobileNetworkType);

            dbStore = new DBstore(arg0);
            dbStore.insertIntoDB(location, timeStamp, cellularInfo, dataActivity, dataState);

//
    }
}
