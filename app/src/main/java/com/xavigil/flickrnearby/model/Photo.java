package com.xavigil.flickrnearby.model;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("unused")
public class Photo implements Parcelable {
    public String id;
    public String owner;
    public String secret;
    public String server;
    public String farm;
    public String title;
    public int ispublic;
    public int isfriend;
    public int isfamily;
    public String url_n;
    public int height_n;
    public int width_n;
    public String url_z;
    public int height_z;
    public int width_z;

    public Photo(Parcel in){
        id=in.readString();
        owner=in.readString();
        secret=in.readString();
        server=in.readString();
        farm=in.readString();
        title=in.readString();
        ispublic=in.readInt();
        isfriend=in.readInt();
        isfamily=in.readInt();
        url_n=in.readString();
        height_n=in.readInt();
        width_n=in.readInt();
        url_z=in.readString();
        height_z=in.readInt();
        width_z=in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(owner);
        parcel.writeString(secret);
        parcel.writeString(server);
        parcel.writeString(farm);
        parcel.writeString(title);
        parcel.writeInt(ispublic);
        parcel.writeInt(isfriend);
        parcel.writeInt(isfamily);
        parcel.writeString(url_n);
        parcel.writeInt(height_n);
        parcel.writeInt(width_n);
        parcel.writeString(url_z);
        parcel.writeInt(height_z);
        parcel.writeInt(width_z);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Photo createFromParcel(Parcel in) {
            return new Photo(in);
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

}
