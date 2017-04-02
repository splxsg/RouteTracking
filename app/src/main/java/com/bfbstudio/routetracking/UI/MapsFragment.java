package com.bfbstudio.routetracking.UI;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.DecorToolbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bfbstudio.routetracking.R;
import com.bfbstudio.routetracking.RecycleAdapter.JourneyRecycleAdapter;
import com.bfbstudio.routetracking.data.JourneyContract;
import com.bfbstudio.routetracking.data.JourneyQuery;
import com.bfbstudio.routetracking.rest.CustomSharedPreference;
import com.bfbstudio.routetracking.rest.RecyclerViewItemClickListener;
import com.bfbstudio.routetracking.rest.Utility;
import com.bfbstudio.routetracking.service.TrackingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.text.Text;

import java.sql.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import android.os.Handler;

/**
 * Created by Blues on 30/03/2017.
 */

public class MapsFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{


    private static final String TAG = MapsFragment.class.getSimpleName();
    private JourneyQuery query;
    private TrackingBroadCastReceiver trackingBroadCastReceiver;
    private GoogleApiClient mGoogleApiClient;
    private BottomSheetDialog mBottomSheetDialog;
    private BottomSheetBehavior mBottomSheetBehavior;
    private PolylineOptions mPolyLineOption;
    private Polyline mPolyline;
    private View rootView;
    private View bottomSheetView;
    private boolean mLocationPermissionGranted;
    private Intent mServiceIntent;
    private GoogleMap mMap;
    private MapView mapView;
    private static final int DEFAULT_ZOOM = 16;
    private static final int MAX_ZOOM = 19;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int CURRENT_POSITION_ZOOM = 1;
    private static final int FULL_LOCATION_ZOOM = 2;
    private CameraPosition mCameraPosition;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private Location mLastKnownLocation;
    private String mLastJourneyId;
    private String mCurrentJourneyId;
    private static final String KEY_JOURNEY_ID = "KEY_JOURNEY_ID";
    private JourneyRecycleAdapter mJourneyAdapter;

    private static final int VIEW_NORMAL_KEY = 1;
    private static final int VIEW_TRACKING_KEY = 2;
    private static final int VIEW_JOURNEY_KEY = 3;
    private int viewStatus = VIEW_NORMAL_KEY;
    private ImageButton trackingOnBtn, trackingOffBtn, historyDisplayBtn, menuBtn, settingBtn,  journeyViewOffBtn;
    private TextView trackingStatusTv, journeyInfoTv;
    ProgressDialog progressDialog;
    private AsyncQueryHandler async;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("oncreate","creat");

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity() /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            //savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
            // savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
            outState.putString(KEY_JOURNEY_ID, mCurrentJourneyId);
            super.onSaveInstanceState(outState);
          //  outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
          //  outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        rootView = inflater.inflate(R.layout.activity_maps, container, false);
        trackingOnBtn  = ((ImageButton)  rootView.findViewById(R.id.imagebtn_tracking_start));
        historyDisplayBtn = ((ImageButton)  rootView.findViewById(R.id.imagebtn_history));
        menuBtn = ((ImageButton)  rootView.findViewById(R.id.imagebtn_popmenu));
        settingBtn = ((ImageButton)  rootView.findViewById(R.id.imagebtn_setting));
        journeyViewOffBtn = ((ImageButton)  rootView.findViewById(R.id.imagebtn_journey_end));
        trackingOffBtn = ((ImageButton)  rootView.findViewById(R.id.imagebtn_tracking_end));
        trackingStatusTv = ((TextView) rootView.findViewById(R.id.tv_tracking_status));
        journeyInfoTv = ((TextView) rootView.findViewById(R.id.tv_journey_infomation));
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        query = new JourneyQuery(getActivity());
        trackingBroadCastReceiver = new TrackingBroadCastReceiver();
        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mServiceIntent = new Intent(getContext(), TrackingService.class);
        View bottomSheet =  rootView.findViewById(R.id.bottom_sheet);





        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
            }
            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });


         menuBtn.setOnClickListener(new View.OnClickListener(){
             @Override
             public void onClick(View v){
                 if(menuBtn.getRotation()==360f)
                     imagebtnAnimationPopback();
                 else
                 imagebtnAnimationPopout();
             }
         });

        settingBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                SettingDialogFragment settingDialogFragment = new SettingDialogFragment();
                settingDialogFragment.show(getFragmentManager().beginTransaction(),"setting");
            }
        });


       trackingOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (!CustomSharedPreference.getServiceState()) {

                   startTracking();
              //  } //else
                    //endTracking();
            }
        });

        trackingOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (!CustomSharedPreference.getServiceState()) {
                endTracking();

            }
        });


        historyDisplayBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                imagebtnAnimationPopback();
                showBottomSheetDialog();
                viewStatus = VIEW_JOURNEY_KEY;
            }
        });

        journeyViewOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (!CustomSharedPreference.getServiceState()) {
                endViewJourney();

            }
        });




        return rootView;
    }

    private void imagebtnAnimationPopout()
    {

        menuBtn.animate().setStartDelay(0).setDuration(300).rotation(360f);
        trackingOnBtn.animate().setStartDelay(0).translationX(300).setDuration(100);
        historyDisplayBtn.animate().setStartDelay(0).translationX(600).setDuration(200);
        settingBtn.animate().translationX(900).setStartDelay(0).setDuration(300);
    }

    private void imagebtnAnimationPopback()
    {
        menuBtn.animate().setDuration(300).rotation(0);
        settingBtn.animate().translationX(0).setDuration(300);
        historyDisplayBtn.animate().translationX(0).setStartDelay(100).setDuration(200);
        trackingOnBtn.animate().translationX(0).setStartDelay(200).setDuration(100);



    }

    private void startViewJourney()
    {
        initPolyline();
        imagebtnAnimationPopback();
        menuBtnSlideIn();
        journeyViewOffBtn.animate().translationY(-400).setStartDelay(800).setDuration(500);
        journeyInfoTv.setText(query.getJourneyInfo(mCurrentJourneyId));
        journeyInfoTv.animate().translationY(journeyInfoTv.getHeight()).setStartDelay(800).setDuration(500);
        //journey information tv
          // ****
        viewStatus = VIEW_JOURNEY_KEY;
    }

    private void endViewJourney()
    {
        refreshMap(mMap);
        menuBtnSlideOut();
        journeyViewOffBtn.animate().setStartDelay(0).translationY(0);
        journeyInfoTv.setText(null);
        mCurrentJourneyId = null;
        journeyInfoTv.animate().setStartDelay(0).translationY(0);
        viewStatus = VIEW_NORMAL_KEY;
        getDeviceLocation();
    }

    private void startTracking()
    {
        initPolyline();
        CustomSharedPreference.setServiceState(true);
        getContext().startService(mServiceIntent);
        imagebtnAnimationPopback();
        menuBtnSlideIn();
        trackingOffBtn.animate().translationY(-400).setStartDelay(800).setDuration(500);
        trackingStatusTv.animate().setStartDelay(800).translationY(trackingStatusTv.getHeight());
        viewStatus = VIEW_TRACKING_KEY;
        followCurrentPosition(MAX_ZOOM);
    }

    private void endTracking()
    {
        Uri inserteduri;
        getContext().stopService(mServiceIntent);
        inserteduri = query.insertNewJourneyRecord(
                mCurrentJourneyId,
                System.currentTimeMillis()-Long.parseLong(mCurrentJourneyId)+"",
                query.calculateDistance(mCurrentJourneyId));
        refreshMap(mMap);
        CustomSharedPreference.setServiceState(false);

        trackingOffBtn.animate().setStartDelay(0).translationY(0);
        trackingStatusTv.animate().translationY(0);
        startViewJourney();
        redrawPath(FULL_LOCATION_ZOOM);
    }

    private void menuBtnSlideOut(){
        menuBtn.animate().translationX(0);
        historyDisplayBtn.animate().translationX(0);
        trackingOnBtn.animate().translationX(0);
        settingBtn.animate().translationX(0);
    }

    private void menuBtnSlideIn()
    {
        menuBtn.animate().translationX(-900).setDuration(300).setStartDelay(300);
        historyDisplayBtn.animate().translationX(-900).setDuration(300).setStartDelay(300);
        trackingOnBtn.animate().translationX(-900).setDuration(300).setStartDelay(300);
        settingBtn.animate().translationX(-900).setDuration(300).setStartDelay(300);
    }



    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        mapView.getMapAsync(this);
    }


    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(viewStatus == VIEW_NORMAL_KEY || viewStatus == VIEW_TRACKING_KEY)
        getDeviceLocation();
        updateLocationUI();
        Log.d("maponready","   ");
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng arg0)
            {
                if(menuBtn.getRotation()==360f)
                imagebtnAnimationPopback();
            }
        });
        switch(viewStatus)
        {
            case VIEW_NORMAL_KEY: {
                if (mMap != null)
                    getDeviceLocation();
                imagebtnAnimationPopback();
            }
            break;
            case VIEW_TRACKING_KEY:
                redrawPath(CURRENT_POSITION_ZOOM);
            break;
            case VIEW_JOURNEY_KEY:
                redrawPath(FULL_LOCATION_ZOOM);
            break;

        }
    }


    private void moveCameraBounds(LatLngBounds bounds)
    {
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,200));
    }

    private void getDeviceLocation() {
        /**
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
           {
                if (ContextCompat.checkSelfPermission(this.getContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }

        /* Get the best and most recent location of the device, which may be null in rare
        / * cases when a location is not available.
         */
                             if (mLocationPermissionGranted) {
                   mLastKnownLocation = LocationServices.FusedLocationApi
                           .getLastLocation(mGoogleApiClient);
               }

                // Set the map's camera position to the current location of the device.

               if (mCameraPosition != null) {

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
                } else if (mLastKnownLocation != null) {

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                } else {

                    Log.d(TAG, "Current location is null. Using defaults.");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            }

        }

        public void followCurrentPosition(float zoom)
        {

            if (ContextCompat.checkSelfPermission(this.getContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    mLastKnownLocation= LocationServices.FusedLocationApi
                            .getLastLocation(mGoogleApiClient);
                Log.d("getlastlocation","1");
                if (mLastKnownLocation != null) {
                    Log.d("getlastlocation","2");
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), zoom));
                }

            }
        }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    private void turnoffMyLocation(){
        if (mMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(false);
        }

    }



    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void refreshMap(GoogleMap mapInstance) {
        if(mapInstance != null)
            mapInstance.clear();
    }


    private void redrawPath(int zoomtype)
    {
        initPolyline();
        final int zoomType = zoomtype;
        async = new AsyncQueryHandler(getActivity().getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor mCursor) {
                super.onQueryComplete(token, cookie, mCursor);
                new drawJourneyAsyncTask(mCursor,zoomType).execute();
            }
        };
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching location data, please wait...");
        progressDialog.setIndeterminate(false);
        // progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        if(mMap != null && mCurrentJourneyId!=null){
            async.startQuery(0,null,JourneyContract.LocationEntry.CONTENT_URI,
                    null,
                    JourneyContract.LocationEntry.COLUMN_JOURNEY_ID + "=?",
                    new String[]{mCurrentJourneyId},
                    null);
        }


       /* if(mMap != null && journeyId!=null) {
            initPolyline();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            Cursor mCursor = getContext().getContentResolver().query(JourneyContract.LocationEntry.CONTENT_URI,
                    null,
                    JourneyContract.LocationEntry.COLUMN_JOURNEY_ID + "=?",
                    new String[]{journeyId},
                    null);
            if (mCursor.moveToFirst())
                while (!mCursor.isAfterLast()) {
                    mMap.addPolyline(mPolyLineOption.add(query.getLatLng(mCursor)));
                    builder.include(query.getLatLng(mCursor));
                    mCursor.moveToNext();
                }
                LatLngBounds bounds = builder.build();
                switch (zoomtype)
                {
                    case CURRENT_POSITION_ZOOM:
                        getDeviceLocation();
                    case FULL_LOCATION_ZOOM:
                        moveCameraBounds(bounds);
                }
        }*/
    }

    private void initPolyline(){
        refreshMap(mMap);
        mPolyLineOption = new PolylineOptions().color(COLOR_GREEN_ARGB).width(20);
    }


    private void showBottomSheetDialog() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        mBottomSheetDialog = new BottomSheetDialog(getContext());

        bottomSheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_view, null);

        RecyclerView recyclerView = (RecyclerView) bottomSheetView.findViewById(R.id.journey_cardview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mJourneyAdapter = new JourneyRecycleAdapter(getContext(),null);
        //recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(mJourneyAdapter);

        mBottomSheetDialog.setContentView(bottomSheetView);
        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(getContext(),
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (position != -1) {
                             Cursor mCursor = query.getCursor(JourneyContract.JourneyEntry.CONTENT_URI);
                            mCursor.moveToPosition(position);
                            mBottomSheetDialog.dismiss();
                            mCurrentJourneyId = mCursor.getString(mCursor.getColumnIndex(JourneyContract.JourneyEntry.COLUMN_JOURNEY_ID));
                            redrawPath(FULL_LOCATION_ZOOM);
                            startViewJourney();
                             }
                    }
                }));
    }

    private class TrackingBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("broadcast","received");
            String local = intent.getExtras().getString("RESULT_CODE");
            assert local != null;
            if (mMap != null &&local.equals("LOCAL")) {
                mCurrentJourneyId = intent.getExtras().getString("JourneyId");
                drawPath(intent.getExtras().getDouble("CURRENT_LATITUDE"),intent.getExtras().getDouble("CURRENT_LONGITUDE"));
                followCurrentPosition(MAX_ZOOM);
            }

        }
    }

    @Override
    public void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()){ //&& mRequestingLocationUpdates) {

        }
        if(trackingBroadCastReceiver == null){
            trackingBroadCastReceiver = new TrackingBroadCastReceiver();
            Log.v("get new service","get new service");
        }
        IntentFilter filter = new IntentFilter(TrackingService.ACTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(trackingBroadCastReceiver, filter);
    }


    @Override
    public void onStop() {
        super.onStop();
     //       mGoogleApiClient.disconnect();
        turnoffMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(trackingBroadCastReceiver);
    }

    private void drawPath(double latitude, double longitude){
        Log.d(TAG, "draw path on "+latitude+" "+longitude);
        mMap.addPolyline(mPolyLineOption.add(new LatLng(latitude, longitude)));
        Log.d(TAG, "draw path on "+latitude+" "+longitude);
    }


private class drawJourneyAsyncTask extends AsyncTask<Void,Void, List<LatLng>>{
    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    Cursor mCursor;
    int zoomType;
    drawJourneyAsyncTask(Cursor mCursor, int zoomType){this.mCursor = mCursor; this.zoomType = zoomType;}


    @Override
    protected List<LatLng> doInBackground(Void... params) {
        List<LatLng> mMaplocations = new ArrayList<>();


        if (mMap != null ) {
          ;
            if(mCursor != null)
            {
                if (mCursor.moveToFirst())
                    while (!mCursor.isAfterLast()) {
                        mMaplocations.add(query.getLatLng(mCursor));
                        mCursor.moveToNext();
                    }
            }
        }
        return mMaplocations;
    }

    @Override
    protected void onPostExecute(List<LatLng> mMapLocations)
    {

        super.onPostExecute(mMapLocations);
        Cursor cursor;
        initPolyline();
        for(LatLng mMapLocation : mMapLocations)
        {
            mMap.addPolyline(mPolyLineOption.add(mMapLocation));
            builder.include(mMapLocation);
        }
        LatLngBounds bounds = builder.build();
       //
        progressDialog.hide();

        switch (zoomType)
        {
            case CURRENT_POSITION_ZOOM:
                followCurrentPosition(MAX_ZOOM);
            case FULL_LOCATION_ZOOM:
                moveCameraBounds(bounds);
        }



    }
}
}
