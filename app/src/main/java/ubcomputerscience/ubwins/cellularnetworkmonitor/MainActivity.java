/**
 *   Created by Gautam on 6/18/16.
 *   MBP111.0138.B16
 *   agautam2@buffalo.edu
 *   University at Buffalo, The State University of New York.
 *   Copyright © 2016 Gautam. All rights reserved.
 *
 *   CelNetMon v1.0 ~ gets location pure network based location does not fail to GPS
 *   CelNetMon v1.1 ~ registers device uses a JSON POST
 *   CelNetMon v1.2 ~ with user permissions for android v6.0+, records DataActivity and DataSate, logs call state
 *   CelNetMon v1.2.1 ~ with alarm and periodic recording on 60 secs.
 *   CelNetMon v1.2.2 ~ Permissions handled on onCreate in MainActivity. GPS functionality included. Records data even when location object returns null.
 *   //TODO - check listeners for wifi = connected and charging = true
 *   //TODO - explore MessagePack
 */

package ubcomputerscience.ubwins.cellularnetworkmonitor;

import android.Manifest;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import android.support.annotation.NonNull;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.Toast;
import android.support.design.widget.Snackbar;



import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback
{

    public final String TAG = "[CELNETMON-ACTIVITY]";
    Button track;
    private View mLayout;
    private static final int REQUEST_LOCATION = 0;
    private static final int REQUEST_STORAGE = 2;
    private static final int REQUEST_PHONE = 1;

    Handler h = null;
    public static String FusedApiLatitude;
    public static String FusedApiLongitude;

    //Exports SQLiteDB to CSV file in Phone Storage


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Creating googleApiClient for Fused Location Provider

        mLayout = findViewById(R.id.myLayout);
        //Ask for permissions here itself(both for final app and one write to storage for generating CSV file)
        //First, read location permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // LOCATION permission has not been granted.

            requestLocationPermission();

        }
        else
        {
            // LOCATION permission is already available.
            Log.v(TAG, "LOCATION permission has already been granted.");
            //carry on
        }

        //Second read phone state permission

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
        {
            // phone permission has not been granted.
            requestPhonePermission();
        }
        else
        {
            // phone permissions is already available.
            Log.v(TAG, "Phone permission has already been granted.");
            //carry on
        }

        //Write to Storage permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Storage permission has not been granted.

            requestStoragePermission();

        } else {

            // Storage permission is already available.
            Log.v(TAG,
                    "Storage permission has already been granted.");
            //carry on
        }


        Log.v(TAG,"NetAnalyzer Service Started");

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        Boolean started = sharedPref.getBoolean("Started", false);

        if(started)
        {
            Button button = (Button) findViewById(R.id.button);
            assert button!=null;
            button.setEnabled(false);
        }
        ImageButton imageButton = (ImageButton) findViewById(R.id.btnShowLocation);
        Button cancelAlarmButton = (Button)findViewById(R.id.button5);

        imageButton.setOnClickListener(this);
        cancelAlarmButton.setOnClickListener(this);

        track = (Button) findViewById(R.id.button1);
        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg1)
            {
                deleteDB();
            }
        });

        track = (Button) findViewById(R.id.button2);
        track.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg1)
            {
                exportDB();
            }
        });

        track = (Button) findViewById(R.id.button3);
        track.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg1)
            {
                exportToCSV();
            }
        });



        assert imageButton!=null;
    }

    //create the overridden function for onClickListener

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnShowLocation:
                Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                startIntent.setAction("startforeground");
                startService(startIntent);
                break;
            case R.id.button5:
                Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                stopIntent.setAction("stopforeground");
                startService(stopIntent);
                break;

            default:
                break;
        }

    }

    public void requestLocationPermission()
    {
        Log.i(TAG, "LOCATION permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
    }
    public void requestPhonePermission()
    {
        Log.i(TAG, "Phone permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PHONE);
    }
    public void requestStoragePermission()
    {
        Log.i(TAG, "STORAGE permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {

        if (requestCode == REQUEST_LOCATION)
        {
            Log.i(TAG, "Received response for Location permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Location permission has been granted
                Log.i(TAG, "Location permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_location,
                        Snackbar.LENGTH_SHORT).show();
            }
            else
            {
                Log.i(TAG, "Location permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }

        }
        else if (requestCode == REQUEST_STORAGE)
        {
            Log.i(TAG, "Received response for storage permissions request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Storage permission has been granted
                Log.i(TAG, "Storage permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_storage,
                        Snackbar.LENGTH_SHORT).show();
            }
            else
            {
                Log.i(TAG, "Storage permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }
        }
        else if (requestCode == REQUEST_PHONE)
        {
            Log.i(TAG, "Received response for phone permissions request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Phone permission has been granted
                Log.i(TAG, "Phone permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_phone,
                        Snackbar.LENGTH_SHORT).show();
            }
            else
            {
                Log.i(TAG, "Phone permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }

        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private String getIMEI()
    {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getService()
    {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    private String getModel()

    {
        return android.os.Build.MANUFACTURER+":"+android.os.Build.MODEL;
    }

    private String getOS()

    {
        return android.os.Build.VERSION.RELEASE;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
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


    public String POST(String url)
    {
        InputStream inputStream = null;
        String result = "";
            try {

                String IMEI = getIMEI();
                String service = getService();
                String modelMake = getModel();
                String androidVersion = getOS();

                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(url);
                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("IMEI", IMEI);
                jsonObject.accumulate("SERVICE", service);
                jsonObject.accumulate("MODEL", modelMake);
                jsonObject.accumulate("OS_VERSION", androidVersion);

                // 4. convert JSONObject to JSON to String
                json = jsonObject.toString();
                // ** Alternative way to convert Person object to JSON string usin Jackson Lib
                // ObjectMapper mapper = new ObjectMapper();
                // json = mapper.writeValueAsString(person);

                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity
                httpPost.setEntity(se);

                // 7. Set some headers to inform server about the type of the content   
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);

                // 9. receive response as inputStream
                inputStream = httpResponse.getEntity().getContent();

                // 10. convert inputstream to string
                if(inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else
                    result = "Did not work!";

            }
            catch (Exception e)
            {
                Log.d("InputStream", e.getLocalizedMessage());
            }

        // 11. return result
        return result;
        }

    public void onRegisterClicked(View view)
    {
        boolean isConnected = isConnected();
        if(isConnected)
        {
            Log.v(TAG, "isConnected = TRUE");
            //TODO
            new HttpAsyncTask().execute("YOUR_URL_HERE");
        }
        else
        {
            Log.v(TAG, "isConnected = FALSE");
            Toast.makeText(getBaseContext(), "Device has. No Internet Connectivity! Please check your Network Connection and try again", Toast.LENGTH_LONG).show();
        }

    }
    private class HttpAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... urls)
        {
            Log.v(TAG, "inside AsyncTask");
            return POST(urls[0]);
        }
        @Override
        protected void onPostExecute(String result)
        {
            Toast.makeText(getBaseContext(), "Device Registered!", Toast.LENGTH_LONG).show();
        }
    }

    public void exportToCSV()
    {

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state))
        {
            Toast.makeText(this, "MEDIA MOUNT ERROR!", Toast.LENGTH_LONG).show();
        }
        else
        {
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists())
            {
                exportDir.mkdirs();
                Log.v(TAG, "Directory made");
            }

            File file = new File(exportDir, "CellularData.csv") ;
            PrintWriter printWriter = null;
            try
            {
                file.createNewFile();
                printWriter = new PrintWriter(new FileWriter(file));
                DBHandler dbHandler = new DBHandler(getApplicationContext());
                SQLiteDatabase sqLiteDatabase = dbHandler.getReadableDatabase();
                Cursor curCSV = sqLiteDatabase.rawQuery("select * from cellRecords", null);
                printWriter.println("Latitude_LM,Longitude_LM,Latitude_FA,Longitude_FA,NETWORK_PROVIDER,TIMESTAMP,NETWORK_TYPE,NETWORK_STATE,NETWORK_RSSI,DATA_STATE,DATA_ACTIVITY");
                while(curCSV.moveToNext())
                {
                    String lmLatitude = curCSV.getString(curCSV.getColumnIndex("N_LAT"));
                    String lmLongitude = curCSV.getString(curCSV.getColumnIndex("N_LONG"));
                    String fLatitude = curCSV.getString(curCSV.getColumnIndex("F_LAT"));
                    String fLongitude = curCSV.getString(curCSV.getColumnIndex("F_LONG"));
                    String networkProvider = curCSV.getString(curCSV.getColumnIndex("NETWORK_PROVIDER"));

                    String timeStamp = curCSV.getString(curCSV.getColumnIndex("TIMESTAMP"));
                    String networkType = curCSV.getString(curCSV.getColumnIndex("NETWORK_TYPE"));
                    String networkState = curCSV.getString(curCSV.getColumnIndex("NETWORK_STATE"));
                    String networkRSSI = curCSV.getString(curCSV.getColumnIndex("NETWORK_RSSI"));
                    String dataState = curCSV.getString(curCSV.getColumnIndex("DATA_STATE"));
                    String dataActivity = curCSV.getString(curCSV.getColumnIndex("DATA_ACTIVITY"));

                    String record = lmLatitude + "," + lmLongitude + "," + fLatitude + "," + fLongitude + "," + networkProvider + "," + timeStamp + "," + networkType + "," + networkState + "," + networkRSSI+ "," + dataState + "," + dataActivity;
                    printWriter.println(record);
                }
                curCSV.close();
                sqLiteDatabase.close();
            }

            catch(Exception exc)
            {
                exc.printStackTrace();
                Toast.makeText(this, "ERROR!", Toast.LENGTH_LONG).show();
            }
            finally
            {
                if(printWriter != null) printWriter.close();
            }

            //If there are no errors, return true.
            Toast.makeText(this, "DB Exported to CSV file!", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteDB()
    {
        boolean result = this.deleteDatabase("mainTuple");
        if (result==true)
        {
            Toast.makeText(this, "DB Deleted!", Toast.LENGTH_LONG).show();
        }
    }
    private void exportDB()
    {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        String currentDBPath = "/data/" + "ubcomputerscience.ubwins.cellularnetworkmonitor" + "/databases/" + "mainTuple";
        String backupDBPath = "mainTuple";
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        if (currentDB.exists())
        {
            try
            {
                source = new FileInputStream(currentDB).getChannel();
                destination = new FileOutputStream(backupDB).getChannel();
                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();
                Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


}




