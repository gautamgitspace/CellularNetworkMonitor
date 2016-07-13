/**
 * Created by Gautam on 6/18/16.
 * MBP111.0138.B16
 * agautam2@buffalo.edu
 * University at Buffalo, The State University of New York.
 * Copyright © 2016 Gautam. All rights reserved.
 */

package ubcomputerscience.ubwins.cellularnetworkmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;


public class DBstore
{
    static final String TAG = "[CELNETMON-DBSTORE]";
    private final Context mContext;
    LocationFinder locationFinder;
    String locality;
    String adminArea;
    String countryCode;
    String throughFare;
    String providerType;
    Double latitude;
    Double longitude;

    public DBstore(Context context)
    {
        this.mContext=context;
    }

    public void insertIntoDB(Location location, String timeStamp, String cellularInfo, String dataActivity, String dataState)
    {
        ContentValues contentValues = new ContentValues();
        DBHandler dbHandler = new DBHandler(mContext);
        SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();
        if(location!=null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        else
        {
            latitude=null;
            longitude=null;
        }

        locationFinder = new LocationFinder(mContext);
//        locationFinder.addressResolver(location);
//        locality=locationFinder.getLocality();
//        adminArea=locationFinder.getAdminArea();
//        countryCode=locationFinder.getCountryCode();
//        throughFare=locationFinder.getThroughFare();

        Log.v(TAG, "before split: " + cellularInfo);
        String[] splitter = cellularInfo.split("@");
        Log.v(TAG, "splitter of zero: "+splitter[0]);
        Log.v(TAG, "splitter of one: "+splitter[1]);
        String networkType = splitter[0];
        String splitter1[] = splitter[1].split("_");
        String networkState = splitter1[0];
        Log.v(TAG,"splitter1 of zero: " + splitter1[0]);
        Log.v(TAG,"splitter1 of one: " + splitter1[1]);
        String networkRSSI = splitter1[1];

        Log.v(TAG,"Trying to push to DB");
        contentValues.put("LAT",latitude);
        contentValues.put("LONG",longitude);
        contentValues.put("NETWORK_PROVIDER", providerType);
        contentValues.put("LOCALITY","");
        contentValues.put("CITY","");
        contentValues.put("STATE","");
        contentValues.put("COUNTRY","");
        contentValues.put("TIMESTAMP",timeStamp);
        contentValues.put("NETWORK_TYPE", networkType);
        contentValues.put("NETWORK_STATE", networkState);
        contentValues.put("NETWORK_RSSI", networkRSSI);
        contentValues.put("DATA_STATE",dataState);
        contentValues.put("DATA_ACTIVITY", dataActivity);

        sqLiteDatabase.insert("cellRecords", null, contentValues);
        sqLiteDatabase.close();
        Log.v(TAG,"Push to DB Successful");
    }

}
