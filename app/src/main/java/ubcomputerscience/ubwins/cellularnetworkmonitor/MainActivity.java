/**
 *   Created by Gautam on 6/18/16.
 *   MBP111.0138.B16
 *   System Serial: C02P4SP9G3QH
 *   agautam2@buffalo.edu
 *   University at Buffalo, The State University of New York.
 *   Copyright © 2016 Gautam. All rights reserved.
 */

package ubcomputerscience.ubwins.cellularnetworkmonitor;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity
{

    static final String TAG = "[CELNETMON-DEBUG]";
    //Exports SQLiteDB to CSV file in Phone Storage
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
                printWriter.println("Latitude,Longitude,locality,city,state,country,NETWORK_PROVIDER,TIMESTAMP,NETWORK_TYPE,NETWORK_STATE,NETWORK_RSSI");
                while(curCSV.moveToNext())
                {
                    Double latitude = curCSV.getDouble(curCSV.getColumnIndex("LAT"));
                    Double longitude = curCSV.getDouble(curCSV.getColumnIndex("LONG"));
                    String networkProvider = curCSV.getString(curCSV.getColumnIndex("NETWORK_PROVIDER"));
                    String locality = curCSV.getString(curCSV.getColumnIndex("LOCALITY"));
                    String city = curCSV.getString(curCSV.getColumnIndex("CITY"));
                    String stateName = curCSV.getString(curCSV.getColumnIndex("STATE"));
                    String country = curCSV.getString(curCSV.getColumnIndex("COUNTRY"));

                    String timeStamp = curCSV.getString(curCSV.getColumnIndex("TIMESTAMP"));
                    String networkType = curCSV.getString(curCSV.getColumnIndex("NETWORK_TYPE"));
                    String networkState = curCSV.getString(curCSV.getColumnIndex("NETWORK_STATE"));
                    String networkRSSI = curCSV.getString(curCSV.getColumnIndex("NETWORK_RSSI"));

                    String record = latitude + "," + longitude + "," + locality + "," + city + "," + stateName + "," + country + "," + networkProvider + "," + timeStamp + "," + networkType + "," + networkState + "," + networkRSSI;
                    Log.v(TAG, "attempting to write to file");
                    printWriter.println(record);
                    Log.v(TAG, "data written to file");
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
        FileChannel source = null;
        FileChannel destination = null;
        String currentDBPath = "/data/" + "ubwins.ubcomputerscience.netanalyzer" + "/databases/" + "mainTuple";
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


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
