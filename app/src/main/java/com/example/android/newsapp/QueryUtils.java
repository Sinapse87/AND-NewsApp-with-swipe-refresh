package com.example.android.newsapp;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.graphics.BitmapFactory.decodeStream;

/**
 * Created by Maximilian on 28.09.2017.
 */

public class QueryUtils {

    private static final String LOG_TAG = "Results: ";

    public static List<News> fetchNewsData(String requestUrl){

        Log.i(LOG_TAG, "fetchNewsData started");

        List<News> news = new ArrayList<>();

        /** Create URL from input string */
        URL url = createUrl(requestUrl);

        Log.i(LOG_TAG, "URL created " + url.toString());

        String jsonResponse = "";

        try {
            jsonResponse = makeHttpRequest(url);

            news = extractDataFromJson(jsonResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return news;
    }

    /**
     * Creates a properly formatted URL from the input string
     * @param requestUrl: User request in String format
     * @return: User request in URL format
     */
    private static URL createUrl(String requestUrl){

        URL url = null;

        try {
            url = new URL(requestUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        Log.i(LOG_TAG, "Created URL " + url);
        return url;
    }

    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        if(url == null){
            return null;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /** 10 seconds */);
            urlConnection.setConnectTimeout(15000 /** 15 seconds */);
            urlConnection.connect();

            if(urlConnection.getResponseCode() == 200){
                inputStream = urlConnection.getInputStream();
                jsonResponse = readInputStream(inputStream);
                Log.i(LOG_TAG, "Connection O.K, InputStream transferred");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if (inputStream != null){
                inputStream.close();
            }
        }

        return jsonResponse;
    }

    private static String readInputStream(InputStream input) throws IOException {
        StringBuilder output = new StringBuilder();

        if (input != null) {

            InputStreamReader inputStreamReader = new InputStreamReader(input, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);

            Log.i(LOG_TAG, "start reading inputStream");

            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }

        return output.toString();
    }

    private static List<News> extractDataFromJson(String newsJSON){
        if(TextUtils.isEmpty(newsJSON)){
            Log.i(LOG_TAG, "JSONResponse is empty");
            return null;
        }
        List<News> news = new ArrayList<>();

        try {
            Log.v(LOG_TAG, "Trying to create new News objects");
            JSONObject jsonResponse = new JSONObject(newsJSON);

            JSONObject responseJson = jsonResponse.getJSONObject("response");

            JSONArray jsonArray = responseJson.getJSONArray("results");
            Log.v(LOG_TAG, "JSONArray length : "+ jsonArray.length());

            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject currentNews = jsonArray.getJSONObject(i);

                String title = null;
                String webUrl = null;
                String section = null;
                String date = null;
                String previewText = null;
                String author = null;
                Bitmap previewImage = null;

                if(currentNews.has("webTitle")){
                    title = currentNews.getString("webTitle");
                }

                if(currentNews.has("sectionName")){
                    section = currentNews.getString("sectionName");
                }

                if(currentNews.has("webUrl")){
                    webUrl = currentNews.getString("webUrl");
                }

                String unformattedDate;
                if(currentNews.has("webPublicationDate")){
                    unformattedDate = currentNews.getString("webPublicationDate");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    try {
                        Date dateObject = dateFormat.parse(unformattedDate);
                        date = (String) android.text.format.DateFormat.format("dd" + "." + "MMM" + " " + "yyyy" + ", " + "HH:mm", dateObject);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                JSONObject fields = currentNews.getJSONObject("fields");

                if(fields.has("trailText")) {
                    previewText = removeHtmlTags(fields.getString("trailText"));
                }

                if(fields.has("byline")) {
                    author = fields.getString("byline");
                }

                if(fields.has("thumbnail")){
                    previewImage = fetchThumbnail(fields.getString("thumbnail"));
                }

                news.add(new News(title, section, webUrl, date, previewText, author, previewImage));
                Log.i(LOG_TAG, "News object created, Title :" + title);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return news;
    }

    private static Bitmap fetchThumbnail(String requestUrl){
        URL url = createUrl(requestUrl);

        Bitmap thumbnail = null;

        try {
            thumbnail = makeHttpRequestBitmap(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return thumbnail;
    }

    private static Bitmap makeHttpRequestBitmap(URL url) throws IOException {
        Bitmap bitmapResponse = null;

        if(url == null){
            return null;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /** 10 seconds */);
            urlConnection.setConnectTimeout(15000 /** 15 seconds */);
            urlConnection.connect();

            if(urlConnection.getResponseCode() == 200){
                inputStream = urlConnection.getInputStream();
                bitmapResponse = decodeStream(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(inputStream != null){
                inputStream.close();
            }
        }

        return bitmapResponse;

    }

    /** Remove Html tags from the string for better readability of the displayed text */
    private static String removeHtmlTags(String jsonHtml){
        String text = android.text.Html.fromHtml(jsonHtml).toString();

        return text;
    }
}
