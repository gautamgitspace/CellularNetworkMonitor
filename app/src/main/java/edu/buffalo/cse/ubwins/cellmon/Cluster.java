package edu.buffalo.cse.ubwins.cellmon;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pcoonan on 5/12/17.
 */

public class Cluster {
    public List<Entry> entries;
    public Coordinate centroid;
    public int id;

    public Cluster(int id){
        this.id = id;
        this.entries = new ArrayList<Entry>();
        this.centroid = null;
    }

    public List<Entry> getEntries(){
        return entries;
    }

    public void addEntry(Entry entry){
        entries.add(entry);
    }

    public void setEntries(List<Entry> entries){
        this.entries = entries;
    }

    public Coordinate getCentroid() {
        return centroid;
    }

    public void setCentroid(Coordinate centroid) {
        this.centroid = centroid;
    }

    public int getId() {
        return id;
    }

    public void clear(){
        entries.clear();
    }
}
