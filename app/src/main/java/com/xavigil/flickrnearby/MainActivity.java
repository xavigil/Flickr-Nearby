package com.xavigil.flickrnearby;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.xavigil.flickrnearby.location.LocationManager;
import com.xavigil.flickrnearby.model.PhotosResponse;
import com.xavigil.flickrnearby.server.FlickrAPI;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_ASK_PERMISSION = 1;

    private IntentFilter mLocationAskPermissionIntentFilter;
    private IntentFilter mLocationChangedIntentFilter;
    private IntentFilter mLocationErrorIntentFilter;

    private Retrofit mRetrofit;
    private int mPageCount = 1;

    //region Broadcast receivers

    private BroadcastReceiver mLocationManagerAskPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{permission}, REQUEST_CODE_ASK_PERMISSION);
        }
    };
    private BroadcastReceiver mLocationChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = (Location)intent.getExtras().get(LocationManager.EXTRA_LOCATION);
            if(location != null) {
                Toast.makeText(MainActivity.this, "" + location.toString(), Toast.LENGTH_SHORT).show();
                MainActivity.this.requestPhotos(location, mPageCount++);
            }
        }
    };
    private BroadcastReceiver mLocationManagerErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String error = intent.getExtras().getString(LocationManager.EXTRA_ERROR);
            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
        }
    };

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupAPI();
        setupIntentFilters();
        LocationManager.get().init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mLocationManagerAskPermissionReceiver, mLocationAskPermissionIntentFilter);
        registerReceiver(mLocationChangedReceiver, mLocationChangedIntentFilter);
        registerReceiver(mLocationManagerErrorReceiver, mLocationErrorIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mLocationManagerAskPermissionReceiver);
        unregisterReceiver(mLocationChangedReceiver);
        unregisterReceiver(mLocationManagerErrorReceiver);
    }

    private void setupAPI(){
        mRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private void setupIntentFilters(){
        mLocationAskPermissionIntentFilter = new IntentFilter();
        mLocationAskPermissionIntentFilter.addAction(LocationManager.BROADCAST_ASK_PERMISSION);
        mLocationAskPermissionIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mLocationChangedIntentFilter = new IntentFilter();
        mLocationChangedIntentFilter.addAction(LocationManager.BROADCAST_LOCATION_CHANGED);
        mLocationChangedIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mLocationErrorIntentFilter = new IntentFilter();
        mLocationErrorIntentFilter.addAction(LocationManager.BROADCAST_LOCATION_ERROR);
        mLocationErrorIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    LocationManager.get().updateLocation();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Location denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void requestPhotos(Location location, int page){
        Call<PhotosResponse> call = mRetrofit.create(FlickrAPI.class).getPhotos(
                "flickr.photos.search",
                BuildConfig.API_KEY,
                "json",
                "1",
                String.valueOf(location.getLatitude()),
                String.valueOf(location.getLongitude()),
                "url_n,url_z",
                String.valueOf(page));

        call.enqueue(new Callback<PhotosResponse>() {
            @Override
            public void onResponse(Response<PhotosResponse> response, Retrofit retrofit) {
                Log.d(TAG, "request " + response.raw().request().toString() );
                Log.d(TAG, "received " + response.body().photos.total + " photos");
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

}
