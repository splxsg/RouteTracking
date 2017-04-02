package com.bfbstudio.routetracking.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.bfbstudio.routetracking.R;
import com.bfbstudio.routetracking.data.JourneyQuery;
import com.bfbstudio.routetracking.rest.CustomSharedPreference;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import static android.content.ContentValues.TAG;

/**
 * Created by Blues on 28/03/2017.
 */

public class TrackingService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private LocationRequest mLocationRequest;
    private boolean isServiceRunning = false;
    private GoogleApiClient mGoogleApiClient;
    private long startTimeInMilliSeconds = 0L;
    private JourneyQuery query;
    private double latitude = 0.0;
    private double longitude = 0.0;
    public static final String ACTION = "com.bfbstudio.map_v2.service.TrackingService";
    private Location mLastKnownLocation;
    private String journeyId;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TrackingService.class.getSimpleName(),"Tracking Intent Service");
        if(isRouteTrackingOn()) {startTimeInMilliSeconds = System.currentTimeMillis();
            Log.d(TAG, "Current time " + startTimeInMilliSeconds);
            Log.d(TAG, "Service is running");}


        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
        query = new JourneyQuery(getApplicationContext());
        mLocationRequest = createLocationRequest();
        journeyId = startTimeInMilliSeconds+"";
       // Toast.makeText(getApplicationContext(), getString(R.string.service_started), Toast.LENGTH_SHORT).show();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        isServiceRunning = true;
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connection method has been called");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult( LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastKnownLocation != null) {
                                latitude = mLastKnownLocation.getLatitude();
                                longitude = mLastKnownLocation.getLongitude();
                                Log.d(TAG, "Latitude 1: " + latitude + " Longitude 1: " + longitude);
                                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, TrackingService.this);
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }



    @Override
    public void onConnectionSuspended(int i) {
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Latitude " + location.getLatitude() + " Longitude " + location.getLongitude());
        Log.d(TAG, "SERVICE RUNNING " + isServiceRunning);
        if(isRouteTrackingOn() && startTimeInMilliSeconds == 0){
            startTimeInMilliSeconds = System.currentTimeMillis();
        }
        if(isRouteTrackingOn() && startTimeInMilliSeconds > 0){
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d(TAG, "Latitude " + latitude + " Longitude " + longitude);
            // insert values to local sqlite database
            query.addNewLocationObject(journeyId,System.currentTimeMillis(), latitude, longitude);
            // send local broadcast receiver to application components
            Intent localBroadcastIntent = new Intent(ACTION);
            localBroadcastIntent.putExtra("RESULT_CODE", "LOCAL");
            localBroadcastIntent.putExtra("JourneyId",journeyId);
            localBroadcastIntent.putExtra("CURRENT_LATITUDE",latitude);
            localBroadcastIntent.putExtra("CURRENT_LONGITUDE",longitude);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localBroadcastIntent);
            long timeoutTracking = 2 * 60 * 60 * 1000;
            if(System.currentTimeMillis() >= startTimeInMilliSeconds + timeoutTracking){
                //turn of the tracking
                CustomSharedPreference.setServiceState(false);
                Log.d(TAG, "SERVICE HAS BEEN STOPPED");
                this.stopSelf();
            }
        }
        if(!isRouteTrackingOn()){
            Log.d(TAG, "SERVICE HAS BEEN STOPPED 1");
            isServiceRunning = false;
            Log.d(TAG, "SERVICE STOPPED " + isServiceRunning);
            this.stopSelf();
        }
    }
    private boolean isRouteTrackingOn(){
        Log.d(TAG, "SERVICE STATE " + CustomSharedPreference.getServiceState());
        return CustomSharedPreference.getServiceState();
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }



}