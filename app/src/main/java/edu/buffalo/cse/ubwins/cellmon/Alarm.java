package edu.buffalo.cse.ubwins.cellmon;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

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

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by pcoonan on 4/12/17.
 */

public class Alarm extends BroadcastReceiver {
    CellularDataRecorder cdr;
    PhoneCallStateRecorder pcsr;
    DBstore dbStore;
    public final String TAG = "[CELNETMON-ALARM]";
    static int keepAlive = 0;
    String IMEI_HASH;
    String IMEI;
    public static long lastTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
//        long cur = System.currentTimeMillis();
//        Log.d(TAG, "Received alarm trigger since " + ((cur - lastTime)/1000.0));
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
//        wakeLock.acquire();

        logData(context);
        setAlarm(context);
//        wakeLock.release();
    }

    @TargetApi(19)
    public void setAlarm(Context context){
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+10000, pi);
        lastTime = System.currentTimeMillis();
//        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (10000 * offset), 60000, pi);
//        Log.d(TAG, "Alarm set!");
    }

    public void cancelAlarm(Context context){
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.d(TAG, "Alarm cancelled!");
    }

    private void logData(Context arg0) {
        keepAlive++;

        final TelephonyManager telephonyManager =
                (TelephonyManager) arg0.getSystemService(Context.TELEPHONY_SERVICE);
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

        final LocationManager locationManager = (LocationManager) arg0.getSystemService(LOCATION_SERVICE);

        if (ForegroundService.FusedApiLatitude == null || ForegroundService.FusedApiLongitude == null) {
            return;
        }

     /*FETCH INFO FROM FUSED API*/
        Double fusedApiLatitude = ForegroundService.FusedApiLatitude;
        Double fusedApiLongitude = ForegroundService.FusedApiLongitude;
        boolean stale = ((System.currentTimeMillis() - ForegroundService.LastFusedLocation) > 40000)
                && dataState == 0 && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Double locationdata[] = {fusedApiLatitude, fusedApiLongitude};


     /*FETCH INFO FROM PCSR CLASS*/
        int phoneCallState = PhoneCallStateRecorder.call_state;

        dbStore = new DBstore(arg0);
        dbStore.insertIntoDB(locationdata, stale, timeStamp, cellularInfo, dataActivity, dataState,
                phoneCallState, mobileNetworkType);

        if (keepAlive == 360) {
            new PingTask().execute();
            keepAlive = 0;
        }
    }

    class PingTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String IMEI_HASH = "";
            String responseStr = "";
            try {
             /*HASH IMEI*/
                IMEI_HASH = genHash(IMEI);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "GENERATED IMEI HASH");
            //TODO KEEP-ALIVE GET
            HttpResponse response = null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                String customURL = "http://104.196.177.7/aggregator/ping?imei_hash="
                        + URLEncoder.encode(IMEI_HASH, "UTF-8");
                Log.d(TAG, customURL);
                request.setURI(new URI(customURL));
                response = client.execute(request);
                Log.v(TAG, "RESPONSE PHRASE FOR HTTP GET: "
                        + response.getStatusLine().getReasonPhrase());
                Log.v(TAG, "RESPONSE STATUS FOR HTTP GET: "
                        + response.getStatusLine().getStatusCode());
                responseStr = response.getStatusLine().getReasonPhrase();
            } catch (URISyntaxException|IOException e) {
                e.printStackTrace();
                FirebaseCrash.log("Error in ping task for: " + IMEI_HASH);
            }
            return responseStr;
        }
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        String IMEI_Base64 = "";
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] sha256Hash = sha256.digest(input.getBytes("UTF-8"));
            IMEI_Base64 = Base64.encodeToString(sha256Hash, Base64.DEFAULT);
            IMEI_Base64 = IMEI_Base64.replaceAll("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IMEI_Base64;
    }


}
