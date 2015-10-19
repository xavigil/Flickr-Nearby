package com.xavigil.flickrnearby;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.xavigil.flickrnearby.activity.PhotoDetailsActivity;
import com.xavigil.flickrnearby.activity.PhotoPreviewActivity;
import com.xavigil.flickrnearby.location.LocationManager;
import com.xavigil.flickrnearby.model.Photo;
import com.xavigil.flickrnearby.model.PhotosResponse;
import com.xavigil.flickrnearby.server.FlickrAPI;

import java.util.ArrayList;

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
    private ArrayList<Photo> mPhotos;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

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
                MainActivity.this.requestPhotos(location, mPageCount);
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

        setupSwipeRefreshLayout();
        setupRecyclerView();

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

    private void setupSwipeRefreshLayout(){
        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPageCount = 1;
                LocationManager.get().updateLocation();
            }
        });
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    private void setupRecyclerView(){
        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mRecyclerView.getContext(), 3));
        mRecyclerView.setAdapter(new GridAdapter(this));
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

    private void requestPhotos(Location location, int page) {
        Log.d(TAG, "requesting page " + page);
        Call<PhotosResponse> call = mRetrofit.create(FlickrAPI.class).getPhotos(
                "flickr.photos.search",
                BuildConfig.API_KEY,
                "json",
                "1",
                String.valueOf(location.getLatitude()),
                String.valueOf(location.getLongitude()),
                "url_n,url_z,owner_name,geo",
                String.valueOf(page));

        call.enqueue(new Callback<PhotosResponse>() {
            @Override
            public void onResponse(Response<PhotosResponse> response, Retrofit retrofit) {
                Log.d(TAG, "request " + response.raw().request().toString());
//                Log.d(TAG, "received " + response.body().photos.total + " photos");
                mSwipeRefreshLayout.setRefreshing(false);
                if(mPageCount == 1){
                    mPhotos = response.body().photos.photo;
                }
                else{
                    mPhotos.addAll(response.body().photos.photo);
                }
                ((GridAdapter) mRecyclerView.getAdapter()).updateItems(mPhotos);
                mPageCount++;
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    //region RecyclerViewAdapter

    public static class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder>{

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public final View mView;
            public final ImageView mPhoto;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mPhoto = (ImageView) view.findViewById(R.id.photo);
            }

        }

        private Context mContext;
        private ArrayList<Photo> mItems;

        public GridAdapter(Context context){
            mContext = context;
            mItems = new ArrayList<>();
        }

        public void updateItems(ArrayList<Photo> photos){
            mItems = photos;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.gridview_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Picasso.with(mContext)
                    .load(mItems.get(position).url_n)
                    .into(holder.mPhoto);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap bitmap = ((BitmapDrawable) holder.mPhoto.getDrawable()).getBitmap();
                    Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {

                        @Override
                        public void onGenerated(Palette palette) {
                            if (palette != null) {

                                Palette.Swatch darkVibrantSwatch = palette.getDarkVibrantSwatch();
                                Palette.Swatch darkMutedSwatch = palette.getDarkMutedSwatch();
                                Palette.Swatch lightVibrantSwatch = palette.getLightVibrantSwatch();
                                Palette.Swatch lightMutedSwatch = palette.getLightMutedSwatch();
                                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();

                                Palette.Swatch background = (darkVibrantSwatch != null)
                                        ? darkVibrantSwatch : darkMutedSwatch;

                                Palette.Swatch text = (darkVibrantSwatch != null)
                                        ? lightVibrantSwatch : lightMutedSwatch;

                                showPhotoPreview(mItems.get(position), background, text);
                            }


                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        private void showPhotoPreview(Photo photo, Palette.Swatch colorBg, Palette.Swatch colorText){
            Intent intent = new Intent(mContext, PhotoDetailsActivity.class);
            intent.putExtra(PhotoPreviewActivity.EXTRA_PHOTO, photo);
            if(colorBg!=null)
                intent.putExtra("colorBg", colorBg.getRgb());
            if(colorText!=null)
                intent.putExtra("colorTxt", colorText.getRgb());
            mContext.startActivity(intent);
        }

    }

    //endregion

}
