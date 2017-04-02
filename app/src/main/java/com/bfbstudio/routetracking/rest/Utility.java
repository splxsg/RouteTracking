package com.bfbstudio.routetracking.rest;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Blues on 30/03/2017.
 */

public class Utility {

    private static double miRatio = 1600;
    private static double kmRatio = 1000;
    public final static int DISTANCE_MILES = 0;
    public final static int DISTANCE_KILOMETERS = 1;
    private static int unitKey = DISTANCE_MILES;


    public static void setUnitKey(int key) {unitKey = key;}
    public static int getUnitKey(){return unitKey;}

    public static String millisectoSimpleTimeFormat(long millisec){
        int hours,mins,secs;
        hours =  (int)TimeUnit.MILLISECONDS.toHours(millisec);
        mins = (int) (TimeUnit.MILLISECONDS.toMinutes(millisec) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisec))); // The change is in this line
        secs = (int)(TimeUnit.MILLISECONDS.toSeconds(millisec) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisec)));

        if(hours != 0)
            return String.format("%d hours, %d mins, %d secs",hours,mins,secs);
        else if(mins != 0)
                return String.format("%d mins, %d secs",mins,secs);
        else
            return String.format("%d secs",secs);
    }

    public static float calculateDistance(double latitudenew, double longitudenew, double latitudeold, double longitudeold)
    {
        Location locationA = new Location("point A");
        locationA.setLatitude(latitudenew);
        locationA.setLongitude(longitudenew);
        Location locationB = new Location("point B");
        locationB.setLatitude(latitudeold);
        locationB.setLongitude(longitudeold);
        return locationA.distanceTo(locationB);
    }

    public static String formatDistance(double meters)
    {
        double ratio = 1.0;
        String unit = "";


        switch (unitKey)
        {
            case DISTANCE_MILES:
            {
                ratio = miRatio;
                unit = "mi.";
            }
                break;
            case DISTANCE_KILOMETERS:
                {
                    ratio = kmRatio;
                    unit = "km";
            }
                break;
        }
        if (meters/ratio>=0.5)
            return String.format("%.2f ",meters/ratio)+unit;
        else
            return String.format("%.0f ",meters)+"m";
    }

    public static String millisecToDate(String milliseconds)
    {
        Date date = new Date();
        date.setTime(Long.parseLong(milliseconds));
        return new SimpleDateFormat("MMM d, hh:mm:ss").format(date);
    }
}

