package com.wangsirio.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class UserData implements Parcelable {
    private String gender;
    private String education;
    private int age;
    private int cubeResult;

    public UserData(String gender, String education, int age) {
        this.gender = gender;
        this.education = education;
        this.age = age;
        this.cubeResult = 0;
    }

    protected UserData(Parcel in) {
        gender = in.readString();
        education = in.readString();
        age = in.readInt();
        cubeResult = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(gender);
        dest.writeString(education);
        dest.writeInt(age);
        dest.writeInt(cubeResult);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserData> CREATOR = new Creator<UserData>() {
        @Override
        public UserData createFromParcel(Parcel in) {
            return new UserData(in);
        }

        @Override
        public UserData[] newArray(int size) {
            return new UserData[size];
        }
    };

    // Getters
    public String getGender() { return gender; }
    public String getEducation() { return education; }
    public int getAge() { return age; }

    public void setCubeResult(int cubeResult) {
        this.cubeResult = cubeResult;
    }

    public int getCubeResult() {
        return cubeResult;
    }
} 