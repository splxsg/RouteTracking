package com.bfbstudio.routetracking.rest;

import android.content.Context;

/**
 * Created by Blues on 28/03/2017.
 */

public class CustomSharedPreference {

    private static boolean serviceStatus = false;

    private static String journeySessionid;

    private Context mContext;

    public CustomSharedPreference(Context mContext){this.mContext = mContext;}

    public static void setServiceState(boolean b){serviceStatus = b;}

    public static boolean getServiceState(){return serviceStatus;}

   // public static void setJourneySessionId(String id){journeySessionid = id;}

   // public static String getJourneySessionId(){return journeySessionid;}



}
