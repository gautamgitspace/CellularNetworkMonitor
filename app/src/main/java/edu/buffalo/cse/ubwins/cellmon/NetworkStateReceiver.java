package edu.buffalo.cse.ubwins.cellmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AmmY on 01/09/16.
 */
public class NetworkStateReceiver extends BroadcastReceiver
{

    static List<NetworkStateReceiverListener> listeners;
    protected Boolean connected;
    public final String TAG = "[NW-STATE-RCVR]";



    public NetworkStateReceiver()
    {
        listeners = new ArrayList<>();
        connected = null;
    }


    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;


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

    public static String getConnectivityStatusString(Context context)
    {
        int conn = getConnectivityStatus(context);
        String status = null;
        if (conn == TYPE_WIFI)
        {
            status = "Wifi enabled";
        }
        else if (conn == TYPE_MOBILE)
        {
            status = "Mobile data enabled";
        }
        else if (conn == TYPE_NOT_CONNECTED)
        {
            status = "Not connected to Internet";
        }
        return status;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null || intent.getExtras() == null)
            return;

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED)
        {
            connected = true;
        }
        else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE))
        {
            connected = false;
        }

        notifyStateToAll();
    }

    private void notifyStateToAll()
    {
        for (NetworkStateReceiverListener listener : listeners) {
            //Log.e("NSR","Listener ");
            notifyState(listener);
        }
    }

    private void notifyState(NetworkStateReceiverListener listener)
    {
        if (connected == null || listener == null)
            return;

        if (connected == true)
            listener.networkAvailable();
        else
            listener.networkUnavailable();

    }

    public void addListener(NetworkStateReceiverListener l)
    {
        listeners.add(l);
        notifyState(l);
    }

    public void removeListener(NetworkStateReceiverListener l)
    {
        listeners.remove(l);
    }

    public interface NetworkStateReceiverListener
    {
        void networkAvailable();
        void networkUnavailable();
    }
}