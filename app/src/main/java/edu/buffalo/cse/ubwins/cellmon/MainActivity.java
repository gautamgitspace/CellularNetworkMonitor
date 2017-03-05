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
 *   CelNetMon v1.3 ~ Uses ScheduledExecutorService, does not uses Alarm(removed), does not uses Handler(removed), Uses wake lock.
 *   CelNetMon v1.3.1 ~ New Registration Fields Added. Now a total of 19 fields in the registration POST.
 *   CelNetMon v1.3.2 ~ Registration fields total of 18. HTTP POST via building ByteArrayEntity. Protocol Buffers tested.
 *   CelNetMon v1.3.3 ~ SALT(SharedPref) and HASH(SHA256) functionality added and tested. Minor Layout Change. Toast Messages Changed.
 *   CelNetMon v1.4.0 ~ Automated Registration. Splash Screen Added.
 *   CelNetMon v1.4.1 ~ BETA v1 : Uploading on wifi and charging in foreground, button changes,
 */

package edu.buffalo.cse.ubwins.cellmon;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;
import android.app.ActivityManager;

import com.facebook.stetho.Stetho;
import com.pushlink.android.PushLink;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback
{

    public final String TAG = "[CELNETMON-ACTIVITY]";
    private View mLayout;
    private static final int REQUEST_LOCATION = 0;
    private static final int REQUEST_STORAGE = 2;
    private static final int REQUEST_PHONE = 1;
    private static final int REQUEST_WAKELOCK = 3;
    String URL = "http://104.196.177.7:80/aggregator/register/";
    String fileName = "device_registration_details";
    File file;
    String responsePhrase;
    String reasonPhrase;
    JSONObject jsonObject;
    String statusPhrase;
    String IMEI_TO_POST;
    NetworkStateReceiver receiver;
    SharedPreferences preferences;
    Button startTrackingButton;
    Button stopTrackingButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableDumpapp(
                        Stetho.defaultDumperPluginsProvider(this)
                ).enableWebKitInspector(
                        Stetho.defaultInspectorModulesProvider(this)
                ).build());

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        file = new File(getApplicationContext().getFilesDir(), fileName);

        startTrackingButton = (Button) findViewById(R.id.button4);
        stopTrackingButton = (Button) findViewById(R.id.button5);

        /*Creating googleApiClient for Fused Location Provider*/
        mLayout = findViewById(R.id.myLayout);
        /*Ask for permissions here itself(both for final app and one write to storage for generating CSV file)*/

        if(isMyServiceRunning(ForegroundService.class)){
            startTrackingButton.setEnabled(false);
            stopTrackingButton.setEnabled(true);
        }
        else{
            startTrackingButton.setEnabled(true);
            stopTrackingButton.setEnabled(false);
        }

        /*DECLARE EDITOR FOR PERMISSIONS */
       SharedPreferences.Editor editor = preferences.edit();


        /*First read location permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestLocationPermission();
        }
        else
        {
            editor.putBoolean("LOCATION", true);
            editor.commit();
            //Log.v(TAG, "LOCATION permission has already been granted.");
        }

        /*Second read phone state permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPhonePermission();
        }
        else
        {
            editor.putBoolean("PHONE", true);
            editor.commit();
            //Log.v(TAG, "Phone permission has already been granted.");
        }

        /*Write to Storage permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
        else
        {
            editor.putBoolean("STORAGE", true);
            editor.commit();
            //Log.v(TAG, "Storage permission has already been granted.");
        }

        /*Wake Lock Permission*/

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            requestWakeLockPermission();
        }
        else
        {
            editor.putBoolean("WAKELOCK", true);
            editor.commit();
            //Log.v(TAG, "Wakelock permission has already been granted.");
        }

        /* CALL TO REGISTER DEVICE*/
        /*FETCH ALL CONDITIONS TO CHECK FROM SHAREDPREF*/
        boolean locPermission = preferences.getBoolean("LOCATION",false);
        boolean storagePermission = preferences.getBoolean("STORAGE",false);
        boolean phonePermission = preferences.getBoolean("PHONE",false);
        boolean isRegistered = preferences.getBoolean("isRegistered", false);
        //Log.e(TAG, "isRegistered Value [1]: " + isRegistered);

        /*Call only after all 3 permissions are granted*/
        if(!isRegistered && locPermission && storagePermission && phonePermission)
        {
            /* PushLink Registration */
            PushLink.start(this, R.mipmap.ic_launcher, "td6pjldtieedf3is", getIMEI());
            onRegisterClicked();
        }
        else if(isRegistered)
        {
            TextView textView = (TextView) findViewById(R.id.textView30);
            textView.setText("Device Already Registered!");
        }

        Log.v(TAG, "CelNetMon Service Started");

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        Boolean started = sharedPref.getBoolean("Started", false);

        if (started) {
            Button button = (Button) findViewById(R.id.button);
            assert button != null;
            button.setEnabled(false);
        }

        startTrackingButton.setOnClickListener(this);
        stopTrackingButton.setOnClickListener(this);
        Button ping = (Button) findViewById(R.id.ping);
        ping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Ping pressed");
                new PingTask().execute();
            }
        });
        /*
        //TRACK BUTTON 1 - DELETE DB
        track = (Button) findViewById(R.id.button1);
        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg1) {
                deleteDB();
            }
        });

        //TRACK BUTTON 2 - EXPORT DB
        track = (Button) findViewById(R.id.button2);
        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg1) {
                exportDB();
            }
        });

        //TRACK BUTTON 3 - EXPORT CSV
        track = (Button) findViewById(R.id.button3);
        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg1) {
                exportToCSV();
            }
        });
        */


        //create receiver and register it

    }

class PingTask extends AsyncTask<String, Void, Void>{

    protected Void doInBackground(String... urls){
        final TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String IMEI_HASH = telephonyManager.getDeviceId();
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] sha256Hash = sha256.digest(IMEI_HASH.getBytes("UTF-8"));
            IMEI_HASH = Base64.encodeToString(sha256Hash, Base64.DEFAULT);
            IMEI_HASH=IMEI_HASH.replaceAll("\n", "");

        }

        catch(Exception e)
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
            Log.d(TAG, customURL);
            request.setURI(new URI(customURL));
            Log.d(TAG, "Prerequest time: " + System.currentTimeMillis());
            response = client.execute(request);
            Log.d(TAG, "Postrequest time: " + System.currentTimeMillis());
            Log.v(TAG, "RESPONSE PHRASE FOR HTTP GET: " + response.getStatusLine().getReasonPhrase());
            Log.v(TAG, "RESPONSE STATUS FOR HTTP GET: " + response.getStatusLine().getStatusCode());
//            Log.d(TAG, new JSONObject(response).toString());
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
        return null;
    }
}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mMenuInfater = getMenuInflater();
        mMenuInfater.inflate(R.menu.app_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_mapView){
            Toast.makeText(MainActivity.this,"MapView Clicked",Toast.LENGTH_LONG).show();
        }
        if(item.getItemId() == R.id.action_aboutUs){
            Toast.makeText(MainActivity.this,"About Us Clicked",Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
       SharedPreferences.Editor editor = preferences.edit();
        Log.e(TAG, "inside on click");
        switch (v.getId()) {
            case R.id.button4:
                Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                startIntent.setAction("startforeground");
                startService(startIntent);
                editor.putBoolean("TRACKING", true);
                editor.commit();
                stopTrackingButton.setEnabled(true);
                startTrackingButton.setEnabled(false);
                break;
            case R.id.button5:
                if(isMyServiceRunning(ForegroundService.class))
                {
                    Log.v("STOP Button","Stopped");
                Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                stopIntent.setAction("stopforeground");
                startService(stopIntent);}
                startTrackingButton.setEnabled(true);
                stopTrackingButton.setEnabled(false);
                editor.putBoolean("TRACKING", false);
                editor.commit();
                break;
            default:
                break;
        }
    }

    public void requestLocationPermission() {
        //Log.i(TAG, "LOCATION permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    public void requestPhonePermission() {
        //Log.i(TAG, "Phone permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PHONE);
    }

    public void requestStoragePermission() {
        //Log.i(TAG, "STORAGE permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }

    public void requestWakeLockPermission() {
        //Log.i(TAG, "Wake lock permission has NOT been granted. Requesting permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, REQUEST_WAKELOCK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SharedPreferences.Editor editor = preferences.edit();
        if (requestCode == REQUEST_LOCATION) {
            //Log.i(TAG, "Received response for Location permission request.");

            /*Check if the only required permission has been granted*/

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Log.i(TAG, "Location permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_location,
                        Snackbar.LENGTH_SHORT).show();
                editor.putBoolean("LOCATION", true);
                editor.commit();
                boolean one = preferences.getBoolean("STORAGE", false);
                boolean two = preferences.getBoolean("PHONE", false);
                boolean isRegistered = preferences.getBoolean("isRegistered", false);
                if(one && two && !isRegistered)
                {
                    onRegisterClicked();
                }
            } else {
                //Log.i(TAG, "Location permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }

        } else if (requestCode == REQUEST_STORAGE) {
            //Log.i(TAG, "Received response for storage permissions request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Log.i(TAG, "Storage permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_storage,
                        Snackbar.LENGTH_SHORT).show();
                editor.putBoolean("STORAGE", true);
                editor.commit();
                boolean one = preferences.getBoolean("LOCATION", false);
                boolean two = preferences.getBoolean("PHONE", false);
                boolean isRegistered = preferences.getBoolean("isRegistered", false);
                if(one && two && !isRegistered)
                {
                    onRegisterClicked();
                }
            } else {
               //Log.i(TAG, "Storage permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PHONE) {
            //Log.i(TAG, "Received response for phone permissions request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Log.i(TAG, "Phone permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_phone,
                        Snackbar.LENGTH_SHORT).show();
                editor.putBoolean("PHONE", true);
                editor.commit();
                boolean one = preferences.getBoolean("LOCATION", false);
                boolean two = preferences.getBoolean("STORAGE", false);
                boolean isRegistered = preferences.getBoolean("isRegistered", false);
                if(one && two && !isRegistered)
                {
                    onRegisterClicked();
                }
            } else {
                //Log.i(TAG, "Phone permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_WAKELOCK) {
            //Log.i(TAG, "Received response for wakeLock permissions request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Log.i(TAG, "Phone permission has now been granted.");
                Snackbar.make(mLayout, R.string.permission_available_wakelock,
                        Snackbar.LENGTH_SHORT).show();
                editor.putBoolean("WAKELOCK", true);
                editor.commit();
            } else {
                //Log.i(TAG, "Phone permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*Methods to get registration fields*/
    /* https://developer.android.com/reference/android/os/Build.html */


    private String getIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getBoard() {
        return Build.BOARD;
    }

    private String getBrand() {
        return Build.BRAND;
    }

    private String getDevice() {
        return Build.DEVICE;
    }

    private String getHardware() {
        return android.os.Build.HARDWARE;
    }

    private String getManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    private String getModel() {
        return android.os.Build.MODEL;
    }

    private String getProduct() {
        return Build.PRODUCT;
    }

    /* https://developer.android.com/reference/android/telephony/TelephonyManager.html */

    private String getNetworkCountryISO() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkCountryIso();
    }

    private String getNetworkOperatorCode() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperator();
    }

    private String getNetworkOperatorName() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperatorName();
    }

    private String getPhoneType() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return Integer.toString(telephonyManager.getPhoneType());
    }

    private String getSimCountryISO() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimCountryIso();
    }

    private String getSimOperator() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimOperator();
    }

    private String getSimOperatorName() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimOperatorName();
    }

    /* https://developer.android.com/reference/android/os/Build.VERSION.html */
    //private String getBaseOS(){return Build.VERSION.BASE_OS;}

    private String getRelease() {
        return Build.VERSION.RELEASE;
    }

    private String getSdkInt() {
        return Integer.toString(Build.VERSION.SDK_INT);
    }


    public boolean isConnected()
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


    public String POST(String url)
    {
        Log.d(TAG, "Registering device...");

        InputStream inputStream = null;
        int statusCode;
        String result = "";

            try {

                String IMEI = getIMEI();
                IMEI_TO_POST = genHash(IMEI);


                /*FETCH OTHER PARAMS*/
                String board = getBoard();
                String brand = getBrand();
                String device = getDevice();
                String hardware = getHardware();
                String manufacturer = getManufacturer();
                String modelMake = getModel();
                String product = getProduct();

                String networkCountryISO = getNetworkCountryISO();
                String networkOperatorCode = getNetworkOperatorCode();
                String networkOperatorName = getNetworkOperatorName();
                String phoneType = getPhoneType();
                String simCountryISO = getSimCountryISO();
                String simOperator = getSimOperator();
                String simOperatorName = getSimOperatorName();

                String release = getRelease();
                String sdkInt = getSdkInt();

                /*SERIALIZATION*/
                RegisterDeviceOuterClass.RegisterDevice registerDevice = RegisterDeviceOuterClass.RegisterDevice.newBuilder()
                        .setIMEIHASH(IMEI_TO_POST)
                        .setBOARD(board)
                        .setBRAND(brand)
                        .setDEVICE(device)
                        .setHARDWARE(hardware)
                        .setMANUFACTURER(manufacturer)
                        .setMODEL(modelMake)
                        .setPRODUCT(product)
                        .setNETWORKCOUNTRYISO(networkCountryISO)
                        .setNETWORKOPERATORCODE(networkOperatorCode)
                        .setNETWORKOPERATORNAME(networkOperatorName)
                        .setPHONETYPE(phoneType)
                        .setSIMCOUNTRYISO(simCountryISO)
                        .setSIMOPERATOR(simOperator)
                        .setSIMOPERATORNAME(simOperatorName)
                        .setRELEASE(release)
                        .setSDKINT(sdkInt).build();

                byte infoToSend[] = registerDevice.toByteArray();

                /*POST LOGGING BLOCK
                Log.v(TAG,"######################################################");
                Log.v(TAG, "Phone Info #1: IMEI: " + IMEI);
                Log.v(TAG, "Phone Info #1B: IMEI_BASE64_HASH: " + IMEI_TO_POST);

                Log.v(TAG, "Phone Info #2: BOARD: " + board);
                Log.v(TAG, "Phone Info #3: BRAND: " + brand);
                Log.v(TAG, "Phone Info #4: DEVICE: " + device);
                Log.v(TAG, "Phone Info #5: HARDWARE: " + hardware);
                Log.v(TAG, "Phone Info #6: MANUFACTURER: " + manufacturer);
                Log.v(TAG, "Phone Info #7: MODEL: " + modelMake);
                Log.v(TAG, "Phone Info #8: PRODUCT: " + product);


                Log.v(TAG,"Phone Info #9: NETWORK_COUNTRY_ISO: " + networkCountryISO);
                Log.v(TAG,"Phone Info #10: NETWORK_OPERATOR_CODE: " + networkOperatorCode);
                Log.v(TAG,"Phone Info #11: NETWORK_OPERATOR_NAME: " + networkOperatorName);
                Log.v(TAG,"Phone Info #12: PHONE_TYPE: " + phoneType);
                Log.v(TAG,"Phone Info #13: SIM_COUNTRY_ISO: " + simCountryISO);
                Log.v(TAG,"Phone Info #14: SIM_OPERATOR: " + simOperator);
                Log.v(TAG,"Phone Info #15: SIM_OPERATOR_NAME: " + simOperatorName);

                Log.v(TAG,"Phone Info #16: RELEASE: " + release);
                Log.v(TAG,"Phone Info #17: SDK_INT: " + sdkInt);
                Log.v(TAG,"######################################################");

                */

                /*1. create HttpClient*/
                HttpClient httpclient = new DefaultHttpClient();

                /*2. make POST request to the given URL*/
                HttpPost httpPost = new HttpPost(url);

                /*5. Build ByteArrayEntity*/
                //StringEntity se = new StringEntity(json);
                ByteArrayEntity byteArrayEntity = new ByteArrayEntity(infoToSend);

                /*6. Set httpPost Entity*/
                httpPost.setEntity(byteArrayEntity);
                //httpPost.setEntity(inputStreamEntity);

                /*7. Set some headers to inform server about the type of the content*/
                //httpPost.setHeader("Accept", "application/json");
                //httpPost.setHeader("Content-type", "application/json");

                /*8. Execute POST request to the given URL*/
                HttpResponse httpResponse = httpclient.execute(httpPost);

                /*9. receive response as inputStream*/
                statusCode = httpResponse.getStatusLine().getStatusCode();

                /*CONVERT INPUT STREAM TO STRING*/
                responsePhrase = EntityUtils.toString(httpResponse.getEntity());
                Log.v(TAG, "RESPONSE" + responsePhrase);
                Log.d(TAG, httpResponse.toString());
                /*PARSE JSON RESPONSE*/


                if(statusCode!=404)
                {
                    result = Integer.toString(statusCode);
                    Log.v(TAG, "STATUS CODE: " + result);
                }

                else
                {
                    result = Integer.toString(statusCode);
                    //Log.v(TAG, "STATUS CODE: " + result);
                }

            }
            catch (Exception e)
            {
                Log.d("InputStream", e.getLocalizedMessage());
            }
        return result;
        }

    public void onRegisterClicked()
    {
        boolean isConnected = isConnected();
        boolean isRegistered = preferences.getBoolean("isRegistered", false);
        if (isConnected && !isRegistered)
        {
            new HttpAsyncTask().execute(URL);
        }
        else
        {
            Log.v(TAG, "isConnected = FALSE");
            Toast.makeText(getBaseContext(), "Device has no Internet Connectivity! Please check your Network Connection and try again", Toast.LENGTH_LONG).show();

            if(!isRegistered)
            {

                receiver = new NetworkStateReceiver();
                receiver.addListener(new NetworkStateReceiver.NetworkStateReceiverListener() {
                @Override
                public void networkAvailable() {
                    //Log.v("AUTOMATE", "NETWORK IS AVAILABLE");

                        onRegisterClicked();

                }

                @Override
                public void networkUnavailable() {
                    //Log.v("AUTOMATE", "NETWORK IS UNAVAILABLE");
                }

            });
            registerReceiver(receiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        }
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... urls)
        {
            return POST(urls[0]);
        }
        @Override
        protected void onPostExecute(String result)
        {
            try {
                jsonObject = new JSONObject(responsePhrase);
                statusPhrase = jsonObject.getString("status");
            }
            catch (JSONException j)
            {
                j.printStackTrace();
            }

            //Log.e(TAG, "STATUS PHRASE: " + statusPhrase);
            if(statusPhrase!=null && statusPhrase.equals("SUCCESS"))
            {
                TextView textView = (TextView) findViewById(R.id.textView30);
                textView.setText("Device Registered!");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isRegistered", true);
                editor.commit();
                boolean temp = preferences.getBoolean("isRegistered", false);
                Log.e(TAG, "isRegistered value [temp]: " + temp);
            }
            else if(statusPhrase!=null && statusPhrase.equals("FAIL"))
            {
                try {
                    reasonPhrase = jsonObject.getString("reason");
                    //Log.e(TAG, "REASON PHRASE: " + reasonPhrase);
                }
                catch (JSONException j)
                {
                    j.printStackTrace();
                }
                if(reasonPhrase.equals("Given IMEI_HASH configuration already exists"))
                {
                    TextView textView = (TextView) findViewById(R.id.textView30);
                    textView.setText("Device Already Registered!");
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isRegistered", true);
                    editor.commit();
                }
                else
                {
                    Log.d(TAG, reasonPhrase);
                    TextView textView = (TextView) findViewById(R.id.textView30);
                    textView.setText("Device Registration Failed!");
                }
            }
        }
    }


    /* EXPORT TO CSV BLOCK
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
                printWriter.println("Latitude_LM,Longitude_LM,Latitude_FA,Longitude_FA,LOCATION_PROVIDER,TIMESTAMP,NETWORK_TYPE,NETWORK_TYPE2,NETWORK_PARAM1,NETWORK_PARAM2,NETWORK_PARAM3,NETWORK_PARAM4,DBM,NETWORK_LEVEL,ASU_LEVEL,DATA_STATE,DATA_ACTIVITY,CALL_STATE");
                while(curCSV.moveToNext())
                {
                    String lmLatitude = curCSV.getString(curCSV.getColumnIndex("N_LAT"));
                    String lmLongitude = curCSV.getString(curCSV.getColumnIndex("N_LONG"));
                    String fLatitude = curCSV.getString(curCSV.getColumnIndex("F_LAT"));
                    String fLongitude = curCSV.getString(curCSV.getColumnIndex("F_LONG"));
                    String locationProvider = curCSV.getString(curCSV.getColumnIndex("LOCATION_PROVIDER"));

                    String timeStamp = curCSV.getString(curCSV.getColumnIndex("TIMESTAMP"));
                    String networkType = curCSV.getString(curCSV.getColumnIndex("NETWORK_TYPE"));
                    String networkType2 = curCSV.getString(curCSV.getColumnIndex("NETWORK_TYPE2"));
                    String networkParam1 = curCSV.getString(curCSV.getColumnIndex("NETWORK_PARAM1"));
                    String networkParam2 = curCSV.getString(curCSV.getColumnIndex("NETWORK_PARAM2"));
                    String networkParam3 = curCSV.getString(curCSV.getColumnIndex("NETWORK_PARAM3"));
                    String networkParam4 = curCSV.getString(curCSV.getColumnIndex("NETWORK_PARAM4"));

                    String dbm = curCSV.getString(curCSV.getColumnIndex("DBM"));
                    String networklevel = curCSV.getString(curCSV.getColumnIndex("NETWORK_LEVEL"));
                    String asulevel = curCSV.getString(curCSV.getColumnIndex("ASU_LEVEL"));
                    String dataState = curCSV.getString(curCSV.getColumnIndex("DATA_STATE"));
                    String dataActivity = curCSV.getString(curCSV.getColumnIndex("DATA_ACTIVITY"));
                    String callState = curCSV.getString(curCSV.getColumnIndex("CALL_STATE"));

                    String record = lmLatitude + "," + lmLongitude + "," + fLatitude + "," + fLongitude + "," + locationProvider + "," + timeStamp + "," + networkType + ","  + networkType2 + "," + networkParam1 + "," + networkParam2 + ","  + networkParam3 + ","  + networkParam4 + ","  + dbm + ","  + networklevel+ ","  + asulevel + "," + dataState + "," + dataActivity + "," + callState;
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
            Toast.makeText(this, "DB Exported to CSV file!", Toast.LENGTH_LONG).show();
        }
    }
    */

    /* DELETE DB BLOCK
    private void deleteDB()
    {
        boolean result = this.deleteDatabase("mainTuple");
        if (result==true)
        {
            Toast.makeText(this, "DB Deleted!", Toast.LENGTH_LONG).show();
        }
    }
    */

    /* EXPORT DB BLOCK
    private void exportDB()
    {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        String currentDBPath = "/data/" + "ubcomputerscience.edu.buffalo.cse.ubwins.cellmon.cellularnetworkmonitor" + "/databases/" + "mainTuple";
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
    */

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
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
