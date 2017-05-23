package edu.buffalo.cse.ubwins.cellmon;

import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static edu.buffalo.cse.ubwins.cellmon.R.id.textView;

/**
 * Created by pcoonan on 3/15/17.
 */

public class HomeFragment extends Fragment implements View.OnClickListener {

    public final String TAG = "[CELNETMON-HOMEFRAG]";

    Button startTrackingButton;
    Button stopTrackingButton;
    SharedPreferences preferences;
    String URL_UPLOAD = "http://104.196.177.7:80/aggregator/upload/";
    String responsePhrase;
    String statusPhraseLogger;
    String recordsPhraseLogger;
    String IMEI_TO_POST;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        Log.d(TAG, "Creating Home Fragment view.");
        View view = inflater.inflate(R.layout.activity_main, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        startTrackingButton = (Button) view.findViewById(R.id.button4);
        stopTrackingButton = (Button) view.findViewById(R.id.button5);

        if(isMyServiceRunning(ForegroundService.class)){
            startTrackingButton.setEnabled(false);
            stopTrackingButton.setEnabled(true);
        }
        else{
            startTrackingButton.setEnabled(true);
            stopTrackingButton.setEnabled(false);
        }

        boolean isRegistered = preferences.getBoolean("isRegistered", false);
        TextView textView = (TextView) view.findViewById(R.id.textView30);

        if(isRegistered)
        {
            textView.setText("Device Already Registered!");
        }
        else
        {
            textView.setText("Device registration failed!");
        }

        startTrackingButton.setOnClickListener(this);
        stopTrackingButton.setOnClickListener(this);

//        Button forceUpload = (Button) view.findViewById(R.id.force_upload);
//        forceUpload.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Force Upload pressed");
//                new ForceExportTask().execute(URL_UPLOAD);
//            }
//        });
//
//        Button forcePing = (Button) view.findViewById(R.id.ping);
//        forcePing.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new ForcePingTask().execute();
//            }
//        });
//
//        Button mindate = (Button) view.findViewById(R.id.mindate);
//        mindate.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new MinDateTask().execute();
//            }
//        });
        return view;
    }

    @Override
    public void onClick(View v) {
        SharedPreferences.Editor editor = preferences.edit();
        Log.e(TAG, "inside on click");
        switch (v.getId()) {
            case R.id.button4:
                Intent startIntent = new Intent(getActivity(), ForegroundService.class);
                startIntent.setAction("startforeground");
                getActivity().startService(startIntent);
                editor.putBoolean("TRACKING", true);
                editor.commit();
                stopTrackingButton.setEnabled(true);
                startTrackingButton.setEnabled(false);
                break;
            case R.id.button5:
                if(isMyServiceRunning(ForegroundService.class))
                {
                    Log.v("STOP Button","Stopped");
                    Intent stopIntent = new Intent(getActivity(), ForegroundService.class);
                    stopIntent.setAction("stopforeground");
                    getActivity().startService(stopIntent);}
                startTrackingButton.setEnabled(true);
                stopTrackingButton.setEnabled(false);
                editor.putBoolean("TRACKING", false);
                editor.commit();
                break;
            default:
                break;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.home_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    class ForcePingTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            String IMEI_HASH = "";
            String responseStr = "";
            try {
             /*HASH IMEI*/
                IMEI_HASH = genHash(getIMEI());
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
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseStr;
        }
    }

    class MinDateTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            String IMEI_HASH = "";
            String responseStr = "";
            try {
             /*HASH IMEI*/
                IMEI_HASH = genHash(getIMEI());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "GENERATED IMEI HASH");
            //TODO KEEP-ALIVE GET
            HttpResponse response = null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                String customURL = "http://104.196.177.7/aggregator/mindate?imei_hash="
                        + URLEncoder.encode(IMEI_HASH, "UTF-8");
                Log.d(TAG, customURL);
                request.setURI(new URI(customURL));
                response = client.execute(request);
                Log.v(TAG, "RESPONSE PHRASE FOR HTTP GET: "
                        + response.getStatusLine().getReasonPhrase());
                Log.v(TAG, "RESPONSE STATUS FOR HTTP GET: "
                        + response.getStatusLine().getStatusCode());

                responseStr = EntityUtils.toString(response.getEntity());
                Log.v(TAG, "RESPONSE" + responseStr);

                /*PARSE JSON RESPONSE*/
                JSONObject jsonObject = new JSONObject(responseStr);
                String time = jsonObject.getString("timestamp");
//                statusPhraseLogger = jsonObject.getString("status");
                Calendar cal = Calendar.getInstance();
                TimeZone tz = cal.getTimeZone();

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                sdf.setTimeZone(tz);

                long timestamp = Long.parseLong(time);
                String parsed = sdf.format(new Date(timestamp));
                Log.d(TAG, "Mindate: " + parsed);

            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch(JSONException e){
                e.printStackTrace();
            }
            return responseStr;
        }
    }

    class ForceExportTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... urls){
            Log.v(TAG, "inside ForceExportTask");
            return FORCE_POST(urls[0]);
        }
    }

    public String FORCE_POST(String url)
    {
        String TEMP_TAG = "[CURSOR_DATA] : ";
        int statusCode;
        String result = "";

        Cursor cursor = fetchTop12000FromDB();
        int count = cursor.getCount();
        DataRecordOuterClass.DataRecord.Builder dataRecord =
                DataRecordOuterClass.DataRecord.newBuilder();
        DataRecordOuterClass.DataRecord recordToSend;


        if (cursor.moveToFirst())
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
                        .setFUSEDLAT(cursor.getDouble(1))
                        .setFUSEDLONG(cursor.getDouble(2))
                        .setSTALE(cursor.getInt(3) > 0)
                        .setTIMESTAMP(cursor.getLong(4))
                        .setNETWORKCELLTYPEValue(cursor.getInt(5))
                        .setNETWORKTYPEValue(cursor.getInt(6))
                        .setNETWORKPARAM1(cursor.getInt(7))
                        .setNETWORKPARAM2(cursor.getInt(8))
                        .setNETWORKPARAM3(cursor.getInt(9))
                        .setNETWORKPARAM4(cursor.getInt(10))
                        .setSIGNALDBM(cursor.getInt(11))
                        .setSIGNALLEVEL(cursor.getInt(12))
                        .setSIGNALASULEVEL(cursor.getInt(13))
                        .setNETWORKSTATEValue(cursor.getInt(14))
                        .setNETWORKDATAACTIVITYValue(cursor.getInt(15))
                        .setVOICECALLSTATEValue(cursor.getInt(16)).build());

                recordToSend = dataRecord.build();
            } while (cursor.moveToNext());

            byte[] logToSend = recordToSend.toByteArray();
            int len = logToSend.length;
            Log.e("SIZE","Length of 5 entries is : "+len);



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
                Log.v(TAG, "RESPONSE" + responsePhrase);

                /*PARSE JSON RESPONSE*/
                JSONObject jsonObject = new JSONObject(responsePhrase);
                recordsPhraseLogger = jsonObject.getString("records");
                statusPhraseLogger = jsonObject.getString("status");

//                Log.e(LOG_TAG, "STATUS: " + statusPhraseLogger);
//                Log.e(LOG_TAG, "RECORDS INSERTED: " + recordsPhraseLogger);

                /*DELETE FROM DB IF NO OF RECORDS FETCHED == NO OF RECORDS INSERTED*/
                if(Integer.parseInt(recordsPhraseLogger)==count)
                {
                    Log.e(TAG, "Attempting to delete from DB");
                    String rawQuery =
                            "DELETE FROM cellRecords WHERE ID IN " +
                                    "(SELECT ID FROM cellRecords ORDER BY TIMESTAMP LIMIT " +
                                    count + ");";
                    DBHandler dbHandler = new DBHandler(getActivity().getApplicationContext());
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
                    Log.v(TAG, "STATUS CODE: " + result);
                }
                else
                {
                    result = Integer.toString(statusCode);
                    Log.v(TAG, "STATUS CODE: " + result);
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

    private String getIMEI() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getNetworkOperatorCode() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperator();
    }

    private String getNetworkOperatorName() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperatorName();
    }

    private Cursor fetchTop12000FromDB()
    {
        String rawQuery = "SELECT * FROM cellRecords ORDER BY TIMESTAMP LIMIT 12000";
        DBHandler dbHandler = new DBHandler(getActivity().getApplicationContext());
        SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(rawQuery, null);
        return cursor;
    }
}
