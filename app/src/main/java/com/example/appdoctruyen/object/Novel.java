package com.example.appdoctruyen.object;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class Novel {
    public Map<String, Chapter> chapter;
    public String tentruyen;
    public String linkanh;
    public String tacgia;
    public String tinhtrang;
    public String tomtat;
    private String id;
    private String theloai;
    private String latestChapter;
    public Novel() {
        this.chapter = new HashMap<>();
    }


    public Novel(Map<String, Chapter> chapter, String tentruyen, String linkanh, String tacgia, String tinhtrang, String tomtat, String theloai, String id) {
        this.chapter = chapter;
        this.tentruyen = tentruyen;
        this.linkanh = linkanh;
        this.tacgia = tacgia;
        this.tinhtrang = tinhtrang;
        this.tomtat = tomtat;
        this.theloai = theloai;
        this.id = id;
    }

    public String getLatestChapter() {
        return latestChapter;
    }
    public void setLatestChapter(String latestChapter) {
        this.latestChapter = latestChapter;
    }
    public String getTheloai() {
        return theloai;
    }
    public void setTheloai(String theloai) {
        this.theloai = theloai;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Map<String, Chapter> getChapter() {
        return chapter;
    }

    public void setChapter(Map<String, Chapter> chapter) {
        this.chapter = chapter;
    }

    public String getTentruyen() {
        return tentruyen;
    }

    public void setTentruyen(String tentruyen) {
        this.tentruyen = tentruyen;
    }

    public String getLinkanh() {
        return linkanh;
    }

    public void setLinkanh(String linkanh) {
        this.linkanh = linkanh;
    }

    public String getTacgia() {
        return tacgia;
    }

    public void setTacgia(String tacgia) {
        this.tacgia = tacgia;
    }

    public String getTinhtrang() {
        return tinhtrang;
    }

    public void setTinhtrang(String tinhtrang) {
        this.tinhtrang = tinhtrang;
    }

    public String getTomtat() {
        return tomtat;
    }

    public void setTomtat(String tomtat) {
        this.tomtat = tomtat;
    }

}
