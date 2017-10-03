package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Maximilian on 28.09.2017.
 */

public class NewsAdapter extends ArrayAdapter<News>{

    public NewsAdapter(@NonNull Context context, ArrayList<News> newsArrayList) {
        super(context, 0, newsArrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemlist_item, parent, false);
        }

        TextView textTitle = (TextView) convertView.findViewById(R.id.itemlist_title);
        TextView textSection = (TextView) convertView.findViewById(R.id.itemlist_section);
        TextView textDate = (TextView) convertView.findViewById(R.id.itemlist_date);
        TextView textPreview = (TextView) convertView.findViewById(R.id.itemlist_trailText);
        TextView textAuthor = (TextView) convertView.findViewById(R.id.itemlist_author);

        final News currentNews = getItem(position);

        textTitle.setText(currentNews.getTitle());
        textSection.setText(currentNews.getSection());
        textDate.setText(currentNews.getDate());
        textPreview.setText(currentNews.getPreviewText());

        if(currentNews.getAuthor() != null) {
            String byAuthor = getContext().getString(R.string.by_author);
            textAuthor.setText(byAuthor + " " + currentNews.getAuthor());
            textAuthor.setVisibility(View.VISIBLE);
        }else{
            textAuthor.setVisibility(View.GONE);
        }

        ImageView previewImage = (ImageView) convertView.findViewById(R.id.itemlist_image);
        if(currentNews.getNewsImage() != null){
            previewImage.setImageBitmap(currentNews.getNewsImage());
            previewImage.setVisibility(View.VISIBLE);
        }else{
            previewImage.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = currentNews.getWebUrl();
                Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(browser);
            }
        });

        return convertView;
    }
}
