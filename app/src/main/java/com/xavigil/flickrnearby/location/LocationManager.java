package com.xavigil.flickrnearby.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.xavigil.flickrnearby.BuildConfig;

/**
 * Requests the user location
 */
@SuppressWarnings("unused")
public class LocationManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static String TAG = "LocationManager";

    public static final String BROADCAST_ASK_PERMISSION = "com.xavigil.location.ask_permission";
    public static final String BROADCAST_LOCATION_CHANGED = "com.xavigil.location.br_location_changed";
    public static final String BROADCAST_LOCATION_ERROR = "com.xavigil.location.br_location_error";

    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_ERROR = "error";

    private static LocationManager sInstance;
    private Context mContext;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private LocationManager(){

    }

    public void init(Context context){
        if(context == null)
            throw new IllegalArgumentException("context can't be null");
        if(mContext != null)
            throw new IllegalStateException("LocationManager already initialised.");

        mContext = context.getApplicationContext();
        buildGoogleApiClient();
        buildLocationRequest();
        mGoogleApiClient.connect();
    }

    public static LocationManager get(){
        if(sInstance == null){
            sInstance = new LocationManager();
        }
        return sInstance;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected synchronized void buildLocationRequest(){
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setLastLocation(Location location){
        mLastLocation = location;
        if(BuildConfig.DEBUG) {
            if (mLastLocation != null) {
                Log.d(TAG, "last location = " + mLastLocation.toString());
            } else {
                Log.w(TAG, "last location is null");
            }
        }
        sendLocationChangedBroadcast(location);
    }

    public Location getLastLocation(){
        return mLastLocation;
    }

    public void updateLocation(){
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        setLastLocation(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
        if(mLastLocation == null)
        {
            int locationPermission = ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            if(locationPermission == PackageManager.PERMISSION_GRANTED){
                updateLocation();
            }
            else{
                sendAskPermissionBroadcast();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorMessage());
        sendErrorBroadcast(connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {
        setLastLocation(location);
    }

    private void sendAskPermissionBroadcast()
    {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ASK_PERMISSION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        mContext.sendBroadcast(intent);
    }

    private void sendLocationChangedBroadcast(Location location)
    {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_LOCATION_CHANGED);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("location",location);
        mContext.sendBroadcast(intent);
    }

    private void sendErrorBroadcast(String error)
    {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_LOCATION_ERROR);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("error",error);
        mContext.sendBroadcast(intent);
    }


}
