package com.xavigil.flickrnearby.activity;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.xavigil.flickrnearby.R;
import com.xavigil.flickrnearby.location.LocationManager;
import com.xavigil.flickrnearby.model.Photo;

import java.util.ArrayList;

public class PhotoDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_PHOTO = "photo";

    private RelativeLayout mRootLayout;
    private FrameLayout mMapContainer;
    private FrameLayout mMapClickListener;
    private GoogleMap mMap;
    private boolean mIsMapExpanded = false;
    private int mOriginalMapHeight;

    private Photo mPhoto;
    private LatLng mPhotoCoord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_details);

        ActionBar ab = getSupportActionBar();
        setupActionBar(ab);

        ImageView iv = (ImageView)findViewById(R.id.photo);
        setupPhoto(iv);

        mMapContainer = (FrameLayout)findViewById(R.id.mapContainer);
        mMapClickListener = (FrameLayout)findViewById(R.id.mapClickListener);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRootLayout = (RelativeLayout)findViewById(R.id.rootContainer);

        setupLayoutColors(ab);
        setupPhotoDetails();

        LayoutTransition transition = mRootLayout.getLayoutTransition();
        setupTransition(transition);

    }
    private void setupPhoto(ImageView iv){
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            Photo p = extras.getParcelable(EXTRA_PHOTO);
            if(p != null && iv != null){
                mPhoto = p;
                mPhotoCoord = new LatLng(mPhoto.latitude,mPhoto.longitude);
                Picasso.with(this)
                        .load(p.url_z)
                        .into(iv);
            }
        }
    }

    private void setupActionBar(ActionBar ab){
        if(ab!=null) {
            ab.setTitle("");
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupLayoutColors(ActionBar ab){
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            int colorBg = extras.getInt("colorBg");
            if(colorBg!=0) {
                if(ab!=null)
                    ab.setBackgroundDrawable(new ColorDrawable(colorBg));
                mRootLayout.setBackgroundColor(colorBg);
            }
            int colorText = extras.getInt("colorTxt");
            if(colorText!=0) {
                TextView txt = (TextView) findViewById(R.id.txtMap);
                txt.setTextColor(colorText);
                txt = (TextView) findViewById(R.id.txtInfo);
                txt.setTextColor(colorText);
                ImageView img = (ImageView) findViewById(R.id.imgMap);
                img.setColorFilter(colorText);
                img = (ImageView) findViewById(R.id.imgInfo);
                img.setColorFilter(colorText);
            }
        }
    }

    private void setupPhotoDetails(){
        TextView txt = (TextView)findViewById(R.id.txtTitle);
        txt.setText(mPhoto.title);
        txt = (TextView)findViewById(R.id.txtOwner);
        txt.setText(mPhoto.ownername);
        txt = (TextView)findViewById(R.id.txtDistance);
        Location loc = LocationManager.get().getLastLocation();
        double dist = distance(loc.getLatitude(), loc.getLongitude(), mPhoto.latitude,
                mPhoto.longitude, "K");
        String distance = String.format( "%.2f", dist) + " km";
        txt.setText(distance);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if(unit.equals("K")) {
            dist = dist * 1.609344;
        }else if(unit.equals("N")) {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    private  double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private  double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    private void setupTransition(LayoutTransition transition){
        transition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {

            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                if(view!=mMapContainer) return;
                mIsMapExpanded = !mIsMapExpanded;
                mMap.getUiSettings().setZoomControlsEnabled(mIsMapExpanded);
                mMap.setMyLocationEnabled(mIsMapExpanded);
                mMapClickListener.setVisibility(mIsMapExpanded ? View.GONE : View.VISIBLE);
                animateMap();
            }
        });
        transition.enableTransitionType(LayoutTransition.CHANGING);
        transition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
    }

    private void animateMap(){
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(mPhotoCoord);
            int padding; // offset from edges of the map in pixels
            CameraUpdate cu;
            if (mIsMapExpanded) {
                Location loc = LocationManager.get().getLastLocation();
                builder.include(new LatLng(loc.getLatitude(), loc.getLongitude()));
                padding = 100;
                LatLngBounds bounds = builder.build();
                cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            }
            else{
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mPhotoCoord)
                        .zoom(15)
                        .build();
                cu = CameraUpdateFactory.newCameraPosition(cameraPosition);
            }
            mMap.animateCamera(cu);
        }
        catch(IllegalStateException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(mPhotoCoord));
        animateMap();
    }

    public void onMapClick(View v){
        if(mIsMapExpanded) return;
        expandMap();
    }

    public void onPhotoClick(View v){
        if(!mIsMapExpanded) return;
        contractMap();
    }

    private void contractMap(){
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mMapContainer.getLayoutParams();
        params.height = mOriginalMapHeight;
        mMapContainer.setLayoutParams(params);
        mMapContainer.requestLayout();
    }

    private void expandMap(){
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mMapContainer.getLayoutParams();
        mOriginalMapHeight = params.height;
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        mMapContainer.setLayoutParams(params);
        mMapContainer.requestLayout();
    }
}
