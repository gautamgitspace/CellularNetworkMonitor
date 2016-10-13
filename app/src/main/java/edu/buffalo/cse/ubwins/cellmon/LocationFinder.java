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
    public static String locationProvider;

    private static final long distance = 10;
    private static final long updateInterval = 30000;
    boolean isGPSEnabled = false;
    static final String TAG = "[CELNETMON-LOCFINDER]";
    protected LocationManager locationManager;



    public LocationFinder(Context context)
    {
        this.mContext = context;
        //Log.v(TAG,"Context Constructor Fired.");
    }

    public void getLocation()
    {

        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            try
            {
                //Log.v(TAG, "GPS Based Location Services are ENABLED");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateInterval, distance, this);
            }
            catch (SecurityException s)
            {
                s.printStackTrace();
            }
        } else {

            try {
                //Log.v(TAG, "trying to get location from Wi-Fi or Cellular Towers");
                locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateInterval, distance, this);

            }
            catch (SecurityException s)
            {
                s.printStackTrace();
            }


        }
    }

    /* METHOD TO RESOLVE GEO COORDINATES
    public void addressResolver(Location location)
    {

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        geocoder = new Geocoder(mContext);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        try {
            if (isConnected)
            {
                Log.v(TAG, "Attempting to resolve address");
                List<Address> locationList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (locationList.get(0).getLocality() != null)
                {
                    locality = locationList.get(0).getLocality();
                    Log.v(TAG, "[LOCALITY]" + locality);
                }
                if (locationList.get(0).getAdminArea() != null)
                {
                    adminArea = locationList.get(0).getAdminArea();
                    Log.v(TAG, "[ADMIN AREA]" + adminArea);
                }
                if (locationList.get(0).getCountryName() != null)
                {
                    countryCode = locationList.get(0).getCountryName();
                    Log.v(TAG, "[COUNTRY]" + countryCode);
                }
                if (locationList.get(0).getThoroughfare() != null)
                {
                    throughFare = locationList.get(0).getThoroughfare();
                    Log.v(TAG, "[THROUGH FARE]" + throughFare);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    */

    @Override
    public void onLocationChanged(Location location)
    {
        //Log.i(TAG, "onLocationChanged for Location Manager: ");

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        locationProvider=location.getProvider();
        //Log.i(TAG, "onLocationChanged: latitude is " +latitude);
        //Log.i(TAG, "onLocationChanged: Longitude is  "+longitude);
        //Log.i(TAG, "onLocationChanged: Location provider is"+ locationProvider);

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
