package edu.buffalo.cse.ubwins.cellmon;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by patrick on 5/2/17.
 */

public class Entry implements ClusterItem{
    public long timestamp;
//    public double latitude;
//    public double longitude;
    public Coordinate coordinate;
    public int networkCellType;
    public int dbm;
    public int signalLevel;
    public int clusterNumber = 0;

    private Entry(){};
    public static Entry mapJSON(JSONObject object) throws JSONException {
        Entry ret = new Entry();
        ret.timestamp = object.getLong("timestamp");
//        ret.latitude = object.getDouble("fused_lat");
//        ret.longitude = object.getDouble("fused_long");
        ret.coordinate = new Coordinate(object.getDouble("fused_lat"),object.getDouble("fused_long"));
        ret.networkCellType = object.getInt("network_cell_type");
        ret.dbm = object.getInt("signal_dbm");
        ret.signalLevel = object.getInt("signal_level");
        return ret;
    }

    @Override
    public LatLng getPosition() {
        return coordinate.getPosition();
    }

    @Override
    public String getTitle() {
        return String.valueOf(timestamp);
    }

    @Override
    public String getSnippet() {
        return String.valueOf(dbm);
    }
}
