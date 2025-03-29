package com.example.appdoctruyen.object;

public class Chapter {
    public String ngayUp;
    public String noiDung;
    public String title;

    public Chapter(String ngayUp, String noiDung, String title) {
        this.ngayUp = ngayUp;
        this.noiDung = noiDung;
        this.title = title;
    }

    public String getNgayUp() {
        return ngayUp;
    }

    public void setNgayUp(String ngayUp) {
        this.ngayUp = ngayUp;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
