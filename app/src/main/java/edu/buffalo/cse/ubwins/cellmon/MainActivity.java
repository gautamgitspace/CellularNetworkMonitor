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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
//import com.pushlink.android.PushLink;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback
{

    public final String TAG = "[CELNETMON-ACTIVITY]";
    private DrawerLayout mLayout;
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
    String URL_UPLOAD = "http://104.196.177.7:80/aggregator/upload/";

    private String[] actions;
//    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);


        // Initialize Stetho to allow for viewing database in the Chrome inspector
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableDumpapp(
                        Stetho.defaultDumperPluginsProvider(this)
                ).enableWebKitInspector(
                        Stetho.defaultInspectorModulesProvider(this)
                ).build());

//        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(mToolbar);

        file = new File(getApplicationContext().getFilesDir(), fileName);

        /*DECLARE EDITOR FOR PERMISSIONS */
       SharedPreferences.Editor editor = preferences.edit();

        /*First read location permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestLocationPermission();
        }
        else
        {
            editor.putBoolean("LOCATION", true);
            editor.commit();
        }

        /*Second read phone state permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPhonePermission();
        }
        else
        {
            editor.putBoolean("PHONE", true);
            editor.commit();
        }

        /*Write to Storage permission*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestStoragePermission();
        }
        else
        {
            editor.putBoolean("STORAGE", true);
            editor.commit();
        }

        /*Wake Lock Permission*/

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED)
        {
            requestWakeLockPermission();
        }
        else
        {
            editor.putBoolean("WAKELOCK", true);
            editor.commit();
        }

        /* CALL TO REGISTER DEVICE*/
        /*FETCH ALL CONDITIONS TO CHECK FROM SHAREDPREF*/
        boolean locPermission = preferences.getBoolean("LOCATION",false);
        boolean storagePermission = preferences.getBoolean("STORAGE",false);
        boolean phonePermission = preferences.getBoolean("PHONE",false);
        boolean isRegistered = preferences.getBoolean("isRegistered", false);

        /*Call only after all 3 permissions are granted*/
        if(!isRegistered && locPermission && storagePermission && phonePermission)
        {
            onRegisterClicked();
        }

        Log.v(TAG, "CelNetMon Service Started");


        // Set up UI

        mTitle = mDrawerTitle = getTitle();
        actions = getResources().getStringArray(R.array.app_actions);
        mLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.simple_list_item, actions));

        mDrawerList.setOnItemClickListener(new DrawerClickListener());



        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            public void onDrawerClosed(View view){
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView){
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Options");
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mLayout.addDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if(savedInstanceState == null){
            selectItem(0);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }











    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    public void requestPhonePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PHONE);
    }

    public void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }

    public void requestWakeLockPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WAKE_LOCK}, REQUEST_WAKELOCK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        SharedPreferences.Editor editor = preferences.edit();
        if (requestCode == REQUEST_LOCATION) {
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
            }
            else {
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            }
            else {
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_PHONE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_WAKELOCK) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mLayout, R.string.permission_available_wakelock,
                        Snackbar.LENGTH_SHORT).show();
                editor.putBoolean("WAKELOCK", true);
                editor.commit();
            }
            else {
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*Methods to get registration fields*/
    /* https://developer.android.com/reference/android/os/Build.html */


    private String getIMEI() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getBoard() { return Build.BOARD; }
    private String getBrand() { return Build.BRAND; }
    private String getDevice() { return Build.DEVICE; }
    private String getHardware() { return android.os.Build.HARDWARE; }
    private String getManufacturer() { return android.os.Build.MANUFACTURER; }
    private String getModel() { return android.os.Build.MODEL; }
    private String getProduct() { return Build.PRODUCT; }

    /* https://developer.android.com/reference/android/telephony/TelephonyManager.html */

    private String getNetworkCountryISO() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkCountryIso();
    }

    private String getNetworkOperatorCode() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperator();
    }

    private String getNetworkOperatorName() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperatorName();
    }

    private String getPhoneType() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return Integer.toString(telephonyManager.getPhoneType());
    }

    private String getSimCountryISO() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimCountryIso();
    }

    private String getSimOperator() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimOperator();
    }

    private String getSimOperatorName() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimOperatorName();
    }

    /* https://developer.android.com/reference/android/os/Build.VERSION.html */
    //private String getBaseOS(){return Build.VERSION.BASE_OS;}

    private String getRelease() { return Build.VERSION.RELEASE; }
    private String getSdkInt() { return Integer.toString(Build.VERSION.SDK_INT); }

    public boolean isConnected()
    {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
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
                RegisterDeviceOuterClass.RegisterDevice registerDevice =
                        RegisterDeviceOuterClass.RegisterDevice.newBuilder()
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
            Toast.makeText(getBaseContext(),
                    "Device has no Internet Connectivity! " +
                            "Please check your Network Connection and try again",
                    Toast.LENGTH_LONG).show();

            if(!isRegistered)
            {

                receiver = new NetworkStateReceiver();
                receiver.addListener(new NetworkStateReceiver.NetworkStateReceiverListener() {
                    @Override
                    public void networkAvailable() {
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

            if(statusPhrase!=null && statusPhrase.equals("SUCCESS"))
            {
                TextView textView = (TextView) findViewById(R.id.textView30);
                if(textView != null) textView.setText("Device Registered!");
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
                }
                catch (JSONException j)
                {
                    j.printStackTrace();
                }
                if(reasonPhrase.equals("Given IMEI_HASH configuration already exists"))
                {
                    TextView textView = (TextView) findViewById(R.id.textView30);
                    if(textView != null) textView.setText("Device Already Registered!");
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isRegistered", true);
                    editor.commit();
                }
                else
                {
                    Log.d(TAG, reasonPhrase);
                    TextView textView = (TextView) findViewById(R.id.textView30);
                    if(textView != null) textView.setText("Device Registration Failed!");
                }
            }
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

    private class DrawerClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG,"ID selected: " + id);
            Log.d(TAG, "ID for Home: " + R.string.app_menu_home);
            selectItem(position);
        }
    }

    private void selectItem(int position){
        Fragment fragment;
        switch (position){
            case 0:
                fragment = new HomeFragment();
                break;
            case 1:
                fragment = new MapFragment();
                break;
            default:
                fragment = new HomeFragment();
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        mDrawerList.setItemChecked(position, true);
        setTitle(actions[position]);
        mLayout.closeDrawers();
    }

    @Override
    public void setTitle(CharSequence title){
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }
}
