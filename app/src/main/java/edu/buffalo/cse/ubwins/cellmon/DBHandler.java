/**
 *   Created by Gautam on 6/18/16.
 *   MBP111.0138.B16
 *   agautam2@buffalo.edu
 *   University at Buffalo, The State University of New York.
 *   Copyright © 2016 Gautam. All rights reserved.
 */
package edu.buffalo.cse.ubwins.cellmon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*Understanding of SQLite for Android -  http://developer.android.com/training/basics/data-storage/databases.html */

public class DBHandler extends SQLiteOpenHelper
{
    private static  String dbName="mainTuple";
    private static  String dbNameBatt="mainTuple";
    private static int version=2;
    static final String TAG = "[CELNETMON-DBHANDLER]";

    private static final String schema = "CREATE TABLE cellRecords (ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, F_LAT DATA, F_LONG DATA, F_STALE DATA, TIMESTAMP DATA, NETWORK_TYPE DATA, NETWORK_TYPE2 DATA, NETWORK_PARAM1 DATA, NETWORK_PARAM2 DATA, NETWORK_PARAM3 DATA, NETWORK_PARAM4 DATA, DBM DATA, NETWORK_LEVEL DATA,ASU_LEVEL DATA, DATA_STATE DATA, DATA_ACTIVITY DATA, CALL_STATE DATA)";
    private static final String schemeBatteryStatus = "CREATE TABLE batteryStatus (TIMESTAMP DATA, BATTERY_LEVEL DATA)";
    public DBHandler(Context context)

    {
        super(context, dbName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(schema);
        Log.v(TAG, "CREATING DB " + dbName + "WITH TABLE cellRecords");
        db.execSQL(schemeBatteryStatus);
        Log.v(TAG, "CREATING DB " + dbNameBatt + "WITH TABLE batteryStatus");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int obsolete, int latest)
    {
        //logic understood from - http://stackoverflow.com/questions/3675032/drop-existing-table-in-sqlite-when-if-exists-operator-is-not-supported
        db.execSQL("DROP TABLE IF EXISTS cellRecords");
        db.execSQL("DROP TABLE IF EXISTS batteryStatus");
        onCreate(db);
    }

}
