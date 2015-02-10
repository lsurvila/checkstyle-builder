package com.lsurvila.checkstylebuilder;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableExample implements Parcelable {

    private String town;
    private String postCode;
    private double latitude;
    private double longitude;
    private boolean open;

    protected ParcelableExample(Parcel in) {
        town = in.readString();
        postCode = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        open = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(town);
        dest.writeString(postCode);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeByte((byte) (open ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ParcelableExample> CREATOR = new Parcelable.Creator<ParcelableExample>() {
        @Override
        public ParcelableExample createFromParcel(Parcel in) {
            return new ParcelableExample(in);
        }

        @Override
        public ParcelableExample[] newArray(int size) {
            return new ParcelableExample[size];
        }
    };

}