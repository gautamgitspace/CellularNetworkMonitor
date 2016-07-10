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

/**
 * Created by Gautam on 7/9/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */
public class AlarmReceiver extends BroadcastReceiver {
    LocationFinder locationFinder;
    CellularDataRecorder cdr;
    DBstore dbStore;
    Location location;
    MainActivity mainActivity;
    public final String TAG = "[CELNETMON-AlARMRCVR]";

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        Log.v(TAG, "inside onReceive");
        if (ActivityCompat.checkSelfPermission(arg0, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // LOCATION permission has not been granted.

            mainActivity.requestLocationPermission();

        } else {
            // LOCATION permission is already available.
            Log.v(TAG,
                    "LOCATION permission has already been granted.");
            //carry on
        }
        locationFinder = new LocationFinder(arg0);

        location = locationFinder.getLocationByNetwork();


        if (location != null)
        {
            if (ActivityCompat.checkSelfPermission(arg0, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                // phone permission has not been granted.
                mainActivity.requestPhonePermission();
            }
            else
            {
                // phone permissions is already available.
                Log.v(TAG, "Phone permission has already been granted.");
                //carry on
            }

            final TelephonyManager telephonyManager = (TelephonyManager) arg0.getSystemService(Context.TELEPHONY_SERVICE);
            cdr = new CellularDataRecorder();
            Log.v(TAG, "Calling getLocalTimeStamp and getCellularInfo");
            String timeStamp = cdr.getLocalTimeStamp();
            String cellularInfo = cdr.getCellularInfo(telephonyManager);
            String dataActivity = cdr.getCurrentDataActivity(telephonyManager);
            String dataState = cdr.getCurrentDataState(telephonyManager);
            String mobileNetworkType = cdr.getMobileNetworkType(telephonyManager);

            Log.v(TAG, "TIME STAMP: " + timeStamp);
            Log.v(TAG, "CELLULAR INFO: " + cellularInfo);
            Log.v(TAG, "DATA ACTIVITY: " + dataActivity);
            Log.v(TAG, "DATA STATE: " + dataState);
            Log.v(TAG, "MOBILE NETWORK TYPE: " + mobileNetworkType);

            dbStore = new DBstore(arg0);
            dbStore.insertIntoDB(location, timeStamp, cellularInfo, dataActivity, dataState);

//                locationFinder.addressResolver(location);
//                double latitude = location.getLatitude();
//                double longitude = location.getLongitude();
//                String countryCode = locationFinder.getCountryCode();
//                String adminArea = locationFinder.getAdminArea();
//                String locality = locationFinder.getLocality();
//                String throughFare = locationFinder.getThroughFare();
//
//                Toast.makeText(getApplicationContext(), "You are at - " + throughFare + ", " + locality + ", " + adminArea + ", " + countryCode + "\n" +
//                        "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_LONG).show();
        } else {
            Log.v(TAG, "Waiting to get location from NETWORK_PROVIDER");
            Toast.makeText(arg0, "Waiting to get location from the network", Toast.LENGTH_LONG).show();
        }
    }
}
