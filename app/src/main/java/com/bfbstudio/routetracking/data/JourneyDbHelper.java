package com.bfbstudio.routetracking.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.bfbstudio.routetracking.data.JourneyContract.JourneyEntry;
import com.bfbstudio.routetracking.data.JourneyContract.LocationEntry;

/**
 * Created by Blues on 27/03/2017.
 */

public class JourneyDbHelper extends SQLiteOpenHelper {
    private static  final int DATABASE_VERSION =9;
    static final String DATABASE_NAME = "journey.db";
    public JourneyDbHelper(Context context) {super(context, DATABASE_NAME, null, DATABASE_VERSION);}

    private void createLocationTable(SQLiteDatabase sqLiteDatabase){
        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " + LocationEntry.LOCATION_TABLE + " (" +
                LocationEntry._ID + " INTEGER PRIMARY KEY," +
                LocationEntry.COLUMN_JOURNEY_ID + " TEXT NOT NULL," +
                LocationEntry.COLUMN_TIME + " REAL NOT NULL," +
                LocationEntry.COLUMN_LATITUDE + " REAL NOT NULL," +
                LocationEntry.COLUMN_LONGITUDE + " REAL NOT NULL" +
                " );";
        Log.v("Location table creating",SQL_CREATE_LOCATION_TABLE.toString());
        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
        Log.v("Location table", "Create!");
    }

    private void createJourneyTable(SQLiteDatabase sqLiteDatabase){
        final String SQL_CREATE_JOURNEY_TABLE = "CREATE TABLE " +
                JourneyEntry.JOURNEY_TABLE + " (" +
                JourneyEntry._ID + " INTEGER PRIMARY KEY," +
                JourneyEntry.COLUMN_JOURNEY_ID + " TEXT NOT NULL," +
                JourneyEntry.COLUMN_JOURNEY_DATE + " TEXT NOT NULL," +
                JourneyEntry.COLUMN_JOURNEY_DURATION + " TEXT NOT NULL," +
                JourneyEntry.COLUMN_JOURNEY_DISTANCE + " REAL NOT NULL" +
                " );";
        Log.v("Journey table creating",SQL_CREATE_JOURNEY_TABLE.toString());
        sqLiteDatabase.execSQL(SQL_CREATE_JOURNEY_TABLE);
        Log.v("Journey table", "Create!");
    }




    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase){
        createLocationTable(sqLiteDatabase);
        createJourneyTable(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocationEntry.LOCATION_TABLE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + JourneyEntry.JOURNEY_TABLE);
        onCreate(sqLiteDatabase);
    }


}
