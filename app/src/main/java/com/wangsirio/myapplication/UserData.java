package com.wangsirio.myapplication;

public class UserData {
    private String gender;
    private String education;
    private int age;

    public UserData(String gender, String education, int age) {
        this.gender = gender;
        this.education = education;
        this.age = age;
    }

    // Getters
    public String getGender() { return gender; }
    public String getEducation() { return education; }
    public int getAge() { return age; }
} 