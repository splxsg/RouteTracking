package com.bfbstudio.routetracking.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Blues on 27/03/2017.
 */

public class JourneyContract {
    public static final String CONTENT_AUTHORITY = "com.bfbstudio.routetracking";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+CONTENT_AUTHORITY);

    public static final String PATH_LOCATION = "location";
    public static final String PATH_JOURNEY = "journey";


    public static final class LocationEntry implements BaseColumns{
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String LOCATION_TABLE = "location";
        public static final String COLUMN_JOURNEY_ID = "journey_id";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_LATITUDE = "latitude" ;
        public static final String COLUMN_LONGITUDE = "longitude";
        public static Uri buildMoneyUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI,id);
        }
    }

    public static final class JourneyEntry implements BaseColumns{
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_JOURNEY).build();
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String JOURNEY_TABLE = "journey";
        public static final String COLUMN_JOURNEY_ID = "journey_id";
        public static final String COLUMN_JOURNEY_DATE = "date";
        public static final String COLUMN_JOURNEY_DURATION = "duration";
        public static final String COLUMN_JOURNEY_DISTANCE = "distance" ;
        public static Uri buildMoneyUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI,id);
        }
    }
}
