package com.bfbstudio.routetracking.UI;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bfbstudio.routetracking.R;
import com.bfbstudio.routetracking.RecycleAdapter.JourneyRecycleAdapter;
import com.bfbstudio.routetracking.data.JourneyContract;
import com.bfbstudio.routetracking.data.JourneyQuery;
import com.bfbstudio.routetracking.rest.CustomSharedPreference;
import com.bfbstudio.routetracking.rest.RecyclerViewItemClickListener;
import com.bfbstudio.routetracking.service.TrackingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Blues on 30/03/2017.
 */

public class MapsFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = MapsFragment.class.getSimpleName();
    private static final int DEFAULT_ZOOM = 16;
    private static final int MAX_ZOOM = 19;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int CURRENT_POSITION_ZOOM = 1;
    private static final int FULL_LOCATION_ZOOM = 2;
    private static final String KEY_JOURNEY_ID = "KEY_JOURNEY_ID";
    private static final String KEY_VIEW_STATUS = "KEY_VIEW_STATUS";
    private static final int VIEW_NORMAL_KEY = 1;
    private static final int VIEW_TRACKING_KEY = 2;
    private static final int VIEW_JOURNEY_KEY = 3;
    private static final int VIEW_DEFAULT_KEY = -1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    ProgressDialog progressDialog;
    private JourneyQuery query;
    private TrackingBroadCastReceiver trackingBroadCastReceiver;
    private GoogleApiClient mGoogleApiClient;
    private BottomSheetDialog mBottomSheetDialog;
    private BottomSheetBehavior mBottomSheetBehavior;
    private PolylineOptions mPolyLineOption;
    private boolean mLocationPermissionGranted;
    private Intent mServiceIntent;
    private GoogleMap mMap;
    private MapView mapView;
    private CameraPosition mCameraPosition;
    private Location mLastKnownLocation;
    private String mCurrentJourneyId;
    private int viewStatus;
    private ImageButton trackingOnBtn, trackingOffBtn, historyDisplayBtn, menuBtn, settingBtn, journeyViewOffBtn;
    private TextView trackingStatusTv, journeyInfoTv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewStatus = VIEW_DEFAULT_KEY;
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity() /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        if (savedInstanceState != null) {
            mCurrentJourneyId = savedInstanceState.getParcelable(KEY_JOURNEY_ID);
            viewStatus = savedInstanceState.getInt(KEY_VIEW_STATUS);
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
        outState.putString(KEY_JOURNEY_ID, mCurrentJourneyId);
        outState.putInt(KEY_VIEW_STATUS, viewStatus);
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView;
        rootView = inflater.inflate(R.layout.activity_maps, container, false);
        trackingOnBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_tracking_start));
        historyDisplayBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_history));
        menuBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_popmenu));
        settingBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_setting));
        journeyViewOffBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_journey_end));
        trackingOffBtn = ((ImageButton) rootView.findViewById(R.id.imagebtn_tracking_end));
        trackingStatusTv = ((TextView) rootView.findViewById(R.id.tv_tracking_status));
        journeyInfoTv = ((TextView) rootView.findViewById(R.id.tv_journey_infomation));
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        query = new JourneyQuery(getActivity());
        trackingBroadCastReceiver = new TrackingBroadCastReceiver();
        mapView = (MapView) rootView.findViewById(R.id.map);

        mapView.onCreate(savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null);
        mapView.onResume();
        mServiceIntent = new Intent(getContext(), TrackingService.class);

        View bottomSheet = rootView.findViewById(R.id.bottom_sheet);

        if (viewStatus == -1)
            viewStatus = VIEW_NORMAL_KEY;

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });


        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuBtn.getRotation() == 360f)
                    imagebtnAnimationPopback();
                else
                    imagebtnAnimationPopout();
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingDialogFragment settingDialogFragment = new SettingDialogFragment();
                settingDialogFragment.show(getFragmentManager().beginTransaction(), "setting");
            }
        });

        trackingOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        trackingOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().stopService(mServiceIntent);
                query.insertNewJourneyRecord(
                        mCurrentJourneyId,
                        System.currentTimeMillis() - Long.parseLong(mCurrentJourneyId) + "",
                        query.calculateDistance(mCurrentJourneyId));
                refreshMap(mMap);
                CustomSharedPreference.setServiceState(false);

                trackingOffBtn.animate().setStartDelay(0).translationY(0);
                trackingStatusTv.animate().translationY(0);
                startViewJourney();
                redrawPath(FULL_LOCATION_ZOOM);

            }
        });

        historyDisplayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagebtnAnimationPopback();
                showBottomSheetDialog();

            }
        });

        journeyViewOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshMap(mMap);
                menuBtnSlideOut();
                journeyViewOffBtn.animate().setStartDelay(0).translationY(0);
                journeyInfoTv.setText(null);
                mCurrentJourneyId = null;
                journeyInfoTv.animate().setStartDelay(0).translationY(0);
                viewStatus = VIEW_NORMAL_KEY;
                getDeviceLocation();
            }
        });
        return rootView;
    }

    private void imagebtnAnimationPopout() {

        menuBtn.animate().setStartDelay(0).setDuration(300).rotation(360f);
        trackingOnBtn.animate().setStartDelay(0).translationX(300).setDuration(100);
        historyDisplayBtn.animate().setStartDelay(0).translationX(600).setDuration(200);
        settingBtn.animate().translationX(900).setStartDelay(0).setDuration(300);
    }

    private void imagebtnAnimationPopback() {
        menuBtn.animate().setDuration(300).rotation(0);
        settingBtn.animate().translationX(0).setDuration(300);
        historyDisplayBtn.animate().translationX(0).setStartDelay(100).setDuration(200);
        trackingOnBtn.animate().translationX(0).setStartDelay(200).setDuration(100);


    }

    private void startViewJourney() {
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


    private void menuBtnSlideOut() {
        menuBtn.animate().translationX(0);
        historyDisplayBtn.animate().translationX(0);
        trackingOnBtn.animate().translationX(0);
        settingBtn.animate().translationX(0);
    }

    private void menuBtnSlideIn() {
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (viewStatus == VIEW_NORMAL_KEY || viewStatus == VIEW_TRACKING_KEY)
            getDeviceLocation();
        updateLocationUI();
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                if (menuBtn.getRotation() == 360f)
                    imagebtnAnimationPopback();
            }
        });
        switch (viewStatus) {
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


    private void moveCameraBounds(LatLngBounds bounds) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
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

    public void followCurrentPosition(float zoom) {

        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            if (mLastKnownLocation != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(mLastKnownLocation.getLatitude(),
                                mLastKnownLocation.getLongitude()), zoom));
            }

        }
    }


    private void turnoffMyLocation() {
        if (mMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
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
        if (mapInstance != null)
            mapInstance.clear();
    }


    private void redrawPath(int zoomtype) {
        initPolyline();
        AsyncQueryHandler async;
        final int zoomType = zoomtype;
        async = new AsyncQueryHandler(getActivity().getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor mCursor) {
                super.onQueryComplete(token, cookie, mCursor);
                new drawJourneyAsyncTask(mCursor, zoomType).execute();
            }
        };
        if (mMap != null && mCurrentJourneyId != null) {
            async.startQuery(0, null, JourneyContract.LocationEntry.CONTENT_URI,
                    null,
                    JourneyContract.LocationEntry.COLUMN_JOURNEY_ID + "=?",
                    new String[]{mCurrentJourneyId},
                    null);
        }

    }

    private void initPolyline() {
        refreshMap(mMap);
        mPolyLineOption = new PolylineOptions().color(COLOR_GREEN_ARGB).width(20);
    }


    private void showBottomSheetDialog() {

        if (getActivity().getContentResolver().query(
                JourneyContract.JourneyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        ).getCount() == 0) {
            Toast.makeText(getContext(), R.string.bottom_sheet_no_journey_toast, Toast.LENGTH_SHORT).show();
        } else {

            if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            View bottomSheetView;
            JourneyRecycleAdapter mJourneyAdapter;
            mBottomSheetDialog = new BottomSheetDialog(getContext());

            bottomSheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_view, null);

            RecyclerView recyclerView = (RecyclerView) bottomSheetView.findViewById(R.id.journey_cardview);
            recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
            mJourneyAdapter = new JourneyRecycleAdapter(getContext());
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
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) { //&& mRequestingLocationUpdates) {
            if (mMap != null)
                getDeviceLocation();
            updateLocationUI();
        }
        if (trackingBroadCastReceiver == null) {
            trackingBroadCastReceiver = new TrackingBroadCastReceiver();
            Log.v("get new service", "get new service");
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

    @Override
    public void onDestroy() {
        if (CustomSharedPreference.getServiceState()) {
            getContext().stopService(mServiceIntent);
            query.insertNewJourneyRecord(
                    mCurrentJourneyId,
                    System.currentTimeMillis() - Long.parseLong(mCurrentJourneyId) + "",
                    query.calculateDistance(mCurrentJourneyId));
        }
        super.onDestroy();
    }

    private void drawPath(double latitude, double longitude) {
        Log.d(TAG, "draw path on " + latitude + " " + longitude);
        mMap.addPolyline(mPolyLineOption.add(new LatLng(latitude, longitude)));
        Log.d(TAG, "draw path on " + latitude + " " + longitude);
    }

    private class TrackingBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("broadcast", "received");
            String local = intent.getExtras().getString("RESULT_CODE");
            assert local != null;
            if (mMap != null && local.equals("LOCAL")) {
                mCurrentJourneyId = intent.getExtras().getString("JourneyId");
                drawPath(intent.getExtras().getDouble("CURRENT_LATITUDE"), intent.getExtras().getDouble("CURRENT_LONGITUDE"));
                followCurrentPosition(MAX_ZOOM);
            }
        }
    }

    private class drawJourneyAsyncTask extends AsyncTask<Void, Void, List<LatLng>> {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        Cursor mCursor;
        int zoomType;

        drawJourneyAsyncTask(Cursor mCursor, int zoomType) {
            this.mCursor = mCursor;
            this.zoomType = zoomType;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Fetching location data, please wait...");
            progressDialog.setIndeterminate(false);
            // progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }


        @Override
        protected List<LatLng> doInBackground(Void... params) {
            List<LatLng> mMaplocations = new ArrayList<>();


            if (mMap != null) {
                ;
                if (mCursor != null) {
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
        protected void onPostExecute(List<LatLng> mMapLocations) {

            super.onPostExecute(mMapLocations);
            initPolyline();
            for (LatLng mMapLocation : mMapLocations) {
                mMap.addPolyline(mPolyLineOption.add(mMapLocation));
                builder.include(mMapLocation);
            }
            LatLngBounds bounds = builder.build();
            progressDialog.hide();
            switch (zoomType) {
                case CURRENT_POSITION_ZOOM:
                    followCurrentPosition(MAX_ZOOM);
                case FULL_LOCATION_ZOOM:
                    moveCameraBounds(bounds);
            }


        }
    }
}
