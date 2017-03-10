/**
 * Created by Gautam on 6/18/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */

package edu.buffalo.cse.ubwins.cellmon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


public class LocationFinder extends Service implements LocationListener
{
    private final Context mContext;

    public static double latitude;
    public static double longitude;

    // We want instant results once we start listening so we can minimize the amount of time
    // the gps is on
    private static final long distance = 0;
    private static final long updateInterval = 0;
    boolean isGPSEnabled = false;
    public static boolean isGPSUpdated = false;
    static final String TAG = "[CELNETMON-LOCFINDER]";
    protected LocationManager locationManager;



    public LocationFinder(Context context)
    {
        this.mContext = context;
    }

    public void getLocation()
    {

        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled)
        {
            try
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateInterval, distance, this);
            }
            catch (SecurityException s)
            {
                s.printStackTrace();
            }
        }
    }

    public void stopUpdates(){
        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        isGPSUpdated = true;
        stopUpdates();
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        //Log.v(TAG,"inside Provider disabled");
        if (provider == LocationManager.GPS_PROVIDER){
            Log.v(TAG,"GPS Provider disabled by user");
            //can make toast here
        }
        else if(provider == LocationManager.NETWORK_PROVIDER){
            Log.v(TAG,"NETWORK Provider disabled by user");

            //can make toast here
        }
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        //Log.v(TAG,"inside Provider enabled");
        if (provider == LocationManager.GPS_PROVIDER){
            Log.v(TAG,"GPS Provider enabled by user");

            //can make toast here
        }
        else if(provider == LocationManager.NETWORK_PROVIDER){
            Log.v(TAG,"NETWORK Provider enabled by user");

            //can make toast here
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        //Log.v(TAG,"inside Provider status changed");
        if (provider == LocationManager.GPS_PROVIDER)
        {
            Log.v(TAG,"GPS Provider status changed");
            if(status == LocationProvider.OUT_OF_SERVICE){
                Log.v(TAG,"GPS Provider has gone out of service");
            }
            else if(status == LocationProvider.TEMPORARILY_UNAVAILABLE){
                Log.v(TAG,"GPS Provider is temporarily unavailable");
            }
            else if (status == LocationProvider.AVAILABLE){
                Log.v(TAG,"GPS Provider is available again");
            }

        }
        else if(provider == LocationManager.NETWORK_PROVIDER){
            Log.v(TAG,"Network Provider status changed");
            if(status == LocationProvider.OUT_OF_SERVICE){
                Log.v(TAG,"Network Provider has gone out of service");
            }
            else if(status == LocationProvider.TEMPORARILY_UNAVAILABLE){
                Log.v(TAG,"Network Provider is temporarily unavailable");
            }
            else if (status == LocationProvider.AVAILABLE){
                Log.v(TAG,"Network Provider is available again");
            }

        }
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

}
