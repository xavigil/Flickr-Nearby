package com.xavigil.flickrnearby.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.xavigil.flickrnearby.R;
import com.xavigil.flickrnearby.model.Photo;

public class PhotoPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "photo";

    private Photo mPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        ActionBar ab = getSupportActionBar();
        setupActionBar(ab);

        ImageView iv = (ImageView)findViewById(R.id.photo);
        setupPhoto(iv);
    }

    private void setupActionBar(ActionBar ab){
        if(ab!=null) {
            ab.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00000000")));
            ab.setTitle("");
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupPhoto(ImageView iv){
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            Photo p = extras.getParcelable(EXTRA_PHOTO);
            if(p != null && iv != null){
                mPhoto = p;
                Picasso.with(this)
                        .load(p.url_z)
                        .into(iv);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_info:
                showPhotoDetails();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showPhotoDetails(){
        if(mPhoto == null) return;
        Intent intent = new Intent(this, PhotoPreviewActivity.class);
        intent.putExtra(PhotoPreviewActivity.EXTRA_PHOTO, mPhoto);
        startActivity(intent);
    }
}
