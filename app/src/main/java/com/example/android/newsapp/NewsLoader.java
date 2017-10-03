package com.example.android.newsapp;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.util.List;


/**
 * Created by Maximilian on 28.09.2017.
 */

public class NewsLoader extends AsyncTaskLoader<List<News>>{

    private String mUrl;

    private static final String LOG_TAG = "Results: ";

    public NewsLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<News> loadInBackground() {
        Log.i(LOG_TAG, "LoadInBackground started");

        List<News> result = QueryUtils.fetchNewsData(mUrl);

        return result;
    }
}
