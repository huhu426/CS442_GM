package com.example.jaewonkim.tmapproject;


/**
 * Created by Jaewon Kim on 2016-05-18.
 */
public class ListViewItem{

    private String POI_Name;
    private double Latitude;
    private double Longitude;

    public String getPOI_Name() { return POI_Name; }
    public double getLatitude() { return Latitude; }
    public double getLongitude() { return Longitude; }

    public void setPOI_Name(String POI_Name) { this.POI_Name = POI_Name; }
    public void setLatitude(double latitude) { Latitude = latitude; }
    public void setLongitude(double longitude) { Longitude = longitude; }
}