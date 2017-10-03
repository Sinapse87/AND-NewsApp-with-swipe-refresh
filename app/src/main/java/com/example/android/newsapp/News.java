package com.example.android.newsapp;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Maximilian on 28.09.2017.
 */

public class News implements Parcelable{

    private String title;               // Headline of the news
    private String section;             // Section where this news where published
    private String webUrl;              // URL link for more information on theguardian.com
    private String date;
    private String previewText;
    private String author;
    private Bitmap newsImage;


    public News(String title, String section, String webUrl, String date, String previewText, String author, Bitmap newsImage) {
        this.title = title;
        this.section = section;
        this.webUrl = webUrl;
        this.date = date;
        this.previewText = previewText;
        this.author = author;
        this.newsImage = newsImage;
    }

    public News(Parcel in) {
        title = in.readString();
        section = in.readString();
        webUrl = in.readString();
        date = in.readString();
        previewText = in.readString();
        author = in.readString();
        newsImage = null;
    }

    public String getTitle() {
        return title;
    }

    public String getSection() {
        return section;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getDate() {
        return date;
    }

    public String getPreviewText() {
        return previewText;
    }

    public String getAuthor() {
        return author;
    }

    public Bitmap getNewsImage() {
        return newsImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(title);
        out.writeString(section);
        out.writeString(webUrl);
        out.writeString(date);
        out.writeString(previewText);
        out.writeString(author);

    }

    public static final Parcelable.Creator<News> CREATOR = new Parcelable.Creator<News>(){
      public News createFromParcel(Parcel in){
          return new News(in);
      }

        @Override
        public News[] newArray(int size) {
            return new News[size];
        }
    };
}
