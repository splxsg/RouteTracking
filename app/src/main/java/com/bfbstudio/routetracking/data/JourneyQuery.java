package com.bfbstudio.routetracking.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.bfbstudio.routetracking.rest.Utility;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;

import java.util.Date;
import com.bfbstudio.routetracking.data.JourneyContract.JourneyEntry;
import com.bfbstudio.routetracking.data.JourneyContract.LocationEntry;
/**
 * Created by Blues on 28/03/2017.
 */

public class JourneyQuery {
    Context mContext;
    private final static String TAG = "JourneyQuery";
    public JourneyQuery(Context mContext){this.mContext = mContext;}


    public LatLng getLatLng(Cursor mCursor)
    {
        return new LatLng(mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LATITUDE)),mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE)));
    }

    public String getJourneyInfo(String journeyId){
        Cursor mCursor;
        Date date;
        String info = "Journey Starts: ";
        mCursor = mContext.getContentResolver().query(
                JourneyContract.JourneyEntry.CONTENT_URI,
                null,
                JourneyEntry.COLUMN_JOURNEY_ID+"=?",
                new String[]{journeyId},
                null
        );
        if(mCursor.moveToFirst())
        {
            date = new Date(Long.parseLong(journeyId));
            info += new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(date).toString()+"\n";
            date = new Date(Long.parseLong(journeyId)+Long.parseLong(mCursor.getString(mCursor.getColumnIndex(JourneyEntry.COLUMN_JOURNEY_DURATION))));
            info += "Journey Ends: ";
            info += new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(date).toString()+"\n";
            info += "Distance: ";
            info += Utility.formatDistance(mCursor.getDouble(mCursor.getColumnIndex(JourneyEntry.COLUMN_JOURNEY_DISTANCE)));
        }
        return info;
    }

    public Cursor getCursor(Uri CONTENT_URI)
    {

        Cursor mCursor = mContext.getContentResolver().query(CONTENT_URI,
                null,
                null,
                null,
                null);
        return mCursor;
    }
    //Call when insert a new journey
    public Uri insertNewJourneyRecord(String journeyId, String totalTime, double distance)
    {
        Uri inserteduri;
        ContentValues journeyValue = new ContentValues();
        journeyValue.put(JourneyEntry.COLUMN_JOURNEY_ID,journeyId);
        journeyValue.put(JourneyEntry.COLUMN_JOURNEY_DATE,Utility.millisecToDate(journeyId));
        journeyValue.put(JourneyEntry.COLUMN_JOURNEY_DURATION, totalTime);
        journeyValue.put(JourneyEntry.COLUMN_JOURNEY_DISTANCE,distance);
        inserteduri = mContext.getContentResolver().insert(JourneyContract.JourneyEntry.CONTENT_URI,journeyValue);
        if(ContentUris.parseId(inserteduri) != -1)
            Log.d(TAG,"Journey Insert Done");
        else
            Log.d(TAG,"Journey Insert Error");
        return inserteduri;
    }

    //Call when insert a new location
    public void addNewLocationObject(String locationId, Long timeInMilliSeconds, Double latitude, Double longitude)
    {
        Uri inserteduri;
        ContentValues locationValue = new ContentValues();
        locationValue.put(LocationEntry.COLUMN_JOURNEY_ID,locationId);
        locationValue.put(LocationEntry.COLUMN_TIME, timeInMilliSeconds);
        locationValue.put(LocationEntry.COLUMN_LATITUDE, latitude);
        locationValue.put(LocationEntry.COLUMN_LONGITUDE, longitude);
        inserteduri = mContext.getContentResolver().insert(JourneyContract.LocationEntry.CONTENT_URI, locationValue);
        if(ContentUris.parseId(inserteduri) != -1)
            Log.d(TAG,"Location Insert Done");
        else
            Log.d(TAG,"Location Insert Error");
    }


    public double calculateDistance(String locationId)
    {
        double oldLatitude, newLatitude, oldLongitude, newLongitude, distance;

        Cursor mCursor = mContext.getContentResolver().query(JourneyContract.LocationEntry.CONTENT_URI,
                null,
                LocationEntry.COLUMN_JOURNEY_ID+"=?",
                new String[]{locationId},
                null);
        distance = 0.0;
        if(mCursor.moveToFirst()) {

            newLatitude = mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LATITUDE));
            newLongitude = mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE));
            while (!mCursor.isAfterLast()) {
                oldLatitude = newLatitude;
                oldLongitude = newLongitude;
                newLatitude = mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LATITUDE));
                newLongitude = mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE));
                distance += Utility.calculateDistance(newLatitude,newLongitude,oldLatitude,oldLongitude);
                mCursor.moveToNext();
            }
        }
        return distance;
    }
   /* public List<LatLng> getAllPolypoint()
    {
        Cursor mCursor = mContext.getContentResolver().query(JourneyContract.LocationEntry.CONTENT_URI,
                null,
                null,//column_journey_id+"=?",
                null,//new String[]{"TEST"},
                null);
        List<LatLng> polyPoint = new ArrayList<>();
        if (mCursor.moveToFirst()) {
            while (!mCursor.isAfterLast()) {
                double latitude = mCursor.getDouble(mCursor.getColumnIndex(JourneyContract.LocationEntry.COLUMN_LATITUDE));
                double longitude = mCursor.getDouble(mCursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE));
                polyPoint.add(new LatLng(latitude,longitude));
                mCursor.moveToNext();
            }
        }
        return polyPoint;

    }*/
}
