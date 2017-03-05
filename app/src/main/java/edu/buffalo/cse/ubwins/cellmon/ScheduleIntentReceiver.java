package edu.buffalo.cse.ubwins.cellmon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import static edu.buffalo.cse.ubwins.cellmon.LocationFinder.locationProvider;

/**
 * Created by Gautam on 7/18/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */
public class ScheduleIntentReceiver extends Service
{
//    LocationFinder locationFinder;
    CellularDataRecorder cdr;
    PhoneCallStateRecorder pcsr;
    DBstore dbStore;
//    Location location;
    public final String TAG = "[CELNETMON-HNDLRCVR]";
    int keepAlive = 0;
    String IMEI_HASH;
    String IMEI;
 public void onScheduleIntentReceiver(Context arg0)
 {
     keepAlive++;

//     locationFinder = new LocationFinder(arg0);

     final TelephonyManager telephonyManager = (TelephonyManager) arg0.getSystemService(Context.TELEPHONY_SERVICE);
     IMEI = telephonyManager.getDeviceId();
     cdr = new CellularDataRecorder();
     pcsr = new PhoneCallStateRecorder();

//     locationFinder = new LocationFinder(arg0);
     //Log.v(TAG, "Calling getLocalTimeStamp and getCellularInfo");

     /*FETCH INFO FROM CDR CLASS*/
     Long timeStamp = cdr.getLocalTimeStamp();
     String cellularInfo = cdr.getCellularInfo(telephonyManager);
     int dataActivity = cdr.getCurrentDataActivity(telephonyManager);
     int dataState = cdr.getCurrentDataState(telephonyManager);
     int mobileNetworkType = cdr.getMobileNetworkType(telephonyManager);

     /*FETCH INFO FROM FUSED API*/
     Double fusedApiLatitude = ForegroundService.FusedApiLatitude;
     Double fusedApiLongitude = ForegroundService.FusedApiLongitude;

     /*FETCH INFO FROM LOCATION FINDER CLASS*/
//     Double lmLatitude = LocationFinder.latitude;
//     Double lmLongitude = LocationFinder.longitude;
//     String locationProvider = LocationFinder.locationProvider;
//     Double locationdata[] = {lmLatitude,lmLongitude,fusedApiLatitude,fusedApiLongitude};
     Double locationdata[] = {fusedApiLatitude,fusedApiLongitude};
     boolean stale = (System.currentTimeMillis() - ForegroundService.LastFusedLocation) > 10000;

     /*FETCH INFO FROM PCSR CLASS*/
     int phoneCallState = PhoneCallStateRecorder.call_state;
//     Log.i(TAG, "onReceive: Location data is before inserting "+locationdata[0] +" "+ locationdata[1]+" "+ locationdata[2]+" "+ locationdata[3]);
//
//
//     Log.v(TAG, "TIME STAMP: " + timeStamp);
//     Log.v(TAG, "CELLULAR INFO: " + cellularInfo);
//     Log.v(TAG, "DATA ACTIVITY: " + dataActivity);
//     Log.v(TAG, "DATA STATE: " + dataState);
//     Log.v(TAG, "MOBILE NETWORK TYPE: " + mobileNetworkType);

     dbStore = new DBstore(arg0);
     dbStore.insertIntoDB(locationdata, stale, timeStamp, cellularInfo, dataActivity, dataState, phoneCallState, mobileNetworkType);
//     Log.e(TAG, "KEEPALIVE: " + keepAlive);
     if(keepAlive == 1200)
     {
         /*GET IMEI*/
         try {
             /*HASH EMEI*/
             IMEI_HASH = genHash(IMEI);
         }

         catch(NoSuchAlgorithmException e)
         {
             e.printStackTrace();
         }
         Log.v(TAG, "GENERATED IMEI HASH");
         //TODO KEEP-ALIVE GET
         HttpResponse response = null;
         try
         {
             HttpClient client = new DefaultHttpClient();
             HttpGet request = new HttpGet();
             String customURL = "http://104.196.177.7/aggregator/ping?imei_hash=" + URLEncoder.encode(IMEI_HASH, "UTF-8");
             request.setURI(new URI(customURL));
             Log.d(TAG, "Prerequest time: " + System.currentTimeMillis());
             response = client.execute(request);
             Log.d(TAG, "Postrequest time: " + System.currentTimeMillis());
             Log.v(TAG, "RESPONSE PHRASE FOR HTTP GET: " + response.getStatusLine().getReasonPhrase());
             Log.v(TAG, "RESPONSE STATUS FOR HTTP GET: " + response.getStatusLine().getStatusCode());
         }
         catch (URISyntaxException e)
         {
             e.printStackTrace();
         }
         catch (ClientProtocolException e)
         {
             e.printStackTrace();
         }
         catch (IOException e)
         {
             e.printStackTrace();
         }
         //RESET KEEPALIVE
         keepAlive = 0;
         //Log.e(TAG, "KEEPALIVE RESET: " + keepAlive);
     }

 }

    private String genHash(String input) throws NoSuchAlgorithmException
    {
        String IMEI_Base64="";
        try
        {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] sha256Hash = sha256.digest(input.getBytes("UTF-8"));
            IMEI_Base64 = Base64.encodeToString(sha256Hash, Base64.DEFAULT);
            IMEI_Base64=IMEI_Base64.replaceAll("\n", "");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return IMEI_Base64;
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        // Used only in case of bound services.
        return null;
    }

}


