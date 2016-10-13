package edu.buffalo.cse.ubwins.cellmon;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;

public class ForegroundService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener
{
    private static final String LOG_TAG = "ForegroundService";

    ScheduleIntentReceiver scheduleIntentReceiver;
    Scheduler scheduler;
    private GoogleApiClient mGoogleApiClient;
    public LocationRequest mLocationRequest;
    public static Double FusedApiLatitude;
    public static Double FusedApiLongitude;
    LocationFinder locationFinder;
    PowerManager.WakeLock wakeLock;
    SharedPreferences preferences;
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;
    String URL_UPLOAD = "http://104.196.177.7:8000/aggregator/upload/";
    String responsePhrase;
    String statusPhraseLogger;
    String recordsPhraseLogger;
    String IMEI_TO_POST;




    @Override
    public void onCreate()
    {
        super.onCreate();
        buildGoogleApiClient();
        scheduleIntentReceiver = new ScheduleIntentReceiver();
        scheduler = new Scheduler();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);

        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent.getAction().equals("startforeground"))
        {
            Log.i(LOG_TAG, "Received Start Foreground Intent ");
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction("mainAction");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("CellularNetworkMonitor is running")
                    .setSmallIcon(R.mipmap.m)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
            startForeground(101,
                    notification);

            /*ACQUIRING WAKELOCK*/
            PowerManager mgr = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
            wakeLock.acquire();
            Log.v(LOG_TAG, "Acquired WakeLock");

            mGoogleApiClient.connect();
            //finished connecting API Client
            locationFinder = new LocationFinder(getApplicationContext());
            //calling getLocation() from Location provider
            locationFinder.getLocation();

            /*CALL TO SCHEDULER METHOD*/
            scheduler.beep(getApplicationContext());
            //Log.v(LOG_TAG, "SCHEDULER SET TO BEEP Every second");
            Toast.makeText(getApplicationContext(), "Tracking set to ON!", Toast.LENGTH_SHORT).show();

        }
        else if (intent.getAction().equals("stopforeground"))
        {
            //Log.i(LOG_TAG, "Received Stop Foreground Intent");

            /*CANCEL SCHEDULER AND RELEASE WAKELOCK*/
            if(wakeLock.isHeld()) {
                wakeLock.release();
            }
            //Log.v(LOG_TAG, "Releasing WakeLock");

            scheduler.stopScheduler();

            //Log.v(LOG_TAG, "Beeping Service Stoppped");

            /*to disconnect google api client*/
            if(mGoogleApiClient.isConnected())
            {
                mGoogleApiClient.disconnect();
            }
            stopForeground(true);
            stopSelf();
            Toast.makeText(getApplicationContext(), "Tracking set to OFF!", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.i(LOG_TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location){
        //Log.i(LOG_TAG,"Location data has changed");
        FusedApiLatitude = location.getLatitude();
        FusedApiLongitude = location.getLongitude();
        //Log.i(LOG_TAG,"apiLat is : "+FusedApiLatitude);
        //Log.i(LOG_TAG,"apiLong  is : "+FusedApiLongitude);

    }

    @Override
    public void onConnectionSuspended(int i){
        Log.i(LOG_TAG,"Google Api client has been suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.i(LOG_TAG,"Google Api client connection has failed");
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        boolean chargingtrue = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("android.intent.action.ACTION_POWER_CONNECTED")){
                //action for Charging connected
                Log.e("FS","Charging");
                Handler handler = new Handler(Looper.getMainLooper());

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        //Toast.makeText(ForegroundService.this.getApplicationContext(),"Charging",Toast.LENGTH_SHORT).show();
                    }
                });

                //boolean isRegistered = preferences.getBoolean("isRegistered", false);
                //now check for Wifi
                chargingtrue = true;
                int count = 0;

                /*UPLOAD 50 HOURS OF DATA IN ONE GO*/
                while (chargingtrue && count <= 15)
                {
                    //Log.e("FS","Charging: inside while loop");
                    int status = getConnectivityStatus(getApplicationContext());
                    if(status == TYPE_WIFI)
                    {
                        String res = onFetchClicked();
                        if(res.equals("DB_EMPTY")||res.equals("Data not stale enough"))
                        {
                            //Log.e("FS","Charging: breaking the loop");

                            break;

                        }

                        count += 1;
                    }
                    else if(status == TYPE_MOBILE || status == TYPE_NOT_CONNECTED){
                        //wifi disconnected
                        //Stop uploading here
                        break;
                    }
                }
            }
            else if(action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")){
                //action for Charging disconnected
                Log.e("FS","Not Charging");
                chargingtrue =false;


            }

        }

    };

    public static int getConnectivityStatus(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork)
        {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public String onFetchClicked()
    {
        boolean isConnected = isConnected();
        String res = "";
        if(isConnected)
        {
            //Log.v(TAG, "isConnected = TRUE");
            try {
                res = new LogAsyncTask().execute(URL_UPLOAD).get();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Log.v(LOG_TAG, "isConnected = FALSE");
            Toast.makeText(getBaseContext(), "Device has no Internet Connectivity! Please check your Network Connection and try again", Toast.LENGTH_LONG).show();
        }
        return res;
    }

    private class LogAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... urls)
        {
            Log.v(LOG_TAG, "inside LogAsyncTask");
            return LOG_POST(urls[0]);
        }
        @Override
        protected void onPostExecute(String result)
        {
            //Toast.makeText(getBaseContext(), "Attempt to POST made!", Toast.LENGTH_LONG).show();
        }
    }

    public String LOG_POST(String url)
    {
        String TEMP_TAG = "[CURSOR_DATA] : ";
        int statusCode;
        String result = "";

        Cursor cursor = fetchTop100FromDB();
        int count = cursor.getCount();
        DataRecordOuterClass.DataRecord.Builder dataRecord = DataRecordOuterClass.DataRecord.newBuilder();
        DataRecordOuterClass.DataRecord recordToSend;
        boolean uploadflag = true;

        if(cursor.moveToFirst()) {
            String timeGap = DateUtils.getRelativeTimeSpanString(cursor.getLong(6), System.currentTimeMillis(), HOUR_IN_MILLIS).toString();
            //Log.e("TIMEGAP", timeGap);
            String timeHoursarray[] = timeGap.split(" ");
            int hours = Integer.parseInt(timeHoursarray[0]);
            //Log.e("TIMEGAP", "Gap in hours " + hours);
            if(hours <= 1){
                uploadflag = false;
                result = "Data not stale enough";
            }

        }

        if (cursor.moveToFirst() && uploadflag==true)
        {
            String IMEI = getIMEI();
            String networkOperatorCode = getNetworkOperatorCode();
            String networkOperatorName = getNetworkOperatorName();

            try{
                IMEI_TO_POST = genHash(IMEI);
            }
            catch(NoSuchAlgorithmException nsa)
            {
                nsa.printStackTrace();
            }
            dataRecord.setIMEIHASH(IMEI_TO_POST);
            dataRecord.setNETWORKOPERATORNAME(networkOperatorName);
            dataRecord.setNETWORKOPERATORCODE(networkOperatorCode);

            do {
                dataRecord.addENTRY(DataRecordOuterClass.DataEntry.newBuilder()
                        .setNETWORKLAT(cursor.getDouble(1))
                        .setNETWORKLONG(cursor.getDouble(2))
                        .setFUSEDLAT(cursor.getDouble(3))
                        .setFUSEDLONG(cursor.getDouble(4))
                        .setLOCATIONPROVIDERValue(cursor.getInt(5))
                        .setTIMESTAMP(cursor.getLong(6))
                        .setNETWORKCELLTYPEValue(cursor.getInt(7))
                        .setNETWORKTYPEValue(cursor.getInt(8))
                        .setNETWORKPARAM1(cursor.getInt(9))
                        .setNETWORKPARAM2(cursor.getInt(10))
                        .setNETWORKPARAM3(cursor.getInt(11))
                        .setNETWORKPARAM4(cursor.getInt(12))
                        .setSIGNALDBM(cursor.getInt(13))
                        .setSIGNALLEVEL(cursor.getInt(14))
                        .setSIGNALASULEVEL(cursor.getInt(15))
                        .setNETWORKSTATEValue(cursor.getInt(16))
                        .setNETWORKDATAACTIVITYValue(cursor.getInt(17))
                        .setVOICECALLSTATEValue(cursor.getInt(18)).build());

                recordToSend = dataRecord.build();
//
//                Log.e(TEMP_TAG, "[N_LAT] : " + cursor.getString(1));
//                Log.e(TEMP_TAG, "[N_LONG] : " + cursor.getString(2));
//                Log.e(TEMP_TAG, "[F_LAT] : " + cursor.getString(3));
//                Log.e(TEMP_TAG, "[F_LONG] : " + cursor.getString(4));
//                Log.e(TEMP_TAG, "[LOCATION_PROVIDER] : " + cursor.getString(5));
//                Log.e(TEMP_TAG, "[TIMESTAMP] : " + cursor.getString(6));
//                Log.e(TEMP_TAG, "[NETWORK_TYPE] : " + cursor.getString(7));
//                Log.e(TEMP_TAG, "[NETWORK_TYPE2] : " + cursor.getString(8));
//                Log.e(TEMP_TAG, "[NETWORK_PARAM1] : " + cursor.getString(9));
//                Log.e(TEMP_TAG, "[NETWORK_PARAM2] : " + cursor.getString(10));
//                Log.e(TEMP_TAG, "[NETWORK_PARAM3] : " + cursor.getString(11));
//                Log.e(TEMP_TAG, "[NETWORK_PARAM4] : " + cursor.getString(12));
//                Log.e(TEMP_TAG, "[DBM] : " + cursor.getString(13));
//                Log.e(TEMP_TAG, "[NETWORK_LEVEL] : " + cursor.getString(14));
//                Log.e(TEMP_TAG, "[ASU_LEVEL] : " + cursor.getString(15));
//                Log.e(TEMP_TAG, "[DATA_STATE] : " + cursor.getString(16));
//                Log.e(TEMP_TAG, "[DATA_ACTIVITY] : " + cursor.getString(17));
//                Log.e(TEMP_TAG, "[CALL_STATE] : " + cursor.getString(18));
//                Log.e(TEMP_TAG, "---------------------------------------------------");

            } while (cursor.moveToNext());

            byte[] logToSend = recordToSend.toByteArray();
            int len = logToSend.length;
            Log.e("SIZE","Length of the entries is : "+len);



            try {

                    /*1. create HttpClient*/
                HttpClient httpclient = new DefaultHttpClient();

                    /*2. make POST request to the given URL*/
                HttpPost httpPost = new HttpPost(url);

                    /*3. Build ByteArrayEntity*/
                ByteArrayEntity byteArrayEntity = new ByteArrayEntity(logToSend);

                    /*4. Set httpPost Entity*/
                httpPost.setEntity(byteArrayEntity);

                    /*5. Execute POST request to the given URL*/
                HttpResponse httpResponse = httpclient.execute(httpPost);

                /*9. receive response as inputStream*/
                statusCode = httpResponse.getStatusLine().getStatusCode();

                /*CONVERT INPUT STREAM TO STRING*/
                responsePhrase = EntityUtils.toString(httpResponse.getEntity());
                Log.v(LOG_TAG, "RESPONSE" + responsePhrase);

                /*PARSE JSON RESPONSE*/
                JSONObject jsonObject = new JSONObject(responsePhrase);
                recordsPhraseLogger = jsonObject.getString("records");
                statusPhraseLogger = jsonObject.getString("status");

//                Log.e(LOG_TAG, "STATUS: " + statusPhraseLogger);
//                Log.e(LOG_TAG, "RECORDS INSERTED: " + recordsPhraseLogger);

                /*DELETE FROM DB IF NO OF RECORDS FETCHED == NO OF RECORDS INSERTED*/
                if(Integer.parseInt(recordsPhraseLogger)==count)
                {
                    Log.e(LOG_TAG, "Attempting to delete from DB");
                    String rawQuery = "DELETE FROM cellRecords WHERE rowid IN (SELECT rowid FROM cellRecords LIMIT "+count+");";
                    DBHandler dbHandler = new DBHandler(getApplicationContext());
                    SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();
                    sqLiteDatabase.beginTransaction();
                    sqLiteDatabase.execSQL(rawQuery);
                    sqLiteDatabase.setTransactionSuccessful();
                    sqLiteDatabase.endTransaction();
                    sqLiteDatabase.close();
                }


                if (statusCode != 404)
                {
                    result = Integer.toString(statusCode);
                    Log.v(LOG_TAG, "STATUS CODE: " + result);
                }
                else
                {
                    result = Integer.toString(statusCode);
                    Log.v(LOG_TAG, "STATUS CODE: " + result);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Log.e(TEMP_TAG, "DB IS BROKE AS HELL!");
            result = "DB_EMPTY";
        }
        return result;
    }
    private String getIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getNetworkOperatorCode() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperator();
    }

    private String getNetworkOperatorName() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperatorName();
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
    private Cursor fetchTop100FromDB()
    {
        String rawQuery = "SELECT * FROM cellRecords LIMIT 12000";
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(rawQuery, null);
        return cursor;
    }

    public boolean isConnected()
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
}