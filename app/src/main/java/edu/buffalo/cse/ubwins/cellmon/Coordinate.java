package edu.buffalo.cse.ubwins.cellmon;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

/**
 * Created by pcoonan on 5/12/17.
 */

public class Coordinate {
    private LatLng position;

    public Coordinate(double latitude, double longitude){
        this.position = new LatLng(latitude, longitude);
    }

    public void setLatitude(double latitude){
        this.position = new LatLng(latitude, position.longitude);
    }

    public void setLongitude(double longitude){
        this.position = new LatLng(position.latitude, longitude);
    }

    public double getLatitude(){
        return this.position.latitude;
    }

    public double getLongitude(){
        return this.position.longitude;
    }

    public LatLng getPosition(){
        return this.position;
    }

    protected static double distance(Coordinate c, Coordinate centroid){
        return Math.sqrt(
                Math.pow((centroid.getLatitude() - c.getLatitude()), 2) +
                Math.pow((centroid.getLongitude() - c.getLongitude()), 2));
    }

    protected static Coordinate createRandomCoordinate(double maxLat, double minLat,
                                                       double minLong, double maxLong){
        Random r = new Random();
        double latitude = minLat + (maxLat - minLat) * r.nextDouble();
        double longitude = minLong + (maxLong - minLong) * r.nextDouble();
        return new Coordinate(latitude, longitude);
    }
}
