package com.bfbstudio.routetracking.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.bfbstudio.routetracking.data.JourneyContract.JourneyEntry;
/**
 * Created by Blues on 27/03/2017.
 */

public class JourneyProvider extends ContentProvider {
    private  static final UriMatcher sUriMatcher = buildUriMatcher();
    private JourneyDbHelper mOpenHelper;

    static final int LOCATION = 100;
    static final int JOURNEY = 200;
    static UriMatcher buildUriMatcher(){
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = JourneyContract.CONTENT_AUTHORITY;
        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, JourneyContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, JourneyContract.PATH_JOURNEY, JOURNEY);
        return matcher;
    }







    @Override
    public boolean onCreate(){
        mOpenHelper = new JourneyDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case LOCATION:
                return JourneyContract.LocationEntry.CONTENT_ITEM_TYPE;
            case JOURNEY:
                return JourneyContract.JourneyEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder){
        Cursor retCursor;

        switch (sUriMatcher.match(uri)){
            case LOCATION:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        JourneyContract.LocationEntry.LOCATION_TABLE,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case JOURNEY:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        JourneyContract.JourneyEntry.JOURNEY_TABLE,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch(match){
            case LOCATION: {
                long _id = db.insert(JourneyContract.LocationEntry.LOCATION_TABLE, null, values);
                if (_id > 0)
                    returnUri = JourneyContract.LocationEntry.buildMoneyUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }

            case JOURNEY: {
                long _id = db.insert(JourneyContract.JourneyEntry.JOURNEY_TABLE, null, values);
                if (_id > 0)
                    returnUri = JourneyContract.JourneyEntry.buildMoneyUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case LOCATION:
                rowsUpdated = db.update(JourneyContract.LocationEntry.LOCATION_TABLE, values, selection,
                        selectionArgs);
                break;
            case JOURNEY:
                rowsUpdated = db.update(JourneyContract.JourneyEntry.JOURNEY_TABLE, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        if (null == selection) selection = "1";
        switch(match)
        {
            case LOCATION:
                rowsDeleted = db.delete(
                        JourneyContract.LocationEntry.LOCATION_TABLE, selection, selectionArgs);
                break;
            case JOURNEY:
                rowsDeleted = db.delete(
                        JourneyContract.JourneyEntry.JOURNEY_TABLE, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

}
