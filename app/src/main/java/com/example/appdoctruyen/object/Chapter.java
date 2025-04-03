package com.example.appdoctruyen.object;

import java.io.Serializable;
public class Chapter implements Serializable {
    private String id;
    public String ngayup;
    public String noidung;
    public String title;

    public Chapter() {}

    public Chapter(String ngayup, String noidung, String title, String id) {
        this.ngayup = ngayup;
        this.noidung = noidung;
        this.title = title;
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getNgayup() {
        return ngayup;
    }

    public void setNgayup(String ngayup) {
        this.ngayup = ngayup;
    }

    public String getNoidung() {
        return noidung;
    }

    public void setNoidung(String noidung) {
        this.noidung = noidung;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
